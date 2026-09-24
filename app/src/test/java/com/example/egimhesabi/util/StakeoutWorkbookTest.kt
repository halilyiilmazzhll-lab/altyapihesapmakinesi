package com.example.egimhesabi.util

import com.example.egimhesabi.data.StakeoutManholeEntity
import jxl.write.Label
import jxl.write.NumberFormat
import jxl.write.WritableCellFormat
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class StakeoutWorkbookTest {
    @Test
    fun readsRealLegacyXlsWithThirdRowHeadersAndPaddedIdentifier() {
        val output = ByteArrayOutputStream()
        val workbook = jxl.Workbook.createWorkbook(output)
        val worksheet = workbook.createSheet("Merkez", 0)
        worksheet.addCell(Label(0, 0, "Aplikasyon tutanağı"))
        StakeoutField.entries.forEachIndexed { column, field -> worksheet.addCell(Label(column, 2, field.label)) }
        worksheet.addCell(jxl.write.Number(0, 3, 7.0, WritableCellFormat(NumberFormat("0000"))))
        worksheet.addCell(jxl.write.Number(1, 3, 414222.370))
        worksheet.addCell(jxl.write.Number(2, 3, 4360474.879))
        worksheet.addCell(jxl.write.Number(6, 3, 739.773))
        worksheet.addCell(jxl.write.Number(8, 3, 1.8))
        workbook.write()
        workbook.close()

        val sheet = StakeoutWorkbook.read(ByteArrayInputStream(output.toByteArray()), "tutanak.xls").sheets.single()
        val result = StakeoutWorkbook.preview(sheet, StakeoutWorkbook.defaultMapping(sheet))
        assertTrue(result.issues.toString(), result.canImport)
        assertEquals("Merkez", sheet.name)
        assertEquals(4, result.rows.single().excelRow)
        val record = result.rows.single().manhole
        assertEquals("0007", record.name)
        assertEquals(414222.370, record.projectY!!, 0.000001)
        assertEquals(4360474.879, record.projectX!!, 0.000001)
        assertEquals(737.973, record.terrainInvertLevel!!, 0.000001)
        assertNull(record.projectCoverLevel)
    }

    @Test
    fun exportedXlsxRoundTripsBlankAndZeroAndWritesConditionalDerivedFormula() {
        val records = listOf(
            StakeoutManholeEntity(neighborhoodId = 8, name = "A01", projectY = 0.0, projectX = 123.456,
                projectCoverLevel = 100.0, terrainGroundLevel = 101.2, dischargeDepth = 0.0),
            StakeoutManholeEntity(neighborhoodId = 8, name = "0002", terrainGroundLevel = 100.0)
        )
        val output = ByteArrayOutputStream()
        StakeoutWorkbook.export(output, records, "İlçe / Mahalle")
        val sheet = StakeoutWorkbook.read(ByteArrayInputStream(output.toByteArray()), "tutanak.xlsx").sheets.single()
        assertEquals(StakeoutField.NAME.label, sheet.rows[2][0])
        val preview = StakeoutWorkbook.preview(sheet, StakeoutWorkbook.defaultMapping(sheet))
        assertTrue(preview.issues.toString(), preview.canImport)
        assertEquals(2, preview.rows.size)
        assertEquals(0.0, preview.rows[0].manhole.projectY!!, 0.0)
        assertEquals(101.2, preview.rows[0].manhole.terrainInvertLevel!!, 0.000001)
        assertNull(preview.rows[0].manhole.projectGroundLevel)
        assertEquals("0002", preview.rows[1].manhole.name)
        assertNull(preview.rows[1].manhole.dischargeDepth)
        assertNull(preview.rows[1].manhole.terrainInvertLevel)
        val xml = zipEntries(output.toByteArray()).getValue("xl/worksheets/sheet1.xml")
        assertTrue(xml.contains("G4-I4"))
        assertFalse(xml.contains("G5-I5"))
    }

    @Test
    fun xlsxReadsSharedInlineNumericAndCachedFormulaCellsAndSparseRows() {
        val rows = """
            <row r="1"><c r="A1" t="inlineStr"><is><t>Başlık</t></is></c></row>
            <row r="3"><c r="A3" t="inlineStr"><is><t>Baca no</t></is></c><c r="I3" t="inlineStr"><is><t>Deşarj derinlik</t></is></c></row>
            <row r="4"><c r="A4" t="s"><v>0</v></c><c r="B4"><v>414222.37</v></c><c r="C4" t="inlineStr"><is><t>4.360.474,879</t></is></c><c r="G4"><f>100+1.5</f><v>101.5</v></c><c r="I4"><v>1.25</v></c></row>
            <row r="6"><c r="A6" s="1"><v>7</v></c><c r="B6"><f>1-1</f><v>0</v></c></row>
        """.trimIndent()
        val sheet = StakeoutWorkbook.read(ByteArrayInputStream(xlsx(rows)), "tutanak.xlsm").sheets.single()
        val result = StakeoutWorkbook.preview(sheet, StakeoutWorkbook.defaultMapping(sheet))
        assertTrue(result.issues.toString(), result.canImport)
        assertEquals(listOf(4, 6), result.rows.map { it.excelRow })
        assertEquals(1, result.ignoredRowCount)
        assertEquals("A01", result.rows[0].manhole.name)
        assertEquals(4360474.879, result.rows[0].manhole.projectX!!, 0.000001)
        assertEquals(100.25, result.rows[0].manhole.terrainInvertLevel!!, 0.000001)
        assertEquals("0007", result.rows[1].manhole.name)
        assertEquals(0.0, result.rows[1].manhole.projectY!!, 0.0)
    }

    @Test
    fun selectedUncachedFormulaAndExcelErrorsBlockImportButCanBeUnmapped() {
        val rows = """<row r="3"><c r="A3" t="inlineStr"><is><t>Baca no</t></is></c></row>
            <row r="4"><c r="A4" t="inlineStr"><is><t>A1</t></is></c><c r="B4"><f>12+3</f></c><c r="C4" t="e"><v>#DIV/0!</v></c></row>"""
        val sheet = StakeoutWorkbook.read(ByteArrayInputStream(xlsx(rows)), "test.xlsx").sheets.single()
        val mapping = StakeoutWorkbook.defaultMapping(sheet)
        val result = StakeoutWorkbook.preview(sheet, mapping)
        assertFalse(result.canImport)
        assertTrue(result.issues.any { "hesaplanmış sonucu yok" in it })
        assertTrue(result.issues.any { "#DIV/0!" in it })
        val ignored = StakeoutWorkbook.preview(sheet, mapping + mapOf(StakeoutField.PROJECT_Y to null, StakeoutField.PROJECT_X to null))
        assertTrue(ignored.canImport)
        assertNull(ignored.rows.single().manhole.projectY)
    }

    @Test
    fun mappingCanReorderAndIgnoreAdditionalColumns() {
        val sheet = sheet(listOf("1.234,50", "ignored text", "A1", "100,75", "1,5", "extra"))
        val mapping = StakeoutField.entries.associateWith<StakeoutField, Int?> { null } + mapOf(
            StakeoutField.NAME to 2, StakeoutField.PROJECT_Y to 0,
            StakeoutField.TERRAIN_GROUND to 3, StakeoutField.DISCHARGE_DEPTH to 4
        )
        val result = StakeoutWorkbook.preview(sheet, mapping)
        assertTrue(result.issues.toString(), result.canImport)
        assertEquals("A1", result.rows.single().manhole.name)
        assertEquals(1234.5, result.rows.single().manhole.projectY!!, 0.0)
        assertEquals(99.25, result.rows.single().manhole.terrainInvertLevel!!, 0.0)
        assertNull(result.rows.single().manhole.projectX)
    }

    @Test
    fun validatesDuplicatesMissingNamesAndInvalidNumbersWithoutSilentPartialImport() {
        val sheet = sheet(
            listOf(" A1 ", "12,5", "0", "", "", "", "100", "", "1"),
            listOf("a1", "bozuk", "", "", "", "", "", "", "-1"),
            listOf("", "100", "", "", "", "", "", "", ""),
            emptyList()
        )
        val result = StakeoutWorkbook.preview(sheet, StakeoutWorkbook.defaultMapping(sheet))
        assertFalse(result.canImport)
        assertEquals(3, result.rows.size)
        assertEquals(1, result.ignoredRowCount)
        assertTrue(result.issues.any { "tekrarlanıyor" in it && "5. satır" in it })
        assertTrue(result.issues.any { "geçerli bir sayı değil" in it })
        assertTrue(result.issues.any { "negatif olamaz" in it })
        assertTrue(result.issues.any { "6. satır: Baca no boş" in it })
    }

    @Test
    fun acceptsTurkishAndInternationalDecimalsAndRejectsNonFiniteValues() {
        mapOf("1.234,567" to 1234.567, "1,234.567" to 1234.567, "1 234,5" to 1234.5,
            "1\u00A0234,5" to 1234.5, "1.234.567" to 1234567.0, "1,250" to 1.25,
            "1.250" to 1.25, "0" to 0.0, "−1,5" to -1.5, "1.25E3" to 1250.0).forEach { (text, expected) ->
            assertEquals(text, expected, StakeoutWorkbook.parseNumber(text), 0.000001)
        }
        listOf("NaN", "Infinity", "1e9999", "12.34,56", "1,2,3", "", "1m").forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { StakeoutWorkbook.parseNumber(invalid) }
        }
    }

    @Test
    fun invalidOrDuplicateColumnMappingsAndMissingDataBlockImport() {
        val sheet = sheet(listOf("A1", "1"))
        val mapping = StakeoutWorkbook.defaultMapping(sheet)
        assertFalse(StakeoutWorkbook.preview(sheet, mapping + (StakeoutField.NAME to null)).canImport)
        assertFalse(StakeoutWorkbook.preview(sheet, mapping + (StakeoutField.PROJECT_Y to 0)).canImport)
        assertFalse(StakeoutWorkbook.preview(sheet, mapping + (StakeoutField.PROJECT_Y to 128)).canImport)
        assertFalse(StakeoutWorkbook.preview(sheet(), mapping).canImport)
    }

    @Test
    fun enforcesFileAndSheetLimitsAndRejectsXmlEntities() {
        assertThrows(IllegalArgumentException::class.java) {
            StakeoutWorkbook.read(ByteArrayInputStream(ByteArray(StakeoutWorkbook.MAX_FILE_BYTES + 1)), "large.xlsx")
        }
        assertThrows(IllegalArgumentException::class.java) {
            StakeoutWorkbook.read(ByteArrayInputStream(xlsx("<row r=\"1048576\"><c r=\"A1048576\"><v>1</v></c></row>")), "large.xlsx")
        }
        assertThrows(IllegalArgumentException::class.java) {
            StakeoutWorkbook.read(ByteArrayInputStream(xlsx("", "<!DOCTYPE worksheet [<!ENTITY x 'EXPANDED'>]>")), "entity.xlsx")
        }
        assertThrows(IllegalArgumentException::class.java) {
            StakeoutWorkbook.read(ByteArrayInputStream("plain text".toByteArray()), "fake.xlsx")
        }
    }

    private fun sheet(vararg data: List<String>) = StakeoutSheet("Test", listOf(
        listOf("Aplikasyon tutanağı"), emptyList(), StakeoutField.entries.map { it.label }
    ) + data.toList())

    /** Actual OOXML package, with shared/inline strings and explicit formula cache fixtures. */
    private fun xlsx(rows: String, doctype: String = ""): ByteArray {
        val namespace = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        val entries = mapOf(
            "[Content_Types].xml" to """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="xml" ContentType="application/xml"/><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""",
            "_rels/.rels" to """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            "xl/workbook.xml" to """<workbook xmlns="$namespace" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Merkez" sheetId="1" r:id="rId1"/></sheets></workbook>""",
            "xl/_rels/workbook.xml.rels" to """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""",
            "xl/sharedStrings.xml" to """<sst xmlns="$namespace" count="1" uniqueCount="1"><si><r><t>A</t></r><r><t>01</t></r></si></sst>""",
            "xl/styles.xml" to """<styleSheet xmlns="$namespace"><numFmts count="1"><numFmt numFmtId="164" formatCode="0000"/></numFmts><cellXfs count="2"><xf numFmtId="0"/><xf numFmtId="164"/></cellXfs></styleSheet>""",
            "xl/worksheets/sheet1.xml" to """$doctype<worksheet xmlns="$namespace"><sheetData>$rows</sheetData></worksheet>"""
        )
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun zipEntries(bytes: ByteArray): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        val archive = java.io.File.createTempFile("xlsx-test-", ".zip")
        try {
            archive.writeBytes(bytes)
            java.util.zip.ZipFile(archive).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    entries[entry.name] = zip.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
                }
            }
        } finally { archive.delete() }
        return entries
    }
}
