package com.example.egimhesabi.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pipelines",
    indices = [
        Index("projectId"),
        Index(value = ["projectId", "fromManholeId"]),
        Index(value = ["projectId", "toManholeId"]),
        Index(value = ["projectId", "fromManholeId", "toManholeId"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ManholeEntity::class,
            parentColumns = ["projectId", "id"],
            childColumns = ["projectId", "fromManholeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ManholeEntity::class,
            parentColumns = ["projectId", "id"],
            childColumns = ["projectId", "toManholeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PipelineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val fromManholeId: Long,
    val toManholeId: Long,
    val projeMesafe: Double,
    val imalatMesafe: Double?,
    val status: String,
    val isReverseFlow: Boolean,
    val progressPaymentNumber: Int? = null
)

fun PipelineEntity.displayFromManholeId(): Long =
    if (isReverseFlow) toManholeId else fromManholeId

fun PipelineEntity.displayToManholeId(): Long =
    if (isReverseFlow) fromManholeId else toManholeId
