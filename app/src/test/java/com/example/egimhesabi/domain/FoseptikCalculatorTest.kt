package com.example.egimhesabi.domain

import org.junit.Assert.*
import org.junit.Test

class FoseptikCalculatorTest {

    @Test
    fun testNormalCalculation() {
        val result = FoseptikCalculator.calculate(
            manholeInvert = 100.0,
            distance = 50.0,
            slopeType = 1, // %
            slopeVal = 2.0, // %2
            groundElev = 102.0,
            tankHeight = 3.0,
            coverToInlet = 0.5
        )
        
        // Eğim ondalık: 2 / 100 = 0.02
        assertEquals(0.02, result.slopeDecimal!!, 0.001)
        
        // Akar kotu: 100.0 - (50 * 0.02) = 100.0 - 1.0 = 99.0
        assertEquals(99.0, result.inletElev!!, 0.001)
        
        // Kapak kotu: 99.0 + 0.5 = 99.5
        assertEquals(99.5, result.coverElev!!, 0.001)
        
        // Taban kotu: 99.5 - 3.0 = 96.5
        assertEquals(96.5, result.bottomElev!!, 0.001)
        
        // Kazı derinliği: 102.0 - 96.5 = 5.5
        assertEquals(5.5, result.excavationDepth!!, 0.001)
        
        assertNull(result.errorMessage)
    }

    @Test
    fun testSlopeType1OverX() {
        val result = FoseptikCalculator.calculate(
            manholeInvert = 100.0,
            distance = 100.0,
            slopeType = 0, // 1/x
            slopeVal = 200.0, // 1/200 = 0.005
            groundElev = null,
            tankHeight = null,
            coverToInlet = null
        )
        
        assertEquals(0.005, result.slopeDecimal!!, 0.0001)
        assertEquals(99.5, result.inletElev!!, 0.001)
    }

    @Test
    fun testEdgeCaseZeroSlope() {
        val result1 = FoseptikCalculator.calculate(
            manholeInvert = 100.0, distance = 50.0, slopeType = 0, slopeVal = 0.0, // 1/0
            groundElev = null, tankHeight = null, coverToInlet = null
        )
        assertEquals(0.0, result1.slopeDecimal!!, 0.001)
        assertEquals(100.0, result1.inletElev!!, 0.001)

        val result2 = FoseptikCalculator.calculate(
            manholeInvert = 100.0, distance = 50.0, slopeType = 1, slopeVal = 0.0, // %0
            groundElev = null, tankHeight = null, coverToInlet = null
        )
        assertEquals(0.0, result2.slopeDecimal!!, 0.001)
        assertEquals(100.0, result2.inletElev!!, 0.001)
    }

    @Test
    fun testEdgeCaseNegativeDistance() {
        val result = FoseptikCalculator.calculate(
            manholeInvert = 100.0, distance = -10.0, slopeType = 1, slopeVal = 2.0,
            groundElev = null, tankHeight = null, coverToInlet = null
        )
        assertNotNull(result.errorMessage)
        assertEquals("Mesafe negatif olamaz.", result.errorMessage)
    }

    @Test
    fun testEdgeCaseInvalidTankDimensions() {
        // coverToInlet > tankHeight
        val result1 = FoseptikCalculator.calculate(
            manholeInvert = 100.0, distance = 10.0, slopeType = 1, slopeVal = 1.0,
            groundElev = null, tankHeight = 3.0, coverToInlet = 28.0
        )
        assertNotNull(result1.errorMessage)
        assertEquals("Kapak-akar mesafesi, toplam foseptik boyundan büyük olamaz.", result1.errorMessage)
        // Note: inletElev is still calculated
        assertNotNull(result1.inletElev)
        assertNull(result1.bottomElev)

        // Negative dimensions
        val result2 = FoseptikCalculator.calculate(
            manholeInvert = 100.0, distance = 10.0, slopeType = 1, slopeVal = 1.0,
            groundElev = null, tankHeight = -3.0, coverToInlet = 0.5
        )
        assertNotNull(result2.errorMessage)
        assertEquals("Foseptik boyutları negatif olamaz.", result2.errorMessage)
    }
}
