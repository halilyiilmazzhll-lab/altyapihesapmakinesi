package com.example.egimhesabi.util

import com.example.egimhesabi.data.StakeoutManholeEntity
import com.example.egimhesabi.data.normalizeEntityName
import jxl.NumberCell
import jxl.WorkbookSettings
import org.dhatim.fastexcel.Workbook
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.ext.DefaultHandler2
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory

enum class StakeoutField(val label: String) {
    NAME("Baca no"), PROJECT_Y("Proje Y"), PROJECT_X("Proje X"),
    PROJECT_COVER("Proje kapak kotu"), PROJECT_GROUND("Proje siyah kotu"),
    PROJECT_INVERT("Proje akar kotu"), TERRAIN_GROUND("Arazi siyah kotu"),
    DISCHARGE_COVER("Deşarj kapak kotu"), DISCHARGE_DEPTH("Deşarj derinlik")
}

data class StakeoutSheet(val name: String, val rows: List<List<String>>)
data class StakeoutWorkbookData(val sheets: List<StakeoutSheet>)
data class StakeoutPreviewRow(val excelRow: Int, val manhole: StakeoutManholeEntity)
data class StakeoutPreview(
    val rows: List<StakeoutPreviewRow>,
    val issues: List<String>,
    val ignoredRowCount: Int = 0
) {
    val canImport: Boolean get() = rows.isNotEmpty() && issues.isEmpty()
}

/** Workbook I/O without Android APIs. Call from a background dispatcher. */
object StakeoutWorkbook {
    const val HEADER_ROW = 2
    const val MAX_FILE_BYTES = 8 * 1024 * 1024
    const val MAX_ROWS = 20_003
    const val MAX_COLUMNS = 128
    private const val MAX_EXPANDED_BYTES = 32 * 1024 * 1024
    private const val MAX_CELLS = 150_000
    private const val MAX_SHEETS = 30
    private const val CELL_ERROR = "⛔ "

    fun read(input: InputStream, fileName: String): StakeoutWorkbookData {
        val bytes = readBounded(input, MAX_FILE_BYTES, "Dosya en fazla 8 MB olabilir.")
        require(bytes.isNotEmpty()) { "Dosya boş." }
        return try {
            when {
                bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() -> readXlsx(bytes)
                bytes.size >= 8 && bytes.take(8).toByteArray().contentEquals(
                    byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte())
                ) -> readXls(bytes)
                else -> throw IllegalArgumentException("$fileName okunamadı. .xls, .xlsx veya .xlsm biçiminde bir Excel dosyası seçin.")
            }
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (error: Exception) {
            throw IllegalArgumentException("Excel dosyası okunamadı. Dosyanın sağlam ve şifresiz olduğunu kontrol edin.", error)
        }
    }

    fun defaultMapping(sheet: StakeoutSheet): Map<StakeoutField, Int?> {
        val width = sheet.rows.maxOfOrNull { it.size } ?: 0
        return StakeoutField.entries.associateWith { field -> field.ordinal.takeIf { it < width } }
    }

    fun preview(sheet: StakeoutSheet, mapping: Map<StakeoutField, Int?>, dataStartRow: Int = 3): StakeoutPreview {
        val issues = mutableListOf<String>()
        val rows = mutableListOf<StakeoutPreviewRow>()
        val width = sheet.rows.maxOfOrNull { it.size } ?: 0
        if (dataStartRow !in sheet.rows.indices) issues += "Başlangıç satırı sayfada bulunamadı."
        if (mapping[StakeoutField.NAME] == null) issues += "Baca no sütunu seçilmelidir."
        val selected = StakeoutField.entries.mapNotNull { field -> mapping[field]?.let { field to it } }
        if (selected.any { it.second !in 0 until width }) issues += "Seçilen sütunlardan biri çalışma sayfasında bulunamadı."
        if (selected.map { it.second }.distinct().size != selected.size) issues += "Aynı sütun birden fazla bilgiye eşleştirilemez."
        if (issues.isNotEmpty()) return StakeoutPreview(emptyList(), issues)

        val seenNames = mutableMapOf<String, Int>()
        var ignored = 0
        sheet.rows.drop(dataStartRow).forEachIndexed { offset, cells ->
            val excelRow = offset + dataStartRow + 1
            fun cell(field: StakeoutField) = mapping[field]?.let { cells.getOrNull(it) }.orEmpty().trim()
            if (selected.all { cells.getOrNull(it.second).isNullOrBlank() }) {
                ignored++
                return@forEachIndexed
            }
            val rowIssues = mutableListOf<String>()
            val name = cell(StakeoutField.NAME)
            if (name.isBlank()) rowIssues += "Baca no boş olamaz."
            if (name.startsWith(CELL_ERROR)) rowIssues += "Baca no: ${name.removePrefix(CELL_ERROR)}"
            if (name.length > 100) rowIssues += "Baca no en fazla 100 karakter olabilir."
            val normalized = normalizeEntityName(name)
            val previous = seenNames.putIfAbsent(normalized, excelRow)
            if (name.isNotBlank() && previous != null) rowIssues += "\"$name\" baca no tekrarlanıyor (ilk kayıt: $previous. satır)."
            fun number(field: StakeoutField): Double? {
                val raw = cell(field)
                if (raw.isBlank()) return null
                return try {
                    parseNumber(raw)
                } catch (_: IllegalArgumentException) {
                    rowIssues += "${field.label}: ${if (raw.startsWith(CELL_ERROR)) raw.removePrefix(CELL_ERROR) else "\"$raw\" geçerli bir sayı değil."}"
                    null
                }
            }
            val record = StakeoutManholeEntity(
                neighborhoodId = 0,
                name = name,
                projectY = number(StakeoutField.PROJECT_Y),
                projectX = number(StakeoutField.PROJECT_X),
                projectCoverLevel = number(StakeoutField.PROJECT_COVER),
                projectGroundLevel = number(StakeoutField.PROJECT_GROUND),
                projectInvertLevel = number(StakeoutField.PROJECT_INVERT),
                terrainGroundLevel = number(StakeoutField.TERRAIN_GROUND),
                dischargeCoverLevel = number(StakeoutField.DISCHARGE_COVER),
                dischargeDepth = number(StakeoutField.DISCHARGE_DEPTH)
            )
            if ((record.dischargeDepth ?: 0.0) < 0.0) rowIssues += "Deşarj derinlik negatif olamaz."
            if (record.terrainInvertLevel?.isFinite() == false) rowIssues += "Araziye göre akar kotu sayı sınırlarını aşıyor."
            issues += rowIssues.map { "$excelRow. satır: $it" }
            rows += StakeoutPreviewRow(excelRow, record)
        }
        if (rows.isEmpty()) issues += "${dataStartRow + 1}. satırdan itibaren aktarılabilecek baca kaydı bulunamadı."
        return StakeoutPreview(rows, issues, ignored)
    }

    /** Decimal comma or point; grouped values may contain both, spaces or apostrophes. */
    fun parseNumber(value: String): Double {
        var normalized = value.trim().replace('\u2212', '-').replace(Regex("[\\s\u00A0\u202F']"), "")
        require(normalized.isNotEmpty()) { "Sayı boş olamaz." }
        if (',' in normalized && '.' in normalized) {
            val decimal = if (normalized.lastIndexOf(',') > normalized.lastIndexOf('.')) ',' else '.'
            val grouping = if (decimal == ',') '.' else ','
            val pieces = normalized.split(decimal)
            require(pieces.size == 2 && grouping !in pieces[1]) { "Geçersiz sayı." }
            requireGroupedInteger(pieces[0], grouping)
            normalized = pieces[0].replace(grouping.toString(), "") + "." + pieces[1]
        } else if (normalized.count { it == ',' } > 1 || normalized.count { it == '.' } > 1) {
            val grouping = if (',' in normalized) ',' else '.'
            requireGroupedInteger(normalized, grouping)
            normalized = normalized.replace(grouping.toString(), "")
        } else {
            normalized = normalized.replace(',', '.')
        }
        require(normalized.matches(Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?"))) { "Geçersiz sayı." }
        val number = normalized.toDoubleOrNull()
        require(number != null && number.isFinite()) { "Geçersiz sayı." }
        return number
    }

    private fun requireGroupedInteger(value: String, separator: Char) {
        if (separator !in value) return
        val groups = value.removePrefix("-").removePrefix("+").split(separator)
        require(groups.first().length in 1..3 && groups.first().all { it.isDigit() } &&
            groups.drop(1).all { it.length == 3 && it.all(Char::isDigit) }) { "Geçersiz binlik ayırıcı." }
    }

    fun export(output: OutputStream, records: List<StakeoutManholeEntity>, title: String) {
        require(records.size <= MAX_ROWS - 3) { "En fazla 20.000 baca dışa aktarılabilir." }
        val workbook = Workbook(output, "Eğim Hesabı", "2.0")
        val sheet = workbook.newWorksheet("Aplikasyon Tutanağı")
        sheet.value(0, 0, title.take(32767))
        sheet.style(0, 0).bold().set()
        val headers = StakeoutField.entries.map { it.label } + "Araziye göre akar kotu"
        headers.forEachIndexed { column, label ->
            sheet.value(HEADER_ROW, column, label)
            sheet.style(HEADER_ROW, column).bold().wrapText(true).set()
            sheet.width(column, if (column == 0) 18.0 else 23.0)
        }
        records.forEachIndexed { index, record ->
            val row = index + HEADER_ROW + 1
            sheet.value(row, 0, record.name)
            listOf(record.projectY, record.projectX, record.projectCoverLevel, record.projectGroundLevel,
                record.projectInvertLevel, record.terrainGroundLevel, record.dischargeCoverLevel,
                record.dischargeDepth).forEachIndexed { column, value ->
                if (value != null) {
                    require(value.isFinite()) { "${record.name}: geçersiz sayısal değer." }
                    sheet.value(row, column + 1, value)
                    sheet.style(row, column + 1).format("0.000").set()
                }
            }
            if (record.terrainGroundLevel != null && record.dischargeDepth != null) {
                sheet.formula(row, 9, "G${row + 1}-I${row + 1}")
                sheet.style(row, 9).format("0.000").set()
            }
        }
        workbook.finish()
    }

    private fun readXls(bytes: ByteArray): StakeoutWorkbookData {
        val settings = WorkbookSettings().apply {
            locale = Locale.US
            setSuppressWarnings(true)
            setDrawingsDisabled(true)
            setNamesDisabled(true)
            setCellValidationDisabled(true)
            setGCDisabled(true)
        }
        val workbook = jxl.Workbook.getWorkbook(ByteArrayInputStream(bytes), settings)
        try {
            require(workbook.numberOfSheets <= MAX_SHEETS) { "Dosyada en fazla $MAX_SHEETS çalışma sayfası olabilir." }
            var cellCount = 0
            return StakeoutWorkbookData(workbook.sheets.map { sheet ->
                require(sheet.rows <= MAX_ROWS && sheet.columns <= MAX_COLUMNS) { "Sayfa sınırı aşıldı: en fazla 20.000 veri satırı ve $MAX_COLUMNS sütun." }
                val rows = (0 until sheet.rows).map { row ->
                    val cells = sheet.getRow(row)
                    cellCount += cells.size
                    require(cellCount <= MAX_CELLS) { "Dosyada en fazla $MAX_CELLS hücre okunabilir." }
                    cells.map { cell ->
                        when {
                            cell.type == jxl.CellType.ERROR || cell.type == jxl.CellType.FORMULA_ERROR -> CELL_ERROR + "Excel hücresinde hata var: ${cell.contents}"
                            cell is NumberCell -> {
                                val contents = cell.contents
                                if (contents.matches(Regex("0[0-9]+"))) contents else canonicalNumber(cell.value.toString())
                            }
                            else -> cell.contents
                        }.also { require(it.length <= 32767) { "Hücre metni çok uzun." } }
                    }
                }
                StakeoutSheet(sheet.name, rows)
            })
        } finally {
            workbook.close()
        }
    }

    private fun readXlsx(bytes: ByteArray): StakeoutWorkbookData {
        val entries = mutableMapOf<String, ByteArray>()
        var expanded = 0
        var entryCount = 0
        // ZIP64 producers may use data descriptors that ZipInputStream cannot read.
        // ZipFile reads the central directory while limits still apply to expanded bytes.
        val archive = java.io.File.createTempFile("stakeout-", ".zip")
        try {
            archive.writeBytes(bytes)
            java.util.zip.ZipFile(archive).use { zip ->
                val iterator = zip.entries()
                while (iterator.hasMoreElements()) {
                    val entry = iterator.nextElement()
                    require(++entryCount <= 2048) { "Excel arşivinde çok fazla dosya var." }
                    val content = zip.getInputStream(entry).use { readBounded(it, MAX_EXPANDED_BYTES - expanded, "Excel dosyasının açılmış boyutu 32 MB sınırını aşıyor.") }
                    val checksum = java.util.zip.CRC32().apply { update(content) }.value
                    require(entry.crc == checksum) { "Excel arşivindeki dosya bozuk." }
                    expanded += content.size
                    if (!entry.isDirectory && (entry.name.endsWith(".xml") || entry.name == "xl/_rels/workbook.xml.rels")) {
                        require(entries.put(entry.name, content) == null) { "Excel arşivinde yinelenen dosya var." }
                    }
                }
            }
        } finally { archive.delete() }
        val workbookXml = entries["xl/workbook.xml"] ?: throw IllegalArgumentException("Excel çalışma kitabı bulunamadı. .xlsx veya .xlsm dosyası seçin.")
        val relationships = mutableMapOf<String, String>()
        parseXml(entries["xl/_rels/workbook.xml.rels"] ?: throw IllegalArgumentException("Excel sayfa bağlantıları bulunamadı."), object : SafeHandler() {
            override fun startElement(uri: String?, localName: String, qName: String?, attributes: Attributes) {
                if (localName == "Relationship" && attributes.getValue("TargetMode") != "External") {
                    val id = attributes.getValue("Id") ?: return
                    val target = attributes.getValue("Target") ?: return
                    relationships[id] = normalizeZipPath(if (target.startsWith('/')) target.drop(1) else "xl/$target")
                }
            }
        })
        val sheets = mutableListOf<Pair<String, String>>()
        parseXml(workbookXml, object : SafeHandler() {
            override fun startElement(uri: String?, localName: String, qName: String?, attributes: Attributes) {
                if (localName == "sheet") {
                    require(sheets.size < MAX_SHEETS) { "Dosyada en fazla $MAX_SHEETS çalışma sayfası olabilir." }
                    val relationship = (0 until attributes.length).firstOrNull { attributes.getLocalName(it) == "id" }
                        ?.let(attributes::getValue)
                    sheets += (attributes.getValue("name") ?: "Sayfa ${sheets.size + 1}") to
                        (relationships[relationship] ?: throw IllegalArgumentException("Çalışma sayfası bağlantısı geçersiz."))
                }
            }
        })
        require(sheets.isNotEmpty()) { "Dosyada çalışma sayfası bulunamadı." }
        val sharedStrings = mutableListOf<String>()
        entries["xl/sharedStrings.xml"]?.let { xml ->
            parseXml(xml, object : SafeHandler() {
                var builder = StringBuilder()
                var inText = false
                var phonetic = false
                override fun startElement(uri: String?, localName: String, qName: String?, attributes: Attributes) {
                    when (localName) {
                        "si" -> builder = StringBuilder()
                        "rPh" -> phonetic = true
                        "t" -> inText = !phonetic
                    }
                }
                override fun characters(ch: CharArray, start: Int, length: Int) {
                    if (inText) {
                        require(builder.length + length <= 32767) { "Hücre metni çok uzun." }
                        builder.append(ch, start, length)
                    }
                }
                override fun endElement(uri: String?, localName: String, qName: String?) {
                    when (localName) {
                        "t" -> inText = false
                        "rPh" -> phonetic = false
                        "si" -> {
                            require(sharedStrings.size < 100_000) { "Excel dosyasında çok fazla metin hücresi var." }
                            sharedStrings += builder.toString()
                        }
                    }
                }
            })
        }
        val paddingStyles = readPaddingStyles(entries["xl/styles.xml"])
        var cellCount = 0
        return StakeoutWorkbookData(sheets.map { (name, path) ->
            val rows = mutableListOf<List<String>>()
            parseXml(entries[path] ?: throw IllegalArgumentException("\"$name\" çalışma sayfası bulunamadı."), object : SafeHandler() {
                var rowIndex = -1
                var column = -1
                var cells = mutableListOf<String>()
                var type = ""
                var style = -1
                var hasFormula = false
                var inValue = false
                var inText = false
                var phonetic = false
                var value = StringBuilder()
                var inline = StringBuilder()
                override fun startElement(uri: String?, localName: String, qName: String?, attributes: Attributes) {
                    when (localName) {
                        "row" -> {
                            val nextRow = attributes.getValue("r")?.toIntOrNull()?.minus(1) ?: (rowIndex + 1)
                            require(nextRow > rowIndex && nextRow < MAX_ROWS) { "Excel satır sınırı aşıldı veya satır numarası geçersiz (en fazla 20.000 veri satırı)." }
                            rowIndex = nextRow
                            while (rows.size < rowIndex) rows += emptyList<String>()
                            cells = mutableListOf()
                            column = -1
                        }
                        "c" -> {
                            require(++cellCount <= MAX_CELLS) { "Dosyada en fazla $MAX_CELLS hücre okunabilir." }
                            val nextColumn = attributes.getValue("r")?.let(::columnIndex) ?: (column + 1)
                            require(rowIndex >= 0 && nextColumn > column && nextColumn < MAX_COLUMNS) { "Excel sütun sınırı aşıldı veya hücre adresi geçersiz (en fazla $MAX_COLUMNS sütun)." }
                            column = nextColumn
                            type = attributes.getValue("t").orEmpty()
                            style = attributes.getValue("s")?.toIntOrNull() ?: -1
                            hasFormula = false
                            value = StringBuilder()
                            inline = StringBuilder()
                        }
                        "f" -> hasFormula = true
                        "v" -> inValue = true
                        "rPh" -> phonetic = true
                        "t" -> inText = !phonetic
                    }
                }
                override fun characters(ch: CharArray, start: Int, length: Int) {
                    val builder = if (inValue) value else if (inText) inline else return
                    require(builder.length + length <= 32767) { "Hücre metni çok uzun." }
                    builder.append(ch, start, length)
                }
                override fun endElement(uri: String?, localName: String, qName: String?) {
                    when (localName) {
                        "v" -> inValue = false
                        "t" -> inText = false
                        "rPh" -> phonetic = false
                        "c" -> {
                            val raw = value.toString()
                            val text = when {
                                type == "e" -> CELL_ERROR + "Excel hücresinde hata var: $raw"
                                hasFormula && raw.isBlank() -> CELL_ERROR + "Formülün hesaplanmış sonucu yok. Excel'de yeniden hesaplayıp kaydedin."
                                type == "s" -> raw.toIntOrNull()?.let(sharedStrings::getOrNull)
                                    ?: throw IllegalArgumentException("Excel ortak metin başvurusu geçersiz.")
                                type == "inlineStr" -> inline.toString()
                                type == "b" -> CELL_ERROR + "Mantıksal değer sayı olarak kullanılamaz."
                                type == "n" || type.isEmpty() -> paddedNumber(raw, paddingStyles[style])
                                else -> raw
                            }
                            while (cells.size <= column) cells += ""
                            cells[column] = text
                        }
                        "row" -> rows += cells.toList()
                    }
                }
            })
            StakeoutSheet(name, rows)
        })
    }

    private fun readPaddingStyles(xml: ByteArray?): Map<Int, Int> {
        if (xml == null) return emptyMap()
        val formats = mutableMapOf<Int, Int>()
        val styles = mutableMapOf<Int, Int>()
        var inCellFormats = false
        var styleIndex = 0
        parseXml(xml, object : SafeHandler() {
            override fun startElement(uri: String?, localName: String, qName: String?, attributes: Attributes) {
                when (localName) {
                    "numFmt" -> {
                        val format = attributes.getValue("formatCode").orEmpty().substringBefore(';')
                        val id = attributes.getValue("numFmtId")?.toIntOrNull()
                        if (id != null && format.matches(Regex("0{2,100}"))) formats[id] = format.length
                    }
                    "cellXfs" -> inCellFormats = true
                    "xf" -> if (inCellFormats) {
                        formats[attributes.getValue("numFmtId")?.toIntOrNull()]?.let { styles[styleIndex] = it }
                        styleIndex++
                    }
                }
            }
            override fun endElement(uri: String?, localName: String, qName: String?) {
                if (localName == "cellXfs") inCellFormats = false
            }
        })
        return styles
    }

    private fun paddedNumber(value: String, width: Int?): String {
        if (width == null || value.isBlank()) return canonicalNumber(value)
        return try {
            val integer = BigDecimal(value).toBigIntegerExact().toString()
            if (integer.startsWith('-')) "-" + integer.drop(1).padStart(width, '0') else integer.padStart(width, '0')
        } catch (_: ArithmeticException) {
            value
        } catch (_: NumberFormatException) {
            value
        }
    }

    private fun canonicalNumber(value: String): String {
        if (value.toDoubleOrNull()?.isFinite() != true) return value
        return try {
            val number = BigDecimal(value).stripTrailingZeros()
            // Avoid expanding extreme exponents from a malformed workbook into huge strings.
            if (number.scale() !in -400..400) value else number.toPlainString()
        } catch (_: NumberFormatException) {
            value
        }
    }

    private fun columnIndex(reference: String): Int {
        val letters = reference.takeWhile { it.isLetter() }
        require(letters.isNotEmpty() && letters.length <= 3) { "Geçersiz Excel hücre adresi." }
        return letters.fold(0) { result, char -> result * 26 + (char.uppercaseChar() - 'A' + 1) } - 1
    }

    private fun normalizeZipPath(path: String): String {
        val segments = mutableListOf<String>()
        path.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> {
                    require(segments.isNotEmpty()) { "Excel sayfa yolu geçersiz." }
                    segments.removeAt(segments.lastIndex)
                }
                else -> segments += segment
            }
        }
        return segments.joinToString("/")
    }

    private open class SafeHandler : DefaultHandler2() {
        override fun startDTD(name: String?, publicId: String?, systemId: String?) {
            throw SAXException("Excel XML dosyasında DTD desteklenmez.")
        }
        override fun resolveEntity(publicId: String?, systemId: String?): InputSource {
            throw SAXException("Dış XML kaynakları desteklenmez.")
        }
    }

    private fun parseXml(xml: ByteArray, handler: SafeHandler) {
        val factory = SAXParserFactory.newInstance().apply { isNamespaceAware = true }
        val reader = factory.newSAXParser().xmlReader
        // Lexical handler rejects all DTDs, including internal entity expansion.
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler)
        reader.entityResolver = handler
        reader.contentHandler = handler
        reader.errorHandler = handler
        reader.parse(InputSource(ByteArrayInputStream(xml)))
    }

    private fun readBounded(input: InputStream, limit: Int, message: String): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= limit) { message }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
