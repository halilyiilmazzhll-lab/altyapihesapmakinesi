package com.example.egimhesabi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HistoryEntry::class,
        ImpactHistoryEntry::class,
        DistrictEntity::class,
        ProjectEntity::class,
        ManholeEntity::class,
        PipelineEntity::class,
        WorkOrderEntity::class,
        WorkOrderPhotoEntity::class,
        NeighborhoodEntity::class,
        StakeoutManholeEntity::class
    ],
    version = 16,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao
    abstract fun impactHistoryDao(): ImpactHistoryDao
    abstract fun districtDao(): DistrictDao
    abstract fun projectDao(): ProjectDao
    abstract fun manholeDao(): ManholeDao
    abstract fun pipelineDao(): PipelineDao
    abstract fun workOrderDao(): WorkOrderDao
    abstract fun stakeoutDao(): StakeoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        internal val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "projects", "pipeType", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "projects", "pipeWidth", "TEXT NOT NULL DEFAULT ''")
            }
        }

        internal val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS history_entries_new (
                        id INTEGER NOT NULL,
                        baca1Name TEXT NOT NULL,
                        baca2Name TEXT NOT NULL,
                        baca1KapakKotu REAL,
                        baca1AkarKotu REAL NOT NULL,
                        baca2KapakKotu REAL,
                        baca2AkarKotu REAL NOT NULL,
                        mesafe REAL NOT NULL,
                        slopePercent REAL NOT NULL,
                        slopeRatio REAL NOT NULL,
                        heightDiff REAL NOT NULL,
                        slopeStatus TEXT NOT NULL,
                        slopeDirection INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO history_entries_new (
                        id, baca1Name, baca2Name, baca1KapakKotu, baca1AkarKotu,
                        baca2KapakKotu, baca2AkarKotu, mesafe, slopePercent,
                        slopeRatio, heightDiff, slopeStatus, slopeDirection, timestamp
                    )
                    SELECT
                        id, baca1Name, baca2Name, baca1KapakKotu, baca1AkarKotu,
                        baca2KapakKotu, baca2AkarKotu, mesafe, slopePercent,
                        slopeRatio, heightDiff, slopeStatus, slopeDirection, timestamp
                    FROM history_entries
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE history_entries")
                db.execSQL("ALTER TABLE history_entries_new RENAME TO history_entries")
            }
        }

        internal val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS impact_history_entries (
                        id INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        nodesJson TEXT NOT NULL,
                        minSlopePercent REAL NOT NULL,
                        maxSlopePercent REAL NOT NULL,
                        minManholeDepthMeters REAL NOT NULL,
                        segmentCount INTEGER NOT NULL,
                        allValid INTEGER NOT NULL,
                        summaryText TEXT NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
            }
        }

        internal val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS neighborhoods (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        districtId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        nameKey TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(districtId) REFERENCES districts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_neighborhoods_districtId ON neighborhoods(districtId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_neighborhoods_districtId_nameKey ON neighborhoods(districtId, nameKey)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stakeout_manholes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        neighborhoodId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        nameKey TEXT NOT NULL,
                        projectY REAL,
                        projectX REAL,
                        projectCoverLevel REAL,
                        projectGroundLevel REAL,
                        projectInvertLevel REAL,
                        terrainGroundLevel REAL,
                        dischargeCoverLevel REAL,
                        dischargeDepth REAL,
                        sourceFileName TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(neighborhoodId) REFERENCES neighborhoods(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stakeout_manholes_neighborhoodId ON stakeout_manholes(neighborhoodId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_stakeout_manholes_neighborhoodId_nameKey ON stakeout_manholes(neighborhoodId, nameKey)")
            }
        }


        internal val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stakeout_manholes ADD COLUMN connectedToNameKey TEXT DEFAULT NULL")
            }
        }
        private fun addColumnIfMissing(db: SupportSQLiteDatabase, table: String, column: String, definition: String) {
            val exists = db.query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                var found = false
                while (cursor.moveToNext()) if (cursor.getString(nameIndex) == column) found = true
                found
            }
            if (!exists) db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
        }

        internal val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "projects", "pipeType", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "projects", "pipeWidth", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "work_orders", "progressPaymentNumber", "INTEGER")
            }
        }

        internal val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Repair earlier installations whose migration omitted this nullable field.
                addColumnIfMissing(db, "work_orders", "progressPaymentNumber", "INTEGER")
            }
        }

        internal val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
            MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16
        )

    }
}
