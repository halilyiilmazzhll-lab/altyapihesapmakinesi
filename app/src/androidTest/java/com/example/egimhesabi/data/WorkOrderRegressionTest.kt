package com.example.egimhesabi.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WorkOrderRegressionTest {
    @Test fun updatingNoteDoesNotChangePaymentAndFailedSavePreservesExistingOrder() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val district = db.districtDao().insertDistrict(DistrictEntity(name = "Test ilçe"))
            val project = db.projectDao().insertProject(ProjectEntity(districtId = district, name = "Test proje"))
            val manhole = db.manholeDao().insertManhole(ManholeEntity(projectId = project, name = "A1", x = 0.0, y = 0.0,
                projeSiyahKot = 100.0, araziSiyahKot = 100.0, projeAkarKot = 98.0, imalatKapakKotu = null, imalatAkarKotu = null))
            val dao = db.workOrderDao()
            val id = dao.save(project, null, manhole, "Kontrol", "İlk not", listOf("photo.img"), 2)
            dao.save(project, id, manhole, "Kontrol", "Yeni not", listOf("photo.img"), 3)
            assertEquals(2, dao.getById(id)!!.workOrder.progressPaymentNumber)
            assertEquals("Yeni not", dao.getById(id)!!.workOrder.note)
            try {
                dao.save(project + 1, id, manhole, "Kontrol", "Yanlış", emptyList())
                fail("Cross-project update must fail")
            } catch (_: IllegalArgumentException) { }
            assertEquals("Yeni not", dao.getById(id)!!.workOrder.note)
            assertEquals(listOf("photo.img"), dao.getPhotoPathsByWorkOrderId(id))
        } finally { db.close() }
    }
}
