package com.example.egimhesabi

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data object Main : NavKey
@Serializable data object History : NavKey
@Serializable data object Interpolation : NavKey
@Serializable data object Reverse : NavKey
@Serializable data object Impact : NavKey
@Serializable data object Leveling : NavKey
@Serializable data object ElevationTransfer : NavKey
@Serializable data object Settings : NavKey
@Serializable data object Tracking : NavKey
@Serializable data object Stakeout : NavKey
@Serializable data class ProjectMap(val projectId: Long, val projectName: String) : NavKey
@Serializable data class ProjectPipelines(val projectId: Long, val projectName: String) : NavKey
@Serializable data class ProjectOrderBook(val projectId: Long, val projectName: String) : NavKey

