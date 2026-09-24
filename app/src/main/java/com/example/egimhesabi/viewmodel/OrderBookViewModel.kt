package com.example.egimhesabi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.egimhesabi.data.ManholeDao
import com.example.egimhesabi.data.WorkOrderDao
import com.example.egimhesabi.data.WorkOrderWithPhotos
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class OrderBookItem(
    val workOrderWithPhotos: WorkOrderWithPhotos,
    val manholeName: String
)

class OrderBookViewModel(
    private val projectId: Long,
    private val workOrderDao: WorkOrderDao,
    private val manholeDao: ManholeDao
) : ViewModel() {

    val distinctProgressPayments: StateFlow<List<Int>> = workOrderDao
        .observeDistinctProgressPayments(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedTab = MutableStateFlow<Int?>(null) // null = Bekleyenler, >0 = Hakediş no
    val selectedTab: StateFlow<Int?> = _selectedTab.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val workOrders: StateFlow<List<OrderBookItem>> = _selectedTab
        .flatMapLatest { tab ->
            val ordersFlow = if (tab == null) {
                workOrderDao.observeUnassigned(projectId)
            } else {
                workOrderDao.observeByProgressPayment(projectId, tab)
            }
            combine(ordersFlow, manholeDao.getManholesByProjectId(projectId)) { orders, manholes ->
                val manholeMap = manholes.associateBy { it.id }
                orders.map { order ->
                    OrderBookItem(
                        workOrderWithPhotos = order,
                        manholeName = manholeMap[order.workOrder.manholeId]?.name ?: "Bilinmeyen Baca"
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectTab(tab: Int?) {
        _selectedTab.value = tab
    }
}

