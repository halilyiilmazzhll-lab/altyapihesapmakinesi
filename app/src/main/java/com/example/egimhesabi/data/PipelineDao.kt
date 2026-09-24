package com.example.egimhesabi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PipelineDao {
    @Query("SELECT * FROM pipelines WHERE projectId = :projectId ORDER BY id ASC")
    fun getPipelinesByProjectId(projectId: Long): Flow<List<PipelineEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPipeline(pipeline: PipelineEntity): Long

    @Query(
        "UPDATE pipelines SET status = :status, progressPaymentNumber = :progressPaymentNumber " +
            "WHERE id = :pipelineId AND projectId = :projectId"
    )
    suspend fun updateStatus(
        projectId: Long,
        pipelineId: Long,
        status: String,
        progressPaymentNumber: Int?
    ): Int

    @Query("DELETE FROM pipelines WHERE id = :pipelineId AND projectId = :projectId")
    suspend fun deletePipeline(projectId: Long, pipelineId: Long): Int
}
