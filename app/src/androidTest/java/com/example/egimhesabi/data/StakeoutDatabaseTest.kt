package com.example.egimhesabi.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.egimhesabi.util.StakeoutField
import com.example.egimhesabi.util.StakeoutWorkbook
import jxl.write.Label
import jxl.write.NumberFormat
import jxl.write.WritableCellFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StakeoutDatabaseTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun roomRecordsStayScopedAndReimportKeepsIdentity() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val district = db.districtDao().insertDistrict(DistrictEntity(name = "İlçe"))
            val dao = db.stakeoutDao()
            val north = dao.insertNeighborhood(NeighborhoodEntity(districtId = district, name = "Kuzey"))
            val south = dao.insertNeighborhood(NeighborhoodEntity(districtId = district, name = "Güney"))
            dao.importRecords(north, listOf(record(north, "A1", 100.0, 1.0)))
            dao.importRecords(south, listOf(record(south, "A1", 200.0, 2.0)))
            val northId = dao.records(north).first().single().id
            val untouchedSouth = dao.records(south).first().single()
            assertNotEquals(northId, untouchedSouth.id)

            dao.importRecords(north, listOf(record(north, " a1 ", 120.0, 2.5)))

            val updatedNorth = dao.records(north).first().single()
            assertEquals(northId, updatedNorth.id)
            assertEquals(117.5, updatedNorth.terrainInvertLevel!!, 0.0)
            assertEquals(untouchedSouth, dao.records(south).first().single())
            val qualified = dao.allRecords().first().associateBy { it.manhole.neighborhoodId }
            assertEquals("İlçe / Kuzey / a1", qualified.getValue(north).qualifiedName)
            assertEquals("İlçe / Güney / A1", qualified.getValue(south).qualifiedName)
        } finally {
            db.close()
        }
    }

    @Test
    fun roomRejectsDuplicateNamesWithoutPartiallyImportingBatch() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val district = db.districtDao().insertDistrict(DistrictEntity(name = "İlçe"))
            val dao = db.stakeoutDao()
            val neighborhood = dao.insertNeighborhood(NeighborhoodEntity(districtId = district, name = "Mahalle"))
            dao.saveRecord(record(neighborhood, "A1", 100.0, 1.0))
            val original = dao.records(neighborhood).first().single()

            expectIllegalArgument { dao.saveRecord(record(neighborhood, " a1 ", 200.0, 2.0)) }
            expectIllegalArgument {
                dao.importRecords(neighborhood, listOf(record(neighborhood, "A2"), record(neighborhood, "A3"), record(neighborhood, " a3 ")))
            }
            expectIllegalArgument { dao.saveRecord(original.copy(dischargeDepth = -1.0)) }

            assertEquals(listOf(original), dao.records(neighborhood).first())
        } finally {
            db.close()
        }
    }

    @Test
    fun editedRecordsSurviveReopenAndDerivedLevelUsesNewValues() = runBlocking {
        val name = uniqueDatabaseName()
        var db: AppDatabase? = null
        try {
            val initial = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            db = initial
            val district = initial.districtDao().insertDistrict(DistrictEntity(name = "Kalıcı ilçe"))
            val neighborhood = initial.stakeoutDao().insertNeighborhood(NeighborhoodEntity(districtId = district, name = "Kalıcı mahalle"))
            initial.stakeoutDao().saveRecord(record(neighborhood, "A1", 100.0, 1.0))
            val original = initial.stakeoutDao().records(neighborhood).first().single()
            initial.stakeoutDao().saveRecord(original.copy(terrainGroundLevel = 110.0, dischargeDepth = 2.25))
            initial.close()

            val reopened = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            db = reopened
            val persisted = reopened.stakeoutDao().records(neighborhood).first().single()
            assertEquals(original.id, persisted.id)
            assertEquals(110.0, persisted.terrainGroundLevel!!, 0.0)
            assertEquals(2.25, persisted.dischargeDepth!!, 0.0)
            assertEquals(107.75, persisted.terrainInvertLevel!!, 0.0)
        } finally {
            db?.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migration13To14ValidatesRoomSchemaAndPreservesExistingData() = runBlocking {
        val name = uniqueDatabaseName()
        var db: AppDatabase? = null
        try {
            // The pre-existing tables have the same schema in versions 13 and 14.
            // Remove only the two added tables to build an isolated version-13 fixture.
            val initial = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            db = initial
            val districtId = initial.districtDao().insertDistrict(DistrictEntity(name = "Geçişten önceki ilçe"))
            val history = HistoryEntry(id = 130014L, baca1Name = "KORUNACAK_A1", baca2Name = "KORUNACAK_A2", mesafe = 42.0)
            initial.historyDao().insert(history)
            initial.close()

            SQLiteDatabase.openDatabase(context.getDatabasePath(name).absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { legacy ->
                legacy.execSQL("DROP TABLE stakeout_manholes")
                legacy.execSQL("DROP TABLE neighborhoods")
                legacy.version = 13
                assertEquals(13, legacy.version)
            }

            val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(*AppDatabase.ALL_MIGRATIONS)
                .build()
            db = migrated
            // Opening through Room runs the migration and compares it to generated schema.
            assertEquals(listOf("Geçişten önceki ilçe"), migrated.districtDao().getAllDistricts().first().map { it.name })
            assertEquals(history, migrated.historyDao().getAllHistory().first().single())
            assertEquals(16, migrated.openHelper.writableDatabase.version)

            val dao = migrated.stakeoutDao()
            val neighborhood = dao.insertNeighborhood(NeighborhoodEntity(districtId = districtId, name = "Yeni mahalle"))
            dao.importRecords(neighborhood, listOf(record(neighborhood, "A1", 100.0, 1.5)))
            assertEquals(98.5, dao.records(neighborhood).first().single().terrainInvertLevel!!, 0.0)
            // Verify both new foreign-key levels on the migrated physical schema.
            migrated.districtDao().deleteDistrict(districtId)
            assertTrue(dao.neighborhoods(districtId).first().isEmpty())
            assertTrue(dao.records(neighborhood).first().isEmpty())
            assertEquals(history, migrated.historyDao().getAllHistory().first().single())
        } finally {
            db?.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun xlsxExportAndSaxReaderWorkOnAndroid() {
        val output = ByteArrayOutputStream()
        StakeoutWorkbook.export(
            output,
            listOf(record(1, "A01", 101.25, 1.75).copy(projectY = 414222.37, projectX = 4360474.879)),
            "İlçe / Mahalle"
        )
        val sheet = StakeoutWorkbook.read(ByteArrayInputStream(output.toByteArray()), "tutanak.xlsx").sheets.single()
        val preview = StakeoutWorkbook.preview(sheet, StakeoutWorkbook.defaultMapping(sheet))
        assertTrue(preview.issues.toString(), preview.canImport)
        assertEquals(4, preview.rows.single().excelRow)
        val parsed = preview.rows.single().manhole
        assertEquals("A01", parsed.name)
        assertEquals(414222.37, parsed.projectY!!, 0.000001)
        assertEquals(4360474.879, parsed.projectX!!, 0.000001)
        assertEquals(99.5, parsed.terrainInvertLevel!!, 0.000001)
        assertNull(parsed.projectCoverLevel)
    }

    @Test
    fun legacyXlsReaderWorksOnAndroidAndPreservesPaddedManholeName() {
        val output = ByteArrayOutputStream()
        val workbook = jxl.Workbook.createWorkbook(output)
        try {
            val sheet = workbook.createSheet("Mahalle", 0)
            StakeoutField.entries.forEachIndexed { column, field -> sheet.addCell(Label(column, 2, field.label)) }
            sheet.addCell(jxl.write.Number(0, 3, 7.0, WritableCellFormat(NumberFormat("0000"))))
            sheet.addCell(jxl.write.Number(6, 3, 101.25))
            sheet.addCell(jxl.write.Number(8, 3, 1.75))
            workbook.write()
        } finally {
            workbook.close()
        }
        val sheet = StakeoutWorkbook.read(ByteArrayInputStream(output.toByteArray()), "tutanak.xls").sheets.single()
        val preview = StakeoutWorkbook.preview(sheet, StakeoutWorkbook.defaultMapping(sheet))
        assertTrue(preview.issues.toString(), preview.canImport)
        assertEquals("0007", preview.rows.single().manhole.name)
        assertEquals(99.5, preview.rows.single().manhole.terrainInvertLevel!!, 0.000001)
    }

    private fun uniqueDatabaseName(): String = "stakeout_instrumentation_${UUID.randomUUID()}.db"

    private fun record(neighborhood: Long, name: String, ground: Double? = null, depth: Double? = null) =
        StakeoutManholeEntity(neighborhoodId = neighborhood, name = name, terrainGroundLevel = ground, dischargeDepth = depth)

    private suspend fun expectIllegalArgument(block: suspend () -> Unit) {
        var rejected = false
        try {
            block()
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue("Expected invalid input to be rejected", rejected)
    }
}
