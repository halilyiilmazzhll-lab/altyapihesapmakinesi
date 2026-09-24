package com.example.egimhesabi.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.egimhesabi.domain.ProfileSampling
import com.example.egimhesabi.domain.ReverseCalculator
import org.junit.Assert.*
import org.junit.Test

class CalculationRegressionTest {
    @Test fun changingSlopeUnitPreservesElevation() {
        val vm = ReverseCalculationViewModel()
        vm.updateStartKot("100")
        vm.updateDistance("1000")
        vm.toggleteSlopeMode()
        vm.updateSlopeInput("300")
        val original = vm.uiState.value.resultKot!!
        repeat(20) { vm.toggleteSlopeMode() }
        assertEquals(original, vm.uiState.value.resultKot!!, 1e-10)
        vm.toggleteSlopeMode()
        assertEquals(original, vm.uiState.value.resultKot!!, 1e-10)
    }

    @Test fun manualDistanceIsNotChangedByTotalDistanceAndInvalidInputClearsResult() {
        val vm = InterpolationViewModel()
        vm.updateKot1("100"); vm.updateKot2("90"); vm.updateTotaleDistance("100")
        vm.updateManualeDistance("25")
        vm.updateTotaleDistance("200")
        assertEquals(25.0, vm.uiState.value.currentDistance!!, 0.0)
        assertEquals(98.75, vm.uiState.value.currentKot!!, 1e-10)
        vm.updateManualeDistance("250")
        assertNull(vm.uiState.value.currentKot)
        vm.updateManualeDistance("abc")
        assertNull(vm.uiState.value.currentKot)
    }

    @Test fun badInputClearsPreviouslyGeneratedTable() {
        val vm = ReverseCalculationViewModel()
        vm.updateStartKot("100"); vm.updateSlopeInput("1"); vm.updateDistance("100")
        vm.toggleteTablee()
        assertFalse(vm.uiState.value.tablePoints.isEmpty())
        vm.updateStartKot("")
        assertTrue(vm.uiState.value.tablePoints.isEmpty())
    }

    @Test fun hugeOrInvalidProfilesAreRejectedBeforeAllocation() {
        assertTrue(ProfileSampling.distances(1e300, 1.0).isEmpty())
        assertTrue(ProfileSampling.distances(1.0, Double.MIN_VALUE).isEmpty())
        assertTrue(ProfileSampling.distances(Double.NaN, 1.0).isEmpty())
        assertEquals(listOf(0.0, 3.0, 6.0, 9.0, 10.0), ProfileSampling.distances(10.0, 3.0))
        assertEquals(ProfileSampling.MAX_POINTS, ProfileSampling.distances(15000.0, 1.0).size)
    }

    @Test fun restoredManualDistanceAndZeroSlopeWork() {
        val vm = InterpolationViewModel(SavedStateHandle(mapOf("kot1" to "100", "kot2" to "90", "distance" to "100", "manual" to "25")))
        assertEquals(97.5, vm.uiState.value.currentKot!!, 1e-10)
        val reverse = ReverseCalculationViewModel()
        reverse.updateStartKot("100"); reverse.updateDistance("10"); reverse.updateSlopeInput("0")
        assertEquals(100.0, reverse.uiState.value.resultKot!!, 0.0)
    }
}
