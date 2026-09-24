package com.example.egimhesabi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DistrictDao {
    @Query("SELECT * FROM districts ORDER BY name ASC")
    fun getAllDistricts(): Flow<List<DistrictEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDistrict(district: DistrictEntity): Long

    @Query("DELETE FROM districts WHERE id = :districtId")
    suspend fun deleteDistrict(districtId: Long)
}
