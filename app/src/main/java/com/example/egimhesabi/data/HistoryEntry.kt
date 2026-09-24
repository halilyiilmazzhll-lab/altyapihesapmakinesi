package com.example.egimhesabi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(tableName = "history_entries")
@Serializable
data class HistoryEntry(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    val baca1Name: String = "",
    val baca2Name: String = "",
    val baca1KapakKotu: Double? = null,
    val baca1AkarKotu: Double = 0.0,
    val baca2KapakKotu: Double? = null,
    val baca2AkarKotu: Double = 0.0,
    val mesafe: Double = 0.0,
    val slopePercent: Double = 0.0,
    val slopeRatio: Double = 0.0,
    val heightDiff: Double = 0.0,
    val slopeStatus: String = "UNKNOWN",
    val slopeDirection: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry): Long

    @Delete
    suspend fun delete(entry: HistoryEntry): Int

    @Query("DELETE FROM history_entries")
    suspend fun deleteAll(): Int
}
