package com.example.egimhesabi.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StakeoutDaoTest {
    @Test
    fun sameManholeNameInDifferentNeighborhoodsRemainsIndependent() = runTest {
        val dao = MemoryStakeoutDao()
        dao.importRecords(1, listOf(record(1, "A1", 100.0)))
        dao.importRecords(2, listOf(record(2, "A1", 200.0)))

        assertEquals(2, dao.saved.size)
        assertEquals(100.0, dao.saved.single { it.neighborhoodId == 1L }.terrainGroundLevel!!, 0.0)
        assertEquals(200.0, dao.saved.single { it.neighborhoodId == 2L }.terrainGroundLevel!!, 0.0)
        assertTrue(dao.saved.map { it.id }.distinct().size == 2)
    }

    @Test
    fun reimportByNormalizedNamePreservesIdAndUpdatesDerivedValue() = runTest {
        val dao = MemoryStakeoutDao()
        dao.importRecords(1, listOf(record(1, "A1", 100.0)))
        val existingId = dao.saved.single().id
        dao.importRecords(1, listOf(record(1, " a1 ", 105.0).copy(id = 900, dischargeDepth = 2.5)))

        assertEquals(existingId, dao.saved.single().id)
        assertEquals("a1", dao.saved.single().nameKey)
        assertEquals(102.5, dao.saved.single().terrainInvertLevel!!, 0.0)
    }

    @Test
    fun duplicateBatchIsRejectedBeforeAnyWrites() = runTest {
        val dao = MemoryStakeoutDao()
        expectIllegalArgument {
            dao.importRecords(1, listOf(record(1, "A1"), record(1, " a1 ")))
        }
        assertTrue(dao.saved.isEmpty())
    }

    @Test
    fun nonfiniteValuesOrMixedNeighborhoodsRejectEntireBatch() = runTest {
        val dao = MemoryStakeoutDao()
        expectIllegalArgument {
            dao.importRecords(1, listOf(record(1, "A1"), record(1, "A2").copy(projectX = Double.NaN)))
        }
        expectIllegalArgument {
            dao.importRecords(1, listOf(record(1, "A1"), record(2, "A2")))
        }
        expectIllegalArgument {
            dao.importRecords(1, listOf(record(1, "A1").copy(terrainGroundLevel = Double.MAX_VALUE, dischargeDepth = -Double.MAX_VALUE)))
        }
        assertTrue(dao.saved.isEmpty())
    }

    @Test
    fun editCannotMoveRecordToAnotherNeighborhoodOrOverwriteDuplicate() = runTest {
        val dao = MemoryStakeoutDao()
        dao.importRecords(1, listOf(record(1, "A1"), record(1, "A2")))
        val first = dao.saved.first()
        expectIllegalArgument { dao.saveRecord(first.copy(neighborhoodId = 2)) }
        expectIllegalArgument { dao.saveRecord(first.copy(name = "A2")) }

        assertEquals(listOf("A1", "A2"), dao.saved.map { it.name })
        assertTrue(dao.saved.all { it.neighborhoodId == 1L })
    }

    @Test
    fun scopedDeletionCannotRemoveOtherNeighborhoodRecord() = runTest {
        val dao = MemoryStakeoutDao()
        dao.saveRecord(record(1, "A1"))
        val id = dao.saved.single().id
        dao.deleteRecord(2, id)
        assertEquals(1, dao.saved.size)
        dao.deleteRecord(1, id)
        assertTrue(dao.saved.isEmpty())
    }

    @Test
    fun negativeDepthIsRejectedForBothSaveAndImportButZeroIsValid() = runTest {
        val dao = MemoryStakeoutDao()
        val negative = record(1, "A1", 100.0).copy(dischargeDepth = -1.0)
        expectIllegalArgument { dao.saveRecord(negative) }
        expectIllegalArgument { dao.importRecords(1, listOf(record(1, "A2"), negative)) }
        assertTrue(dao.saved.isEmpty())

        dao.saveRecord(negative.copy(dischargeDepth = 0.0))
        assertEquals(100.0, dao.saved.single().terrainInvertLevel!!, 0.0)
    }

    @Test
    fun derivedLevelRequiresBothInputsAndQualifiedLabelIncludesScope() {
        assertNull(record(1, "A1").terrainInvertLevel)
        assertNull(record(1, "A1", 100.0).terrainInvertLevel)
        val manhole = record(1, "A1", 100.0).copy(dischargeDepth = 0.0)
        assertEquals(100.0, manhole.terrainInvertLevel!!, 0.0)
        assertEquals("Kadıköy / Caferağa / A1", StakeoutRecord(manhole, "Kadıköy", "Caferağa").qualifiedName)
    }

    @Test
    fun connectionsSurviveReimportAndRenameAndClearOnDeletion() = runTest {
        val dao = MemoryStakeoutDao()
        dao.importRecords(1, listOf(record(1, "A1"), record(1, "A2")))
        val source = dao.saved.first()
        val target = dao.saved.last()
        dao.updateConnection(source.id, target.nameKey, 1)
        dao.importRecords(1, listOf(record(1, "A1", 200.0)))
        assertEquals(target.nameKey, dao.saved.first().connectedToNameKey)
        dao.saveRecord(target.copy(name = "A3"))
        assertEquals("a3", dao.saved.first().connectedToNameKey)
        dao.deleteRecord(1, target.id)
        assertNull(dao.saved.single().connectedToNameKey)
    }

    private fun record(neighborhoodId: Long, name: String, ground: Double? = null) =
        StakeoutManholeEntity(neighborhoodId = neighborhoodId, name = name, terrainGroundLevel = ground)

    private suspend fun expectIllegalArgument(block: suspend () -> Unit) {
        var rejected = false
        try {
            block()
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue("Expected input to be rejected", rejected)
    }

    /** Exercises public DAO validation and scope rules without an Android runtime. */
    private class MemoryStakeoutDao : StakeoutDao() {
        val saved = mutableListOf<StakeoutManholeEntity>()
        private var nextId = 1L

        override fun neighborhoods(districtId: Long): Flow<List<NeighborhoodEntity>> = flowOf(emptyList())
        override suspend fun insertNeighborhoodIgnoringConflict(neighborhood: NeighborhoodEntity): Long = 1L
        override fun records(neighborhoodId: Long): Flow<List<StakeoutManholeEntity>> =
            flowOf(saved.filter { it.neighborhoodId == neighborhoodId })
        override fun allRecords(): Flow<List<StakeoutRecord>> = flowOf(emptyList())
        override suspend fun findRecord(neighborhoodId: Long, id: Long): StakeoutManholeEntity? =
            saved.find { it.neighborhoodId == neighborhoodId && it.id == id }
        override suspend fun findRecordByName(neighborhoodId: Long, nameKey: String): StakeoutManholeEntity? =
            saved.find { it.neighborhoodId == neighborhoodId && it.nameKey == nameKey }
        override suspend fun insertRecord(record: StakeoutManholeEntity): Long {
            val id = nextId++
            saved.add(record.copy(id = id))
            return id
        }
        override suspend fun updateRecord(record: StakeoutManholeEntity): Int {
            val index = saved.indexOfFirst { it.id == record.id }
            if (index < 0) return 0
            saved[index] = record
            return 1
        }
        override suspend fun clearRecords(neighborhoodId: Long) {
            saved.removeAll { it.neighborhoodId == neighborhoodId }
        }
        override suspend fun findRecordById(id: Long) = saved.find { it.id == id }
        override suspend fun retargetConnections(neighborhoodId: Long, oldKey: String, target: String?) {
            saved.indices.forEach { i ->
                if (saved[i].neighborhoodId == neighborhoodId && saved[i].connectedToNameKey == oldKey)
                    saved[i] = saved[i].copy(connectedToNameKey = target)
            }
        }
        override suspend fun updateConnectionRow(id: Long, targetNameKey: String?, time: Long) {
            val i = saved.indexOfFirst { it.id == id }
            if (i >= 0) saved[i] = saved[i].copy(connectedToNameKey = targetNameKey, updatedAt = time)
        }
        override suspend fun deleteRecordRow(neighborhoodId: Long, id: Long) {
            saved.removeAll { it.neighborhoodId == neighborhoodId && it.id == id }
        }
    }
}
