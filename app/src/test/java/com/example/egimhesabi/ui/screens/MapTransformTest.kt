package com.example.egimhesabi.ui.screens

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class MapTransformTest {

    @Test
    fun zoomAroundOffCenterCentroid_keepsTouchedWorldPointStationary() {
        val pan = calculateCentroidLockedPan(
            currentPan = Offset.Zero,
            viewportCenter = Offset(500f, 500f),
            gestureCentroid = Offset(750f, 500f),
            panChange = Offset.Zero,
            appliedZoomChange = 2f
        )

        assertEquals(-250f, pan.x, 0.001f)
        assertEquals(0f, pan.y, 0.001f)
    }

    @Test
    fun zoomAndPan_areAppliedInTheSameGestureFrame() {
        val pan = calculateCentroidLockedPan(
            currentPan = Offset(30f, -20f),
            viewportCenter = Offset(500f, 400f),
            gestureCentroid = Offset(600f, 500f),
            panChange = Offset(12f, -8f),
            appliedZoomChange = 1.5f
        )

        assertEquals(7f, pan.x, 0.001f)
        assertEquals(-88f, pan.y, 0.001f)
    }
}
