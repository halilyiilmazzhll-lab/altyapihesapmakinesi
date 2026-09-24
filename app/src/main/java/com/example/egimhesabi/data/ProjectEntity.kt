package com.example.egimhesabi.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [
        Index("districtId"),
        Index(value = ["districtId", "nameKey"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = DistrictEntity::class,
            parentColumns = ["id"],
            childColumns = ["districtId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val districtId: Long,
    val name: String,
    val pipeType: String = "",
    val pipeWidth: String = "",
    val nameKey: String = normalizeEntityName(name),
    val createdAt: Long = System.currentTimeMillis()
)

