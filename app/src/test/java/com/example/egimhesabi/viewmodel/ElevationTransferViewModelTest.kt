package com.example.egimhesabi.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevationTransferViewModelTest {

    @Test
    fun rsAndTargetReadingsTransferElevationWithTurkishDecimals() {
        val viewModel = ElevationTransferViewModel()
        viewModel.updateRsElevation("100,000")
        viewModel.updateRsReading("1,325")
        viewModel.updateTargetReading("0,875")

        val state = viewModel.uiState.value
        assertEquals(101.325, state.instrumentElevation!!, 0.000001)
        assertEquals(100.450, state.targetElevation!!, 0.000001)
        assertEquals(0.450, state.heightDifference!!, 0.000001)
    }

    @Test
    fun negativeRsElevationIsSupported() {
        val viewModel = ElevationTransferViewModel()
        viewModel.updateRsElevation("2,500")
        viewModel.toggleRsSign()
        viewModel.updateRsReading("1,250")
        viewModel.updateTargetReading("2,000")

        assertEquals(-3.250, viewModel.uiState.value.targetElevation!!, 0.000001)
        assertFalse(viewModel.uiState.value.rsElevationError)
    }

    @Test
    fun invalidNegativeMiraShowsErrorAndClearsResult() {
        val viewModel = ElevationTransferViewModel()
        viewModel.updateRsElevation("100")
        viewModel.updateRsReading("1")
        viewModel.updateTargetReading("-0,5")

        assertTrue(viewModel.uiState.value.targetReadingError)
        assertNull(viewModel.uiState.value.targetElevation)
    }

    @Test
    fun clearAllResetsTransfer() {
        val viewModel = ElevationTransferViewModel()
        viewModel.updateRsElevation("100")
        viewModel.updateRsReading("1")
        viewModel.updateTargetReading("2")
        viewModel.clearAll()

        assertEquals("", viewModel.uiState.value.rsElevationInput)
        assertNull(viewModel.uiState.value.instrumentElevation)
        assertNull(viewModel.uiState.value.targetElevation)
    }
}
