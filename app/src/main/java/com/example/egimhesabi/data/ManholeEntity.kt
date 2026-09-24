package com.example.egimhesabi.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "manholes",
    indices = [
        Index("projectId"),
        Index(value = ["projectId", "id"], unique = true),
        Index(value = ["projectId", "nameKey"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ManholeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val nameKey: String = normalizeEntityName(name),
    val x: Double,
    val y: Double,
    val projeSiyahKot: Double,
    val projeKapakKotu: Double = projeSiyahKot,
    val araziSiyahKot: Double,
    val projeAkarKot: Double,
    val imalatKapakKotu: Double?,
    val imalatAkarKotu: Double?,
    val status: String = ElementStatus.NOT_STARTED,
    val progressPaymentNumber: Int? = null
) {
    @get:Ignore
    val projeAraziFarki: Double
        get() = projeKapakKotu - araziSiyahKot

    @get:Ignore
    val workOrderTitle: String
        get() = ""

    @get:Ignore
    val workOrderNote: String
        get() = ""
}

fun ManholeEntity.projectTerrainDifference(): Double = projeKapakKotu - araziSiyahKot

/**
 * Compatibility projections for screens while letegacy embedded work-order columns are migrated to
 * the dedicated work-order tables. New records are read through [WorkOrderDao].
 */
fun ManholeEntity.workOrderPhotoPaths(): List<String> = emptyList()
