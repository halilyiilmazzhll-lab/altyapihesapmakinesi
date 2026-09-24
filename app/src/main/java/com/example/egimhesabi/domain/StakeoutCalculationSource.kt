package com.example.egimhesabi.domain

import com.example.egimhesabi.data.StakeoutRecord
import kotlinx.serialization.Serializable

@Serializable
enum class StakeoutElevationBasis(val label: String) {
    PROJECT("Proje kotları"),
    TERRAIN("Arazi kotları")
}

/** A snapshot, identified by the database id rather than the visible manhole name. */
@Serializable
data class StakeoutCalculationSource(
    val recordId: Long,
    val qualifiedName: String,
    val basis: StakeoutElevationBasis,
    val upperLevel: Double?,
    val invertLevel: Double?
) {
    val manholeName: String get() = qualifiedName.substringAfterLast(" / ")
    val label: String get() = "$qualifiedName · ${basis.label}"
    val upperText: String get() = upperLevel?.toString().orEmpty()
    val invertText: String get() = invertLevel?.toString().orEmpty()

    companion object {
        fun from(record: StakeoutRecord, basis: StakeoutElevationBasis): StakeoutCalculationSource {
            val manhole = record.manhole
            return StakeoutCalculationSource(
                recordId = manhole.id,
                qualifiedName = record.qualifiedName,
                basis = basis,
                upperLevel = when (basis) {
                    StakeoutElevationBasis.PROJECT -> manhole.projectCoverLevel
                    StakeoutElevationBasis.TERRAIN -> manhole.terrainGroundLevel
                }?.takeIf { it.isFinite() },
                invertLevel = when (basis) {
                    StakeoutElevationBasis.PROJECT -> manhole.projectInvertLevel
                    StakeoutElevationBasis.TERRAIN -> manhole.terrainInvertLevel
                }?.takeIf { it.isFinite() }
            )
        }
    }
}
