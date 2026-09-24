package com.example.egimhesabi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.egimhesabi.data.DistrictDao
import com.example.egimhesabi.data.DistrictEntity
import com.example.egimhesabi.data.ManholeDao
import com.example.egimhesabi.data.ProjectDao
import com.example.egimhesabi.data.ProjectEntity
import com.example.egimhesabi.data.SettingsDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackingViewModel(
    private val districtDao: DistrictDao,
    private val projectDao: ProjectDao,
    private val manholeDao: ManholeDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    fun clearMessage() { _message.value = null }
    private fun mutate(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { _message.value = error.message ?: "İşlem tamamlanamadı." }
        }
    }

    val districts: StateFlow<List<DistrictEntity>> = districtDao.getAllDistricts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val projectCount: StateFlow<Int> = projectDao.getProjectCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _selectedDistrictId = MutableStateFlow<Long?>(null)
    val selectedDistrictId: StateFlow<Long?> = _selectedDistrictId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val projectsForSelectedDistrict: StateFlow<List<ProjectEntity>> = _selectedDistrictId
        .flatMapLatest { districtId ->
            if (districtId != null) {
                projectDao.getProjectsByDistrictId(districtId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addDistrict(name: String, onSaved: () -> Unit = {}) {
        mutate {
            require(name.isNotBlank()) { "İlçe adı boş olamaz." }
            require(districtDao.insertDistrict(DistrictEntity(name = name.trim())) != -1L) { "Bu ilçe zaten var." }
            onSaved()
        }
    }

    val projectSettings = settingsDataStore.projectSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.example.egimhesabi.data.ProjectSettings()
    )

    fun addProject(districtId: Long, name: String, pipeType: String, pipeWidth: String, onSaved: () -> Unit = {}) {
        mutate {
            require(name.isNotBlank()) { "Proje adı boş olamaz." }
            require(projectDao.insertProject(ProjectEntity(districtId = districtId, name = name.trim(), pipeType = pipeType, pipeWidth = pipeWidth)) != -1L) { "Bu proje zaten var." }
            onSaved()
        }
    }

    fun deleteDistrict(districtId: Long) {
        mutate {
            if (_selectedDistrictId.value == districtId) {
                _selectedDistrictId.value = null
            }
            districtDao.deleteDistrict(districtId)
        }
    }

    fun deleteProject(projectId: Long, districtId: Long) {
        mutate {
            projectDao.deleteProject(projectId, districtId)
        }
    }

    fun toggleDistrict(districtId: Long) {
        _selectedDistrictId.value = if (_selectedDistrictId.value == districtId) null else districtId
    }
}
