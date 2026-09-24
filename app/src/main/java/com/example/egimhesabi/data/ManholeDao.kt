package com.example.egimhesabi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ManholeDao {
    @Query("SELECT * FROM manholes WHERE projectId = :projectId ORDER BY name ASC")
    abstract fun getManholesByProjectId(projectId: Long): Flow<List<ManholeEntity>>

    @Query("SELECT * FROM manholes WHERE id = :manholeId AND projectId = :projectId LIMIT 1")
    abstract suspend fun getManhole(manholeId: Long, projectId: Long): ManholeEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertManhole(manhole: ManholeEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertManholesIgnoringConflicts(
        manholes: List<ManholeEntity>
    ): List<Long>

    @Query(
        "UPDATE manholes SET x = :x, y = :y, projeSiyahKot = :projeSiyahKot, " +
            "projeKapakKotu = :projeKapakKotu, araziSiyahKot = :araziSiyahKot, " +
            "projeAkarKot = :projeAkarKot WHERE projectId = :projectId AND nameKey = :nameKey"
    )
    protected abstract suspend fun updateImportedManhole(
        projectId: Long,
        nameKey: String,
        x: Double,
        y: Double,
        projeSiyahKot: Double,
        projeKapakKotu: Double,
        araziSiyahKot: Double,
        projeAkarKot: Double
    ): Int

    @Transaction
    open suspend fun importManholes(projectId: Long, manholes: List<ManholeEntity>): Int {
        require(manholes.all { it.projectId == projectId }) {
            "CSV satırlarının tamamı açık projeye ait olmalıdır."
        }
        val insertResults = insertManholesIgnoringConflicts(manholes)
        manholes.zip(insertResults).forEach { (manhole, insertedId) ->
            if (insertedId == -1L) {
                updateImportedManhole(
                    projectId = projectId,
                    nameKey = manhole.nameKey,
                    x = manhole.x,
                    y = manhole.y,
                    projeSiyahKot = manhole.projeSiyahKot,
                    projeKapakKotu = manhole.projeKapakKotu,
                    araziSiyahKot = manhole.araziSiyahKot,
                    projeAkarKot = manhole.projeAkarKot
                )
            }
        }
        return manholes.size
    }

    @Query(
        "UPDATE manholes SET status = :status, progressPaymentNumber = :progressPaymentNumber " +
            "WHERE id = :manholeId AND projectId = :projectId"
    )
    abstract suspend fun updateStatus(
        projectId: Long,
        manholeId: Long,
        status: String,
        progressPaymentNumber: Int?
    ): Int

    @Query("DELETE FROM manholes WHERE id = :manholeId AND projectId = :projectId")
    abstract suspend fun deleteManhole(projectId: Long, manholeId: Long): Int
}
