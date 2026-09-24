package com.example.egimhesabi.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "work_orders",
    indices = [Index(value = ["manholeId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = ManholeEntity::class,
            parentColumns = ["id"],
            childColumns = ["manholeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WorkOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val manholeId: Long,
    val title: String,
    val note: String,
    val progressPaymentNumber: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "work_order_photos",
    indices = [
        Index("workOrderId"),
        Index(value = ["workOrderId", "displayOrder"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = WorkOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["workOrderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WorkOrderPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val path: String,
    val displayOrder: Int,
    val createdAt: Long = System.currentTimeMillis()
)

data class WorkOrderWithPhotos(
    @Embedded val workOrder: WorkOrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workOrderId"
    )
    val photos: List<WorkOrderPhotoEntity>
) {
    fun orderedPhotos(): List<WorkOrderPhotoEntity> =
        photos.sortedWith(compareBy(WorkOrderPhotoEntity::displayOrder, WorkOrderPhotoEntity::id))
}
