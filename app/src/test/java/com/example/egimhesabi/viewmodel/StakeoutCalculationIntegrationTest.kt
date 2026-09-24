package com.example.egimhesabi.viewmodel

import com.example.egimhesabi.data.StakeoutManholeEntity
import com.example.egimhesabi.data.StakeoutRecord
import com.example.egimhesabi.domain.StakeoutCalculationSource
import com.example.egimhesabi.domain.StakeoutElevationBasis
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StakeoutCalculationIntegrationTest {
    private fun record(id: Long = 1, neighborhood: String = "Merkez") = StakeoutRecord(
        manhole = StakeoutManholeEntity(
            id = id,
            neighborhoodId = id,
            name = "A1",
            projectCoverLevel = 105.0,
            projectGroundLevel = 107.0,
            projectInvertLevel = 101.25,
            terrainGroundLevel = 108.5,
            dischargeCoverLevel = 999.0,
            dischargeDepth = 3.75
        ),
        districtName = "Çankaya",
        neighborhoodName = neighborhood
    )

    private fun source(id: Long = 1, neighborhood: String = "Merkez", terrain: Boolean = false) =
        StakeoutCalculationSource.from(
            record(id, neighborhood),
            if (terrain) StakeoutElevationBasis.TERRAIN else StakeoutElevationBasis.PROJECT
        )

    @Test
    fun chosenBasisTransfersExactFieldsAndDerivedTerrainInvert() {
        val project = source()
        val terrain = source(terrain = true)
        assertEquals(105.0, project.upperLevel!!, 0.000001)
        assertEquals(101.25, project.invertLevel!!, 0.000001)
        assertEquals(108.5, terrain.upperLevel!!, 0.000001)
        assertEquals(104.75, terrain.invertLevel!!, 0.000001)
    }

    @Test
    fun missingDepthDoesNotBecomeZeroOrUseDischargeCoverAsFallback() {
        val incomplete = record().let { it.copy(manhole = it.manhole.copy(dischargeDepth = null)) }
        val terrain = StakeoutCalculationSource.from(incomplete, StakeoutElevationBasis.TERRAIN)
        assertNull(terrain.invertLevel)
        assertEquals("", terrain.invertText)
        assertEquals(108.5, terrain.upperLevel!!, 0.000001)
    }

    @Test
    fun incompleteUpperLevelRemainsBlankAndZeroIsAValidElevation() {
        val partial = record().let {
            it.copy(manhole = it.manhole.copy(projectCoverLevel = null, projectInvertLevel = 0.0))
        }
        val source = StakeoutCalculationSource.from(partial, StakeoutElevationBasis.PROJECT)
        assertEquals("", source.upperText)
        assertEquals("0.0", source.invertText)
        assertNull(source.upperLevel)
    }

    @Test
    fun sameManholeNamesInDifferentNeighborhoodsRetainIndependentIdentity() {
        val first = source(1, "Merkez")
        val second = source(2, "Bahçelievler", terrain = true)
        val viewModel = InterpolationViewModel()
        viewModel.updateTotaleDistance("42,5")
        viewModel.importStart(first)
        viewModel.importEnd(second)
        val state = viewModel.uiState.value
        assertEquals(1L, state.startSource!!.recordId)
        assertEquals(2L, state.endSource!!.recordId)
        assertEquals("Çankaya / Merkez / A1", state.startSource.qualifiedName)
        assertEquals("Çankaya / Bahçelievler / A1", state.endSource.qualifiedName)
        assertEquals("42,5", state.totalDistance)
        assertEquals(101.25, state.parsedKot1!!, 0.000001)
        assertEquals(104.75, state.parsedKot2!!, 0.000001)
        assertTrue(state.isValid)

        viewModel.updateKot1("99")
        assertNull(viewModel.uiState.value.startSource)
        assertEquals(second, viewModel.uiState.value.endSource)
        viewModel.clearAll()
        assertNull(viewModel.uiState.value.endSource)
    }

    @Test
    fun reverseCalculationImportsInvertAndKeepsUserDistance() {
        val viewModel = ReverseCalculationViewModel()
        viewModel.updateDistance("30")
        viewModel.updateSlopeInput("1")
        viewModel.importStart(source())
        assertEquals(100.95, viewModel.uiState.value.resultKot!!, 0.000001)
        assertEquals("30", viewModel.uiState.value.distance)
        assertEquals(1L, viewModel.uiState.value.startSource!!.recordId)
        viewModel.importStart(source().copy(invertLevel = null))
        assertEquals("101.25", viewModel.uiState.value.startKot)
    }

    @Test
    fun levelingImportInvalidatesPreviousReadingAndKeepsSelectedNeighborhood() {
        val viewModel = LevelingViewModel()
        viewModel.updateRsElevation("110")
        viewModel.updateRsBacksight("1")
        viewModel.updateDistance("3")
        viewModel.importManhole1(source())
        viewModel.importManhole2(source(2, "Bahçelievler", terrain = true))
        viewModel.updateActualReading("1.5")
        viewModel.checkActualReading()
        assertTrue(viewModel.uiState.value.isReadingChecked)

        viewModel.importManhole1(source(3, "Aşağı Ayrancı"))
        val state = viewModel.uiState.value
        assertFalse(state.isReadingChecked)
        assertEquals("", state.actualReadingInput)
        assertNull(state.correction)
        assertEquals("3", state.distanceInput)
        assertEquals("Çankaya / Aşağı Ayrancı / A1", state.manhole1Source!!.qualifiedName)
        assertEquals(2L, state.manhole2Source!!.recordId)
    }

    @Test
    fun elevationTransferImportsSurfaceAsRsAndNeverInvert() {
        val viewModel = ElevationTransferViewModel()
        viewModel.updateRsReading("1")
        viewModel.updateTargetReading("2")
        viewModel.importRs(source(terrain = true))
        assertEquals(108.5, viewModel.uiState.value.rsElevation!!, 0.000001)
        assertEquals(107.5, viewModel.uiState.value.targetElevation!!, 0.000001)
        assertEquals("Çankaya / Merkez / A1", viewModel.uiState.value.rsSource!!.qualifiedName)
        viewModel.importRs(source().copy(upperLevel = null))
        assertEquals(108.5, viewModel.uiState.value.rsElevation!!, 0.000001)
        viewModel.toggleRsSign()
        assertNull(viewModel.uiState.value.rsSource)
    }

    @Test
    fun impactHistoryRoundTripKeepsSourceIdAndOldHistoryStillLoads() {
        val source = source(2, "Bahçelievler", terrain = true)
        val node = ManholeNode(
            name = source.qualifiedName,
            coverText = source.upperText,
            invertText = source.invertText,
            stakeoutSource = source
        )
        val serialized = Json.encodeToString(node.toData())
        val restored = ManholeNode.fromData(Json.decodeFromString<ManholeNodeData>(serialized))
        assertEquals(source, restored.stakeoutSource)
        assertEquals("Çankaya / Bahçelievler / A1", restored.name)
        assertEquals(3.75, restored.depth!!, 0.000001)

        val oldHistory = Json.decodeFromString<ManholeNodeData>("""{"name":"A1","invertText":"99"}""")
        assertNull(ManholeNode.fromData(oldHistory).stakeoutSource)
    }
}
