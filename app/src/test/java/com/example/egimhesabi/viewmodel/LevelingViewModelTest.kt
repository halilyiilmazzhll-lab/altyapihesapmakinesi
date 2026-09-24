package com.example.egimhesabi.viewmodel

import com.example.egimhesabi.domain.CorrectionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelingViewModelTest {

    @Test
    fun turkishInputs_buildRsAndPipePlan() {
        val viewModel = configuredViewModel()
        val state = viewModel.uiState.value

        assertTrue(state.isInstrumentSet)
        assertTrue(state.isLineReady)
        assertEquals(102.0, state.instrumentElevation!!, 0.000001)
        assertEquals(2, state.pipeCount)
        assertEquals(99.5, state.pipePoints.first().designElevation, 0.000001)
        assertEquals(2.1, state.pipePoints.first().expectedStaffReading!!, 0.000001)
        assertEquals(99.0, state.pipePoints.last().designElevation, 0.000001)
    }

    @Test
    fun actualReading_reportsThreeCentimetersDown() {
        val viewModel = configuredViewModel()
        viewModel.selectPipe(1)
        viewModel.updateActualReading("2,070")
        viewModel.checkActualReading()

        val state = viewModel.uiState.value
        assertEquals(99.930, state.actualPipeTopElevation!!, 0.000001)
        assertEquals(99.530, state.actualElevation!!, 0.000001)
        assertEquals(CorrectionDirection.DOWN, state.correction!!.direction)
        assertEquals(0.030, state.correction!!.amountMeters, 0.000001)
    }

    @Test
    fun actualReading_reportsFiveCentimetersUp() {
        val viewModel = configuredViewModel()
        viewModel.selectPipe(1)
        viewModel.updateActualReading("2,150")
        viewModel.checkActualReading()

        val state = viewModel.uiState.value
        assertEquals(99.850, state.actualPipeTopElevation!!, 0.000001)
        assertEquals(99.450, state.actualElevation!!, 0.000001)
        assertEquals(CorrectionDirection.UP, state.correction!!.direction)
        assertEquals(0.050, state.correction!!.amountMeters, 0.000001)
    }

    @Test
    fun actualReadingDoesNotIssueInstructionUntilUserChecksIt() {
        val viewModel = configuredViewModel()

        viewModel.updateActualReading("2")

        assertEquals(2.0, viewModel.uiState.value.actualReading!!, 0.000001)
        assertNull(viewModel.uiState.value.actualElevation)
        assertNull(viewModel.uiState.value.correction)
        assertFalse(viewModel.uiState.value.isReadingChecked)
    }

    @Test
    fun changingPipeOrRs_clearsOldActualReading() {
        val viewModel = configuredViewModel()
        viewModel.updateActualReading("2.500")
        viewModel.checkActualReading()
        assertTrue(viewModel.uiState.value.actualReadingInput.isNotEmpty())
        assertTrue(viewModel.uiState.value.isReadingChecked)

        viewModel.selectNextPipe()
        assertEquals("", viewModel.uiState.value.actualReadingInput)
        assertNull(viewModel.uiState.value.correction)

        viewModel.updateActualReading("3.000")
        viewModel.checkActualReading()
        viewModel.updateRsBacksight("2.100")
        assertEquals("", viewModel.uiState.value.actualReadingInput)
        assertNull(viewModel.uiState.value.correction)
    }

    @Test
    fun changingLineKeepsSelectionInRangeAndClearsCorrection() {
        val viewModel = configuredViewModel()
        viewModel.selectPipe(2)
        viewModel.updateActualReading("3.0")
        viewModel.checkActualReading()

        viewModel.updateDistance("1,5")

        val state = viewModel.uiState.value
        assertEquals(1, state.pipeCount)
        assertEquals(1, state.selectedPipeNumber)
        assertEquals("", state.actualReadingInput)
        assertNull(state.correction)
    }

    @Test
    fun customPipeLengthAndContactOffsetSupportDifferentPipeTypes() {
        val viewModel = LevelingViewModel().apply {
            updateRsElevation("100")
            updateRsBacksight("2")
            updateManhole1Elevation("100")
            updateManhole2Elevation("99")
            updateDistance("21")
            updatePipeLength("7")
            updatePipeTopOffset("1,20")
        }

        val state = viewModel.uiState.value
        assertTrue(state.isLineReady)
        assertEquals(7.0, state.pipeLength!!, 0.000001)
        assertEquals(1.2, state.pipeTopOffset!!, 0.000001)
        assertEquals(3, state.pipeCount)
        assertEquals(listOf(7.0, 14.0, 21.0), state.pipePoints.map { it.distance })
        assertEquals(1.8, state.pipePoints.last().expectedStaffReading!!, 0.000001)
    }

    @Test
    fun signTogglesSupportNegativeRsAndManholeElevations() {
        val viewModel = LevelingViewModel()
        viewModel.updateRsElevation("2,500")
        viewModel.toggleRsSign()
        viewModel.updateRsBacksight("1,250")
        viewModel.updateManhole1Elevation("3,000")
        viewModel.toggleManhole1Sign()
        viewModel.updateManhole2Elevation("4,000")
        viewModel.toggleManhole2Sign()
        viewModel.updateDistance("1,5")

        val state = viewModel.uiState.value
        assertEquals(-1.250, state.instrumentElevation!!, 0.000001)
        assertEquals(-3.0, state.manhole1Elevation!!, 0.000001)
        assertEquals(-4.0, state.manhole2Elevation!!, 0.000001)
        assertFalse(state.rsElevationError)
    }

    @Test
    fun invalidDistanceAndReadingSetErrorsWithoutCrashing() {
        val viewModel = configuredViewModel()
        viewModel.updateDistance("0")
        assertTrue(viewModel.uiState.value.distanceError)
        assertFalse(viewModel.uiState.value.isLineReady)

        viewModel.updateDistance("3")
        viewModel.updateActualReading("-1")
        assertTrue(viewModel.uiState.value.actualReadingError)
        assertNull(viewModel.uiState.value.actualElevation)
    }

    @Test
    fun clearAllResetsThePhysicalSetup() {
        val viewModel = configuredViewModel()
        viewModel.updateActualReading("2.5")
        viewModel.clearAll()

        val state = viewModel.uiState.value
        assertFalse(state.isInstrumentSet)
        assertFalse(state.isLineReady)
        assertEquals("", state.rsElevationInput)
        assertEquals("", state.manhole1ElevationInput)
        assertNull(state.selectedPipe)
        assertNull(state.correction)
    }

    private fun configuredViewModel(): LevelingViewModel {
        return LevelingViewModel().apply {
            updateRsElevation("100,000")
            updateRsBacksight("2,000")
            updateManhole1Elevation("100,000")
            updateManhole2Elevation("99,000")
            updateDistance("3,000")
        }
    }
}
