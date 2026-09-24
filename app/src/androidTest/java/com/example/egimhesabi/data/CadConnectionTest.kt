package com.example.egimhesabi.data

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.egimhesabi.viewmodel.StakeoutViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CadConnectionTest {
    @Test fun connectionCompletionReportsSuccessBusyAndFailureWithoutLosingExistingTarget() = runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val db=Room.inMemoryDatabaseBuilder(instrumentation.targetContext,AppDatabase::class.java).build()
        val store=androidx.lifecycle.ViewModelStore()
        try {
            val district=db.districtDao().insertDistrict(DistrictEntity(name="CAD test"))
            val dao=db.stakeoutDao()
            val neighborhood=dao.insertNeighborhood(NeighborhoodEntity(districtId=district,name="Canvas"))
            dao.importRecords(neighborhood,listOf("A","B","C").map { StakeoutManholeEntity(neighborhoodId=neighborhood,name=it) })
            val records=dao.records(neighborhood).first().associateBy { it.name }
            lateinit var vm: StakeoutViewModel
            instrumentation.runOnMainSync { vm=StakeoutViewModel(db.districtDao(),dao,SavedStateHandle());store.put("cad",vm) }
            fun update(target: String?): Boolean {
                val done=CountDownLatch(1);var success=false
                instrumentation.runOnMainSync { vm.updateConnection(records.getValue("A").id,target) { success=it;done.countDown() } }
                assertTrue(done.await(10,TimeUnit.SECONDS));return success
            }
            val firstDone=CountDownLatch(1);var firstSuccess=false;var rejected: Boolean?=null
            instrumentation.runOnMainSync {
                vm.updateConnection(records.getValue("A").id,records.getValue("B").nameKey) { firstSuccess=it;firstDone.countDown() }
                vm.updateConnection(records.getValue("A").id,records.getValue("C").nameKey) { rejected=it }
            }
            assertEquals(false,rejected)
            assertTrue(firstDone.await(10,TimeUnit.SECONDS));assertTrue(firstSuccess)
            assertFalse(update("missing"))
            assertEquals(records.getValue("B").nameKey,dao.records(neighborhood).first().first { it.name=="A" }.connectedToNameKey)
            assertTrue(update(records.getValue("C").nameKey))
            assertTrue(update(records.getValue("B").nameKey))
            assertEquals(records.getValue("B").nameKey,dao.records(neighborhood).first().first { it.name=="A" }.connectedToNameKey)
        } finally { instrumentation.runOnMainSync { store.clear() };db.close() }
    }
}
