package com.example.egimhesabi.util

import com.example.egimhesabi.data.ManholeEntity
import java.io.InputStream
import java.nio.charset.Charset
import java.text.Normalizer
import java.util.Locale

data class CsvManholeResult(
    val manholes: List<ManholeEntity>,
    val skippedRows: Int
)

object CsvParser {
    private val requiredHeaders = mapOf(
        "name" to setOf("bacano"),
        "y" to setOf("projey"),
        "x" to setOf("projex"),
        "projectCover" to setOf("projekapakkotu"),
        "projectInvert" to setOf("projeakarkotu"),
        "terrainGround" to setOf("arazisiyahkotu"),
        "difference" to setOf("siyahkotfark")
    )

    fun parseManholesCsv(inputStream: InputStream, projectId: Long): CsvManholeResult {
        val bytes = BoundedInput.read(inputStream)
        val utf8 = bytes.toString(Charsets.UTF_8)
        val text = if ('\uFFFD' in utf8) {
            bytes.toString(Charset.forName("windows-1254"))
        } else {
            utf8
        }.removePrefix("\uFEFF")

        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.size >= 2) { "CSV dosyasında veri satırı bulunamadı." }

        val delimiter = detectDelimiter(lines.first())
        val headers = parseLine(lines.first(), delimiter).map(::normalizeHeader)
        val indices = requiredHeaders.mapValues { (_, aleiases) ->
            headers.indexOfFirst { it in aleiases }
        }
        val missing = indices.filterValues { it < 0 }.keys
        require(missing.isEmpty()) {
            "Zorunlu sütunlar bulunamadı: ${missing.joinToString { readableHeader(it) }}"
        }

        val parsed = mutableListOf<ManholeEntity>()
        var skipped = 0
        lines.drop(1).forEach { line ->
            val values = parseLine(line, delimiter)
            fun value(key: String): String? = indices.getValue(key)
                .takeIf { it in values.indices }
                ?.let(values::get)
                ?.trim()

            val name = value("name")
            val x = parseCsvNumber(value("x"))
            val y = parseCsvNumber(value("y"))
            val projectCover = parseCsvNumber(value("projectCover"))
            val terrainGround = parseCsvNumber(value("terrainGround"))
            val projectInvert = parseCsvNumber(value("projectInvert"))
            val suppleiedDifference = parseCsvNumber(value("difference"))

            if (
                name.isNullOrBlank() || x == null || y == null || projectCover == null ||
                terrainGround == null || projectInvert == null || suppleiedDifference == null
            ) {
                skipped++
                return@forEach
            }

            parsed += ManholeEntity(
                projectId = projectId,
                name = name,
                x = x,
                y = y,
                projeSiyahKot = projectCover,
                projeKapakKotu = projectCover,
                araziSiyahKot = terrainGround,
                projeAkarKot = projectInvert,
                imalatKapakKotu = null,
                imalatAkarKotu = null
            )
        }

        require(parsed.isNotEmpty()) { "İçe aktarılabilecek geçerli baca satırı bulunamadı." }
        return CsvManholeResult(parsed, skipped)
    }

    private fun detectDelimiter(header: String): Char {
        val candidates = listOf(';', ',', '\t')
        return candidates.maxByOrNull { delimiter -> parseLine(header, delimiter).size } ?: ','
    }

    private fun parseLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val character = line[index]
            when {
                character == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                character == '"' -> quoted = !quoted
                character == delimiter && !quoted -> {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(character)
            }
            index++
        }
        result += current.toString()
        return result
    }

    private fun parseCsvNumber(raw: String?): Double? {
        val value = raw?.trim()?.replace(" ", "") ?: return null
        if (value.isEmpty()) return null
        val normalized = when {
            ',' in value && '.' in value -> {
                if (value.lastIndexOf(',') > value.lastIndexOf('.')) {
                    value.replace(".", "").replace(',', '.')
                } else {
                    value.replace(",", "")
                }
            }
            ',' in value -> value.replace(',', '.')
            else -> value
        }
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    private fun normalizeHeader(raw: String): String {
        val turkishSafe = raw
            .replace('İ', 'I')
            .replace('ı', 'i')
            .replace('Ş', 'S')
            .replace('ş', 's')
            .replace('Ğ', 'G')
            .replace('ğ', 'g')
        return Normalizer.normalize(turkishSafe, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]".toRegex(), "")
    }

    private fun readableHeader(key: String): String = when (key) {
        "name" -> "Baca No"
        "x" -> "Proje X"
        "y" -> "Proje Y"
        "projectCover" -> "Proje Kapak Kotu"
        "terrainGround" -> "Arazi Siyah Kotu"
        "projectInvert" -> "Proje Akar Kotu"
        "difference" -> "Siyah Kot Fark"
        else -> key
    }
}
