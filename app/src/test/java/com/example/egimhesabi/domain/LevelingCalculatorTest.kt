package com.example.egimhesabi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelingCalculatorTest {

    @Test
    fun instrumentElevation_addsRsElevationAndBacksight() {
        val result = LevelingCalculator.instrumentElevation(100.000, 1.325)
        assertEquals(101.325, result!!, 0.000001)
    }

    @Test
    fun designElevation_interpolatesDescendingAscendingAndFlatLines() {
        assertEquals(99.5, LevelingCalculator.designElevation(100.0, 99.0, 10.0, 5.0)!!, 0.000001)
        assertEquals(100.5, LevelingCalculator.designElevation(100.0, 101.0, 10.0, 5.0)!!, 0.000001)
        assertEquals(100.0, LevelingCalculator.designElevation(100.0, 100.0, 10.0, 7.0)!!, 0.000001)
        assertEquals(100.0, LevelingCalculator.designElevation(100.0, 99.0, 10.0, 0.0)!!, 0.000001)
        assertEquals(99.0, LevelingCalculator.designElevation(100.0, 99.0, 10.0, 10.0)!!, 0.000001)
    }

    @Test
    fun pipePoints_exactMultipleCreatesTwentyPipeEnds() {
        val points = LevelingCalculator.generatePipePoints(
            startElevation = 100.0,
            endElevation = 99.0,
            totalDistance = 30.0,
            instrumentElevation = 102.0
        )

        assertEquals(20, points.size)
        assertEquals(1.5, points.first().distance, 0.000001)
        assertEquals(30.0, points.last().distance, 0.000001)
        assertEquals(99.0, points.last().designElevation, 0.000001)
        assertEquals(3.0, points.last().expectedStaffReading!!, 0.000001)
    }

    @Test
    fun pipePoints_remainderCreatesShortLastPartAtBaca2() {
        val points = LevelingCalculator.generatePipePoints(
            startElevation = 100.0,
            endElevation = 99.0,
            totalDistance = 10.0,
            instrumentElevation = 102.0
        )

        assertEquals(7, points.size)
        assertEquals(1.5, points[5].segmentLength, 0.000001)
        assertEquals(1.0, points.last().segmentLength, 0.000001)
        assertEquals(10.0, points.last().distance, 0.000001)
        assertEquals(99.0, points.last().designElevation, 0.000001)
    }

    @Test
    fun meterPoints_includeStartWholeMetersAndNonWholeEnd() {
        val points = LevelingCalculator.generateMeterPoints(
            startElevation = 100.0,
            endElevation = 98.8,
            totalDistance = 2.4,
            instrumentElevation = 102.0
        )

        assertEquals(listOf(0.0, 1.0, 2.0, 2.4), points.map { it.distance })
        assertEquals(98.8, points.last().designElevation, 0.000001)
    }

    @Test
    fun correction_returnsUnambiguousFieldDirection() {
        val targetElevation = 100.0

        val raise = LevelingCalculator.correction(targetElevation, actualElevation = 99.95)!!
        assertEquals(CorrectionDirection.UP, raise.direction)
        assertEquals(0.05, raise.amountMeters, 0.000001)

        val lower = LevelingCalculator.correction(targetElevation, actualElevation = 100.03)!!
        assertEquals(CorrectionDirection.DOWN, lower.direction)
        assertEquals(0.03, lower.amountMeters, 0.000001)

        val exact = LevelingCalculator.correction(targetElevation, actualElevation = 100.0)!!
        assertEquals(CorrectionDirection.ON_GRADE, exact.direction)
    }

    @Test
    fun expectedAndActualStaffReadings_useCorrectSigns() {
        assertEquals(1.885, LevelingCalculator.expectedStaffReading(101.885, 100.0)!!, 0.000001)
        assertEquals(99.95, LevelingCalculator.pointElevation(101.885, 1.935)!!, 0.000001)
        assertTrue(LevelingCalculator.expectedStaffReading(99.0, 100.0)!! < 0.0)
    }

    @Test
    fun pipeTopOffset_usesFortyCentimetersAboveInvertForTargetMira() {
        val points = LevelingCalculator.generatePipePoints(
            startElevation = 100.0,
            endElevation = 99.0,
            totalDistance = 3.0,
            instrumentElevation = 102.0,
            staffContactOffset = PIPE_TOP_OFFSET_METERS
        )

        assertEquals(99.5, points.first().designElevation, 0.000001)
        assertEquals(99.9, LevelingCalculator.staffContactElevation(99.5, 0.4)!!, 0.000001)
        assertEquals(2.1, points.first().expectedStaffReading!!, 0.000001)
    }

    @Test
    fun negativeElevationsAreValidButInvalidReadingsAndDistancesAreRejected() {
        assertEquals(-1.25, LevelingCalculator.instrumentElevation(-2.5, 1.25)!!, 0.000001)
        assertNull(LevelingCalculator.instrumentElevation(100.0, -0.001))
        assertNull(LevelingCalculator.pointElevation(101.0, -1.0))
        assertNull(LevelingCalculator.designElevation(100.0, 99.0, 0.0, 0.0))
        assertNull(LevelingCalculator.pipeCount(-10.0))
        assertNull(LevelingCalculator.instrumentElevation(Double.NaN, 1.0))
    }
}
