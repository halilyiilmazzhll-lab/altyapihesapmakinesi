package com.example.egimhesabi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "impact_history_entries")
data class ImpactHistoryEntry(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val nodesJson: String = "[]",
    val minSlopePercent: Double = 0.5,
    val maxSlopePercent: Double = 5.0,
    val minManholeDepthMeters: Double = 1.0,
    val segmentCount: Int = 0,
    val allValid: Boolean = false,
    val summaryText: String = ""
)

@Dao
interface ImpactHistoryDao {
    @Query("SELECT * FROM impact_history_entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ImpactHistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ImpactHistoryEntry): Long

    @Delete
    suspend fun delete(entry: ImpactHistoryEntry): Int

    @Query("DELETE FROM impact_history_entries")
    suspend fun deleteAll(): Int
}
