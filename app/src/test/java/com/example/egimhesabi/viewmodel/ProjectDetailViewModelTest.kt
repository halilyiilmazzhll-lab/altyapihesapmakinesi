package com.example.egimhesabi.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectDetailViewModelTest {

    @Test
    fun distance_usesPythagoreanCalculation() {
        val distance = ProjectDetailViewModel.distance(0.0, 0.0, 3.0, 4.0)

        assertEquals(5.0, distance, 0.0001)
    }
}
