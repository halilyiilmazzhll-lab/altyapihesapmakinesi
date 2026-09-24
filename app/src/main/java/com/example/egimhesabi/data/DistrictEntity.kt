package com.example.egimhesabi.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "districts",
    indices = [Index(value = ["nameKey"], unique = true)]
)
data class DistrictEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nameKey: String = normalizeEntityName(name),
    val createdAt: Long = System.currentTimeMillis()
)
