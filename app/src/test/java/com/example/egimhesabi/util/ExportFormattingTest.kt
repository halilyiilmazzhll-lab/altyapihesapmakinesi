package com.example.egimhesabi.util

import org.junit.Assert.*
import org.junit.Test

class ExportFormattingTest {
    @Test fun worksheetNamesAreValidAndRemainUniqueAfterTruncation() {
        val used = mutableSetOf<String>()
        val first = ExcelExportHelper.uniqueSheetName("A".repeat(40), used)
        val second = ExcelExportHelper.uniqueSheetName("A".repeat(35) + "B", used)
        assertNotEquals(first, second)
        assertTrue(first.length <= 31 && second.length <= 31)
        val cleaned = ExcelExportHelper.uniqueSheetName("':/?*[]\\'", used)
        assertFalse(cleaned.any { it in ":/?*[]\\'" })
    }

    @Test fun reportTextCannotChangeHtmlStructure() {
        assertEquals("&lt;b&gt;A &amp; B&lt;/b&gt;&quot;&#39;", HtmlReportGenerator.escapeHtml("<b>A & B</b>\"'"))
    }

    @Test fun fileSizeLimitIsEnforcedWhileReading() {
        assertArrayEquals(byteArrayOf(1, 2), BoundedInput.read(byteArrayOf(1, 2).inputStream(), 2))
        try {
            BoundedInput.read(byteArrayOf(1, 2, 3).inputStream(), 2)
            fail("Oversize input should be rejected")
        } catch (_: IllegalArgumentException) { }
    }
}
