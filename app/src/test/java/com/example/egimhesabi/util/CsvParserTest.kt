package com.example.egimhesabi.util

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvParserTest {

    @Test
    fun parsesExcelSemicolonCsvWithTurkishDecimalValues() {
        val csv = """
            Baca No;Proje Y;Proje X;Proje Kapak Kotu;Proje Siyah Kotu;Proje Akar Kotu;Arazi Siyah Kotu;Deşarj Kapak Kotu;Deşarj Derinlik;Siyah Kot Fark
            1;414222,370;4360474,879;739,790;739,790;737,990;739,773;;1,800;0,02
            2;414193,435;4360460,253;738,260;738,260;736,360;738,518;;1,900;-0,26
        """.trimIndent()

        val result = CsvParser.parseManholesCsv(
            ByteArrayInputStream(csv.toByteArray()),
            projectId = 7
        )

        assertEquals(2, result.manholes.size)
        assertEquals(0, result.skippedRows)
        assertEquals("1", result.manholes.first().name)
        assertEquals(4360474.879, result.manholes.first().x, 0.0001)
        assertEquals(414222.370, result.manholes.first().y, 0.0001)
        assertEquals(739.790, result.manholes.first().projeKapakKotu, 0.0001)
        assertEquals(737.990, result.manholes.first().projeAkarKot, 0.0001)
        assertEquals(739.773, result.manholes.first().araziSiyahKot, 0.0001)
        assertEquals(0.017, result.manholes.first().projeAraziFarki, 0.0001)
    }

    @Test
    fun skipsInvalidRowsUsingOnlyRequiredProjectFormat() {
        val csv = """
            Baca No,Proje Y,Proje X,Proje Kapak Kotu,Proje Akar Kotu,Arazi Siyah Kotu,Siyah Kot Fark
            B-01,20.0,10.0,101.5,98.0,100.0,1.5
            B-02,21.0,bozuk,102.0,98.5,100.5,1.5
        """.trimIndent()

        val result = CsvParser.parseManholesCsv(
            ByteArrayInputStream(csv.toByteArray()),
            projectId = 4
        )

        assertEquals(1, result.manholes.size)
        assertEquals(1, result.skippedRows)
        assertEquals(1.5, result.manholes.first().projeAraziFarki, 0.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLegacyOrRenamedHeaders() {
        val csv = """
            Baca,X,Y,Proje Siyah Kot,Arazi Siyah Kot,Proje Akar Kot
            B-01,10,20,101.5,100,98
        """.trimIndent()

        CsvParser.parseManholesCsv(
            ByteArrayInputStream(csv.toByteArray()),
            projectId = 4
        )
    }
}
