package com.example.egimhesabi.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImpactCalculationStateTest {

    @Test
    fun defaults_useInvertElevationsAndCalculateCommonMiddleRange() {
        val state = ImpactCalculationState()
        // Default nodes: 98.0, 97.4, 97.0
        val n0 = state.nodes[0]
        val n1 = state.nodes[1]
        val n2 = state.nodes[2]

        assertEquals(98.0, n0.invert!!, 0.001)
        assertEquals(97.4, n1.invert!!, 0.001)
        assertEquals(97.0, n2.invert!!, 0.001)
        assertEquals(2.0, state.getSegmentAnalysis(0).slopePercent!!, 0.001)
        assertEquals(1.3333, state.getSegmentAnalysis(1).slopePercent!!, 0.001)
        assertTrue(state.allSegmentsValid)

        val rec = state.getRecommendationFor(1)
        assertNotNull(rec)
        assertEquals(97.15, rec!!.minimumInvert, 0.001)
        assertEquals(97.85, rec.maximumInvert, 0.001)
        assertEquals(0.0, rec.recommendedDeltaCm, 0.001)
        assertEquals(1.15, rec.minimumDepth!!, 0.001)
        assertEquals(1.85, rec.maximumDepth!!, 0.001)
    }

    @Test
    fun loweringMiddleInvert_makesFirstSteeperAndSecondFlatter() {
        val original = ImpactCalculationState()
        val loweredNodes = original.nodes.toMutableList()
        loweredNodes[1] = loweredNodes[1].copy(deltaCm = -10.0)
        val lowered = original.copy(nodes = loweredNodes)

        assertTrue(lowered.getSegmentAnalysis(0).slopePercent!! > original.getSegmentAnalysis(0).slopePercent!!)
        assertTrue(lowered.getSegmentAnalysis(1).slopePercent!! < original.getSegmentAnalysis(1).slopePercent!!)
        assertEquals(2.3333, lowered.getSegmentAnalysis(0).slopePercent!!, 0.001)
        assertEquals(1.0, lowered.getSegmentAnalysis(1).slopePercent!!, 0.001)
    }

    @Test
    fun ascendingLeftToRightInverts_inferRightToLeftFlowAndAreValid() {
        val state = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "97.00", distanceToNextText = "30.00"),
                ManholeNode(name = "A2", invertText = "98.00", distanceToNextText = "30.00"),
                ManholeNode(name = "A3", invertText = "99.00")
            )
        )

        val firstSegment = state.getSegmentAnalysis(0)
        val secondSegment = state.getSegmentAnalysis(1)

        assertEquals(false, state.flowLeftToRight)
        assertEquals(ImpactSlopeStatus.VALID, firstSegment.status)
        assertEquals(ImpactSlopeStatus.VALID, secondSegment.status)
        assertEquals(3.3333, firstSegment.slopePercent!!, 0.001)
        assertEquals(3.3333, secondSegment.slopePercent!!, 0.001)
        assertTrue(state.allSegmentsValid)
    }

    @Test
    fun localRiseAgainstRightToLeftProfile_isARealReverseSlope() {
        val state = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "97.00", distanceToNextText = "30.00"),
                ManholeNode(name = "A2", invertText = "96.35", distanceToNextText = "30.00"),
                ManholeNode(name = "A3", invertText = "99.00")
            ),
            maxSlopePercent = 10.0
        )

        assertEquals(false, state.flowLeftToRight)
        assertEquals(true, state.getSegmentFlowLeftToRight(0))
        assertEquals(false, state.getSegmentFlowLeftToRight(1))
        assertEquals(ImpactSlopeStatus.REVERSE, state.getSegmentAnalysis(0).status)
        assertEquals(ImpactSlopeStatus.VALID, state.getSegmentAnalysis(1).status)
        assertEquals(2.1667, state.getSegmentAnalysis(0).slopePercent!!, 0.001)
        assertEquals(8.8333, state.getSegmentAnalysis(1).slopePercent!!, 0.001)
        assertFalse(state.allSegmentsValid)
    }

    @Test
    fun descendingLeftToRightInverts_inferLeftToRightFlowAndAreValid() {
        val state = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "99.00", distanceToNextText = "30.00"),
                ManholeNode(name = "A2", invertText = "98.00", distanceToNextText = "30.00"),
                ManholeNode(name = "A3", invertText = "97.00")
            )
        )

        assertEquals(true, state.flowLeftToRight)
        assertEquals(ImpactSlopeStatus.VALID, state.getSegmentAnalysis(0).status)
        assertEquals(ImpactSlopeStatus.VALID, state.getSegmentAnalysis(1).status)
        assertEquals(3.3333, state.getSegmentAnalysis(0).slopePercent!!, 0.001)
        assertEquals(3.3333, state.getSegmentAnalysis(1).slopePercent!!, 0.001)
        assertTrue(state.allSegmentsValid)
    }

    @Test
    fun middleSegmentAgainstInferredOverallFlow_isReverse() {
        val state = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "99.00", distanceToNextText = "100.00"),
                ManholeNode(name = "A2", invertText = "100.00", distanceToNextText = "100.00"),
                ManholeNode(name = "A3", invertText = "98.00")
            )
        )

        assertEquals(true, state.flowLeftToRight)
        assertEquals(ImpactSlopeStatus.REVERSE, state.getSegmentAnalysis(0).status)
        assertEquals(ImpactSlopeStatus.VALID, state.getSegmentAnalysis(1).status)
        assertFalse(state.allSegmentsValid)
    }

    @Test
    fun inferredRightToLeftRecommendation_usesCompatibleRangeAndKeepsCurrentMiddleInvert() {
        val state = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "97.00", distanceToNextText = "30.00"),
                ManholeNode(name = "A2", invertText = "98.00", distanceToNextText = "30.00"),
                ManholeNode(name = "A3", invertText = "99.00")
            )
        )

        val recommendation = state.getRecommendationFor(1)

        assertEquals(false, state.flowLeftToRight)
        assertNotNull(recommendation)
        assertEquals(97.50, recommendation!!.minimumInvert, 0.001)
        assertEquals(98.50, recommendation.maximumInvert, 0.001)
        assertEquals(98.00, recommendation.recommendedInvert, 0.001)
        assertEquals(0.0, recommendation.recommendedDeltaCm, 0.001)
    }

    @Test
    fun excessivelyLowMiddleInvert_createsReverseSecondSegment() {
        val state = ImpactCalculationState()
        val loweredNodes = state.nodes.toMutableList()
        loweredNodes[1] = loweredNodes[1].copy(deltaCm = -50.0)
        val testState = state.copy(nodes = loweredNodes)

        assertEquals(ImpactSlopeStatus.REVERSE, testState.getSegmentAnalysis(1).status)
        assertFalse(testState.allSegmentsValid)
    }

    @Test
    fun incompatibleEndInverts_haveNoSharedMiddleRangeWhenDifferenceTooHigh() {
        val state = ImpactCalculationState()
        val modifiedNodes = state.nodes.toMutableList()
        modifiedNodes[2] = modifiedNodes[2].copy(invertText = "93.00")
        val testState = state.copy(nodes = modifiedNodes)

        assertNull(testState.getRecommendationFor(1))
        assertFalse(testState.allSegmentsValid)
    }

    @Test
    fun optionalCoverElevations_deriveManholeDepths() {
        val state = ImpactCalculationState() // already has covers: 100, 99, 98

        assertEquals(2.0, state.nodes[0].depth!!, 0.001)
        assertEquals(1.6, state.nodes[1].depth!!, 0.001)
        assertEquals(1.0, state.nodes[2].depth!!, 0.001)
        
        val rec = state.getRecommendationFor(1)
        assertEquals(1.15, rec!!.minimumDepth!!, 0.001)
        assertEquals(1.85, rec.maximumDepth!!, 0.001)
    }
    
    @Test
    fun minManholeDepth_limitsMaximumInvert() {
        val state = ImpactCalculationState(minManholeDepthMeters = 1.7)
        // With min depth 1.7m, and cover2 = 99.00
        // maxInvert based on slope was 97.85. 
        // maxInvert based on depth is 99.00 - 1.7 = 97.30
        
        val rec = state.getRecommendationFor(1)
        assertEquals(97.15, rec!!.minimumInvert, 0.001)
        assertEquals(97.30, rec.maximumInvert, 0.001)
    }

    @Test
    fun equalAdjacentInverts_haveNoSegmentDirectionAndAreTooFlat() {
        val state = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "100.00", distanceToNextText = "30.00"),
                ManholeNode(name = "A2", invertText = "100.00", distanceToNextText = "30.00"),
                ManholeNode(name = "A3", invertText = "99.00")
            )
        )

        val segment = state.getSegmentAnalysis(0)

        assertNull(state.getSegmentFlowLeftToRight(0))
        assertEquals(0.0, segment.slopePercent!!, 0.001)
        assertNull(segment.slopeRatio)
        assertEquals(ImpactSlopeStatus.TOO_FLAT, segment.status)
    }

    @Test
    fun twoEqualEndpointInverts_haveZeroSlopeNoRatioAndNoOverallFlow() {
        val state = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "98.00", distanceToNextText = "30.00"),
                ManholeNode(name = "A2", invertText = "98.00")
            )
        )

        val segment = state.getSegmentAnalysis(0)

        assertNull(state.flowLeftToRight)
        assertNull(state.getSegmentFlowLeftToRight(0))
        assertEquals(0.0, segment.dropMeters!!, 0.001)
        assertEquals(0.0, segment.slopePercent!!, 0.001)
        assertNull(segment.slopeRatio)
        assertEquals(ImpactSlopeStatus.TOO_FLAT, segment.status)
        assertFalse(state.allSegmentsValid)
    }

    @Test
    fun equalFirstAndLastInverts_leaveOverallFlowUndetermined() {
        val state = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "100.00", distanceToNextText = "100.00"),
                ManholeNode(name = "A2", invertText = "99.00", distanceToNextText = "100.00"),
                ManholeNode(name = "A3", invertText = "100.00")
            )
        )

        assertNull(state.flowLeftToRight)
        assertEquals(true, state.getSegmentFlowLeftToRight(0))
        assertEquals(false, state.getSegmentFlowLeftToRight(1))
        assertEquals(ImpactSlopeStatus.VALID, state.getSegmentAnalysis(0).status)
        assertEquals(ImpactSlopeStatus.VALID, state.getSegmentAnalysis(1).status)
        assertNull(state.getRecommendationFor(1))
        assertFalse(state.allSegmentsValid)
    }

    @Test
    fun missingEitherEndpointInvert_makesSegmentIncompleteAndInputsInvalid() {
        val states = listOf(
            ImpactCalculationState(
                nodes = listOf(
                    ManholeNode(name = "A1", invertText = "", distanceToNextText = "30.00"),
                    ManholeNode(name = "A2", invertText = "99.00")
                )
            ),
            ImpactCalculationState(
                nodes = listOf(
                    ManholeNode(name = "A1", invertText = "100.00", distanceToNextText = "30.00"),
                    ManholeNode(name = "A2", invertText = "")
                )
            )
        )

        states.forEach { state ->
            assertEquals(ImpactSlopeStatus.INCOMPLETE, state.getSegmentAnalysis(0).status)
            assertFalse(state.allInputsValid)
        }
    }

    @Test
    fun zeroOrNegativeDistance_makesSegmentIncompleteAndInputsInvalid() {
        listOf("0", "-30.00").forEach { distance ->
            val state = ImpactCalculationState(
                nodes = listOf(
                    ManholeNode(name = "A1", invertText = "100.00", distanceToNextText = distance),
                    ManholeNode(name = "A2", invertText = "99.00")
                )
            )

            assertEquals(ImpactSlopeStatus.INCOMPLETE, state.getSegmentAnalysis(0).status)
            assertFalse(state.allInputsValid)
        }
    }

    @Test
    fun exactMinimumAndMaximumSlopeBoundaries_areValid() {
        val state = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "99.85", distanceToNextText = "30.00"),
                ManholeNode(name = "A2", invertText = "99.70", distanceToNextText = "34.00"),
                ManholeNode(name = "A3", invertText = "98.00")
            ),
            minSlopePercent = 0.5,
            maxSlopePercent = 5.0
        )

        assertEquals(0.5, state.getSegmentAnalysis(0).slopePercent!!, 0.001)
        assertEquals(ImpactSlopeStatus.VALID, state.getSegmentAnalysis(0).status)
        assertEquals(5.0, state.getSegmentAnalysis(1).slopePercent!!, 0.001)
        assertEquals(ImpactSlopeStatus.VALID, state.getSegmentAnalysis(1).status)
        assertTrue(state.allSegmentsValid)
    }

    @Test
    fun recommendation_keepsSinglePointIntersectionDespiteFloatingPointNoise() {
        val state = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "10.00", distanceToNextText = "2.00"),
                ManholeNode(name = "A2", invertText = "9.99", distanceToNextText = "8.00"),
                ManholeNode(name = "A3", invertText = "9.95")
            ),
            minSlopePercent = 0.5,
            maxSlopePercent = 0.5
        )

        val recommendation = state.getRecommendationFor(1)

        assertNotNull(recommendation)
        assertEquals(9.99, recommendation!!.minimumInvert, 1e-9)
        assertEquals(9.99, recommendation.maximumInvert, 1e-9)
        assertEquals(9.99, recommendation.recommendedInvert, 1e-9)
    }

    @Test
    fun middleRecommendation_requiresBothAdjacentDistances() {
        val missingUpstreamDistance = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "100.00", distanceToNextText = "0"),
                ManholeNode(name = "A2", invertText = "99.00", distanceToNextText = "30.00"),
                ManholeNode(name = "A3", invertText = "98.00")
            )
        )
        val missingDownstreamDistance = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "100.00", distanceToNextText = "30.00"),
                ManholeNode(name = "A2", invertText = "99.00", distanceToNextText = "-30.00"),
                ManholeNode(name = "A3", invertText = "98.00")
            )
        )

        assertNull(missingUpstreamDistance.getRecommendationFor(1))
        assertNull(missingDownstreamDistance.getRecommendationFor(1))
    }

    @Test
    fun twoNodeProfile_infersFlowFromWhicheverEndpointIsHigher() {
        val leftHigher = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "100.00", distanceToNextText = "100.00"),
                ManholeNode(name = "A2", invertText = "99.00")
            )
        )
        val rightHigher = ImpactCalculationState(
            nodes = listOf(
                ManholeNode(name = "A1", invertText = "99.00", distanceToNextText = "100.00"),
                ManholeNode(name = "A2", invertText = "100.00")
            )
        )

        assertEquals(true, leftHigher.flowLeftToRight)
        assertEquals(true, leftHigher.getSegmentFlowLeftToRight(0))
        assertEquals(ImpactSlopeStatus.VALID, leftHigher.getSegmentAnalysis(0).status)
        assertTrue(leftHigher.allSegmentsValid)

        assertEquals(false, rightHigher.flowLeftToRight)
        assertEquals(false, rightHigher.getSegmentFlowLeftToRight(0))
        assertEquals(ImpactSlopeStatus.VALID, rightHigher.getSegmentAnalysis(0).status)
        assertTrue(rightHigher.allSegmentsValid)
    }
}
