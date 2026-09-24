package com.example.egimhesabi

import com.example.egimhesabi.util.impactPdfManholeTitle
import com.example.egimhesabi.util.impactPdfSegmentLabel
import com.example.egimhesabi.viewmodel.ManholeNode
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfTest {
    @Test
    fun impactPdfLabels_includeEnteredManholeNames() {
        val nodes = listOf(
            ManholeNode(name = "A1", invertText = "97.00", distanceToNextText = "30.00"),
            ManholeNode(name = "A2", invertText = "98.00", distanceToNextText = "30.00"),
            ManholeNode(name = "A3", invertText = "99.00")
        )

        assertEquals("1. Baca (A1) (Sabit)", impactPdfManholeTitle(0, nodes[0]))
        assertEquals("2. Baca (A2)", impactPdfManholeTitle(1, nodes[1]))
        assertEquals("Hat A3\u2192A2", impactPdfSegmentLabel(1, nodes, false))
        assertEquals("Hat A1\u2192A2", impactPdfSegmentLabel(0, nodes, true))
    }

    @Test
    fun impactPdfLabels_fallBackToSequenceNumbersWhenNamesAreBlank() {
        val nodes = listOf(
            ManholeNode(name = " ", invertText = "99.00", distanceToNextText = "30.00"),
            ManholeNode(invertText = "98.00")
        )

        assertEquals("1. Baca (Sabit)", impactPdfManholeTitle(0, nodes[0]))
        assertEquals("Hat 1\u21922", impactPdfSegmentLabel(0, nodes, true))
    }
}
