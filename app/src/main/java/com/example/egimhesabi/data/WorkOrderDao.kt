package com.example.egimhesabi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkOrderDao {
    @Transaction
    @Query(
        "SELECT work_orders.* FROM work_orders " +
            "INNER JOIN manholes ON manholes.id = work_orders.manholeId " +
            "WHERE manholes.projectId = :projectId " +
            "ORDER BY work_orders.createdAt DESC, work_orders.id DESC"
    )
    abstract fun observeByProjectId(projectId: Long): Flow<List<WorkOrderWithPhotos>>

    @Transaction
    @Query(
        "SELECT work_orders.* FROM work_orders " +
            "INNER JOIN manholes ON manholes.id = work_orders.manholeId " +
            "WHERE manholes.projectId = :projectId " +
            "AND work_orders.progressPaymentNumber = :paymentNumber " +
            "ORDER BY work_orders.createdAt DESC, work_orders.id DESC"
    )
    abstract fun observeByProgressPayment(projectId: Long, paymentNumber: Int): Flow<List<WorkOrderWithPhotos>>

    @Transaction
    @Query(
        "SELECT work_orders.* FROM work_orders " +
            "INNER JOIN manholes ON manholes.id = work_orders.manholeId " +
            "WHERE manholes.projectId = :projectId " +
            "AND work_orders.progressPaymentNumber IS NULL " +
            "ORDER BY work_orders.createdAt DESC, work_orders.id DESC"
    )
    abstract fun observeUnassigned(projectId: Long): Flow<List<WorkOrderWithPhotos>>

    @Query(
        "SELECT DISTINCT work_orders.progressPaymentNumber FROM work_orders " +
            "INNER JOIN manholes ON manholes.id = work_orders.manholeId " +
            "WHERE manholes.projectId = :projectId AND work_orders.progressPaymentNumber IS NOT NULL " +
            "ORDER BY work_orders.progressPaymentNumber ASC"
    )
    abstract fun observeDistinctProgressPayments(projectId: Long): Flow<List<Int>>

    @Transaction
    @Query("SELECT * FROM work_orders WHERE id = :workOrderId LIMIT 1")
    abstract suspend fun getById(workOrderId: Long): WorkOrderWithPhotos?

    @Query("SELECT projectId FROM manholes WHERE id = :manholeId LIMIT 1")
    protected abstract suspend fun getManholeProjectId(manholeId: Long): Long?

    @Query(
        "SELECT manholes.projectId FROM work_orders " +
            "INNER JOIN manholes ON manholes.id = work_orders.manholeId " +
            "WHERE work_orders.id = :workOrderId LIMIT 1"
    )
    protected abstract suspend fun getWorkOrderProjectId(workOrderId: Long): Long?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertWorkOrder(workOrder: WorkOrderEntity): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun updateWorkOrder(workOrder: WorkOrderEntity): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertPhotos(photos: List<WorkOrderPhotoEntity>)

    @Query("DELETE FROM work_order_photos WHERE workOrderId = :workOrderId")
    protected abstract suspend fun deletePhotos(workOrderId: Long): Int

    @Query("DELETE FROM work_orders WHERE id = :workOrderId")
    protected abstract suspend fun deleteWorkOrder(workOrderId: Long): Int

    @Query("SELECT path FROM work_order_photos WHERE workOrderId = :workOrderId")
    abstract suspend fun getPhotoPathsByWorkOrderId(workOrderId: Long): List<String>

    @Query(
        "SELECT work_order_photos.path FROM work_order_photos " +
            "INNER JOIN work_orders ON work_orders.id = work_order_photos.workOrderId " +
            "WHERE work_orders.manholeId = :manholeId"
    )
    abstract suspend fun getPhotoPathsByManholeId(manholeId: Long): List<String>

    @Query(
        "SELECT work_order_photos.path FROM work_order_photos " +
            "INNER JOIN work_orders ON work_orders.id = work_order_photos.workOrderId " +
            "INNER JOIN manholes ON manholes.id = work_orders.manholeId " +
            "WHERE manholes.projectId = :projectId"
    )
    abstract suspend fun getPhotoPathsByProjectId(projectId: Long): List<String>

    @Query(
        "SELECT work_order_photos.path FROM work_order_photos " +
            "INNER JOIN work_orders ON work_orders.id = work_order_photos.workOrderId " +
            "INNER JOIN manholes ON manholes.id = work_orders.manholeId " +
            "INNER JOIN projects ON projects.id = manholes.projectId " +
            "WHERE projects.districtId = :districtId"
    )
    abstract suspend fun getPhotoPathsByDistrictId(districtId: Long): List<String>

    @Query("SELECT path FROM work_order_photos")
    abstract suspend fun getAllPhotoPaths(): List<String>

    @Query("SELECT COUNT(*) FROM work_order_photos WHERE path = :path")
    abstract suspend fun countReferences(path: String): Int

    @Transaction
    open suspend fun save(
        projectId: Long,
        workOrderId: Long?,
        manholeId: Long,
        title: String,
        note: String,
        photoPaths: List<String>,
        progressPaymentNumber: Int? = null,
        now: Long = System.currentTimeMillis()
    ): Long {
        require(getManholeProjectId(manholeId) == projectId) {
            "Emir kaydı açık projeye ait bir bacaya bağlanmalıdır."
        }
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Emir defteri başlığı boş olamaz." }

        val id = if (workOrderId == null) {
            insertWorkOrder(
                WorkOrderEntity(
                    manholeId = manholeId,
                    title = normalizedTitle,
                    note = note.trim(),
                    progressPaymentNumber = progressPaymentNumber,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            require(getWorkOrderProjectId(workOrderId) == projectId) {
                "Emir kaydı açık projeye ait değildir."
            }
            val current = getById(workOrderId)?.workOrder
                ?: error("Emir kaydı bulunamadı.")
            require(current.manholeId == manholeId) { "Emir kaydının bacası değiştirilemez." }
            updateWorkOrder(
                current.copy(
                    title = normalizedTitle,
                    note = note.trim(),
                    // Editing content never moves a previously assigned work order.
                    progressPaymentNumber = current.progressPaymentNumber,
                    updatedAt = now
                )
            )
            workOrderId
        }

        deletePhotos(id)
        insertPhotos(
            photoPaths.distinct().mapIndexed { index, path ->
                WorkOrderPhotoEntity(
                    workOrderId = id,
                    path = path,
                    displayOrder = index,
                    createdAt = now
                )
            }
        )
        return id
    }

    @Transaction
    open suspend fun delete(projectId: Long, workOrderId: Long): Boolean {
        if (getWorkOrderProjectId(workOrderId) != projectId) return false
        return deleteWorkOrder(workOrderId) > 0
    }
}
