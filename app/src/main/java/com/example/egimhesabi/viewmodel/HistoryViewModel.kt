package com.example.egimhesabi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.egimhesabi.data.HistoryDao
import com.example.egimhesabi.data.HistoryEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val historyDao: HistoryDao) : ViewModel() {

    val history: StateFlow<List<HistoryEntry>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteEntry(entry: HistoryEntry) {
        viewModelScope.launch {
            historyDao.delete(entry)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            historyDao.deleteAll()
        }
    }
}
