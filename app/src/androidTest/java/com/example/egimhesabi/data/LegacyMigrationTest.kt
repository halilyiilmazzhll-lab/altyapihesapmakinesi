package com.example.egimhesabi.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class LegacyMigrationTest {
    @Test fun realHistoricalSchemasUpgradeWithoutLosingRecords() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        for (version in listOf(9, 10, 11)) {
            val name = "migration-${UUID.randomUUID()}"
            val path = context.getDatabasePath(name)
            path.parentFile!!.mkdirs()
            try {
                val schema = JSONObject(instrumentation.context.assets.open("legacy-$version.json").bufferedReader().use { it.readText() })
                    .getJSONObject("database")
                SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
                    val entities = schema.getJSONArray("entities")
                    for (i in 0 until entities.length()) {
                        val entity = entities.getJSONObject(i)
                        val table = entity.getString("tableName")
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                        val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                        for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                    db.execSQL("INSERT INTO history_entries VALUES (1, 'A1', 'A2', 0, -1, 0, -2, 100, 1, 100, 1, 'OK', -1, 1)")
                    db.execSQL("INSERT INTO districts (id,name,nameKey,createdAt) VALUES (1,'Eski ilçe','eski ilçe',1)")
                    db.version = version
                }
                val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
                    .addMigrations(*AppDatabase.ALL_MIGRATIONS).build()
                try {
                    assertEquals("Eski ilçe", migrated.districtDao().getAllDistricts().first().single().name)
                    val history = migrated.historyDao().getAllHistory().first().single()
                    assertEquals(0.0, history.baca1KapakKotu!!, 0.0)
                    assertEquals(16, migrated.openHelper.writableDatabase.version)
                } finally { migrated.close() }
            } finally { context.deleteDatabase(name) }
        }
    }
}
