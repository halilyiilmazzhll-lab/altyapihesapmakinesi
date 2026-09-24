package com.example.egimhesabi.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stakeout_manholes",
    indices = [
        Index("neighborhoodId"),
        Index(value = ["neighborhoodId", "nameKey"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = NeighborhoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["neighborhoodId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StakeoutManholeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val neighborhoodId: Long,
    val name: String,
    val nameKey: String = normalizeEntityName(name),
    val projectY: Double? = null,
    val projectX: Double? = null,
    val projectCoverLevel: Double? = null,
    val projectGroundLevel: Double? = null,
    val projectInvertLevel: Double? = null,
    val terrainGroundLevel: Double? = null,
    val dischargeCoverLevel: Double? = null,
    val dischargeDepth: Double? = null,
    val connectedToNameKey: String? = null,
    val sourceFileName: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    @get:Ignore
    val terrainInvertLevel: Double?
        get() = terrainGroundLevel?.let { ground -> dischargeDepth?.let { ground - it } }
}

data class StakeoutRecord(
    @Embedded val manhole: StakeoutManholeEntity,
    val districtName: String,
    val neighborhoodName: String
) {
    @get:Ignore
    val qualifiedName: String
        get() = "$districtName / $neighborhoodName / ${manhole.name}"
}
