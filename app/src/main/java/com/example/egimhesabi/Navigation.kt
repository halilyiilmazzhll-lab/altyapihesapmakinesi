package com.example.egimhesabi

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.egimhesabi.data.AppDatabase
import com.example.egimhesabi.data.WorkOrderPhotoStore
import com.example.egimhesabi.data.SettingsDataStore
import com.example.egimhesabi.ui.screens.*
import com.example.egimhesabi.viewmodel.*
import kotlinx.coroutines.delay

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Home)
    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context.applicationContext) }
    val appDatabase = remember { AppDatabase.getInstance(context.applicationContext) }
    val historyDao = remember { appDatabase.historyDao() }
    val impactHistoryDao = remember { appDatabase.impactHistoryDao() }
    val districtDao = remember { appDatabase.districtDao() }
    val projectDao = remember { appDatabase.projectDao() }
    val manholeDao = remember { appDatabase.manholeDao() }
    val pipelineDao = remember { appDatabase.pipelineDao() }
    val workOrderDao = remember { appDatabase.workOrderDao() }
    val workOrderPhotoStore = remember { WorkOrderPhotoStore(context.applicationContext) }
    val levelingViewModel: LevelingViewModel = viewModel()
    val elevationTransferViewModel: ElevationTransferViewModel = viewModel()

    var openPhotoPath by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    openPhotoPath?.let { path -> com.example.egimhesabi.ui.components.PhotoViewer(path) { openPhotoPath = null } }
    val currentKey: NavKey = backStack.lastOrNull() ?: Home

    var backPressedOnce by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2000)
            backPressedOnce = false
        }
    }
    
    androidx.activity.compose.BackHandler {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        } else if (backPressedOnce) {
            (context as? android.app.Activity)?.finish()
        } else {
            backPressedOnce = true
            android.widget.Toast.makeText(context, "Çıkmak için tekrar dokunun", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val navigateTo = { targetKey: NavKey ->
        if (currentKey != targetKey) {
            backStack.add(targetKey)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            entry<Home> {
                HomeScreen(
                    onNavigateStakeout = { navigateTo(Stakeout) },
                    onNavigateTracking = { navigateTo(Tracking) },
                    onNavigateSlope = { navigateTo(Main) },
                    onNavigateInterpolation = { navigateTo(Interpolation) },
                    onNavigateReverse = { navigateTo(Reverse) },
                    onNavigateImpact = { navigateTo(Impact) },
                    onNavigateLeveling = {
                        navigateTo(Leveling)
                    },
                    onNavigateElevationTransfer = {
                        navigateTo(ElevationTransfer)
                    },
                    onNavigateSettings = { navigateTo(Settings) }
                )
            }
            entry<Stakeout> {
                val stakeoutViewModel: StakeoutViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                            return StakeoutViewModel(districtDao, appDatabase.stakeoutDao(), extras.createSavedStateHandle()) as T
                        }
                    }
                )
                StakeoutScreen(
                    viewModel = stakeoutViewModel,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Tracking> {
                val trackingViewModel: TrackingViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return TrackingViewModel(districtDao, projectDao, manholeDao, settingsDataStore) as T
                        }
                    }
                )
                TrackingScreen(
                    viewModel = trackingViewModel,
                    onOpenDrawer = { backStack.removeLastOrNull() },
                    onOpenProject = { project ->
                        backStack.add(ProjectMap(project.id, project.name))
                    },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<ProjectMap> { key ->
                val projectDetailViewModel: ProjectDetailViewModel = viewModel(
                    key = "project-detail-${key.projectId}",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ProjectDetailViewModel(
                                key.projectId,
                                projectDao,
                                manholeDao,
                                pipelineDao,
                                workOrderDao,
                                workOrderPhotoStore,
                                settingsDataStore
                            ) as T
                        }
                    }
                )
                MapScreen(
                    projectName = key.projectName,
                    viewModel = projectDetailViewModel,
                    onBack = { backStack.removeLastOrNull() },
                    onOpenList = {
                        backStack.add(ProjectPipelines(key.projectId, key.projectName))
                    },
                    onOpenOrderBook = {
                        backStack.add(ProjectOrderBook(key.projectId, key.projectName))
                    },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<ProjectPipelines> { key ->
                val projectDetailViewModel: ProjectDetailViewModel = viewModel(
                    key = "project-detail-${key.projectId}",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ProjectDetailViewModel(
                                key.projectId,
                                projectDao,
                                manholeDao,
                                pipelineDao,
                                workOrderDao,
                                workOrderPhotoStore,
                                settingsDataStore
                            ) as T
                        }
                    }
                )
                ProjectListScreen(
                    projectName = key.projectName,
                    viewModel = projectDetailViewModel,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<ProjectOrderBook> { key ->
                val orderBookViewModel: OrderBookViewModel = viewModel(
                    key = "order-book-${key.projectId}",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return OrderBookViewModel(key.projectId, workOrderDao, manholeDao) as T
                        }
                    }
                )
                OrderBookScreen(
                    projectName = key.projectName,
                    viewModel = orderBookViewModel,
                    onBack = { backStack.removeLastOrNull() },
                    onOpenPhoto = { openPhotoPath = it },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Main> {
                val slopeViewModel: SlopeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                            val savedStateHandle = extras.createSavedStateHandle()
                            return SlopeViewModel(settingsDataStore, historyDao, savedStateHandle) as T
                        }
                    }
                )
                SlopeCalculatorScreen(
                    viewModel = slopeViewModel,
                    onOpenDrawer = { backStack.removeLastOrNull() },
                    onNavigateToHistory = { backStack.add(History) },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<History> {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return HistoryViewModel(historyDao) as T
                        }
                    }
                )
                HistoryScreen(
                    viewModel = historyViewModel,
                    onClose = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Interpolation> {
                val interpolationViewModel: InterpolationViewModel = viewModel()
                InterpolationScreen(
                    viewModel = interpolationViewModel,
                    onOpenDrawer = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Reverse> {
                val reverseViewModel: ReverseCalculationViewModel = viewModel()
                ReverseCalculationScreen(
                    viewModel = reverseViewModel,
                    onOpenDrawer = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Leveling> {
                LevelingScreen(
                    viewModel = levelingViewModel,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<ElevationTransfer> {
                ElevationTransferScreen(
                    viewModel = elevationTransferViewModel,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Impact> {
                val impactViewModel: ImpactCalculationViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                            return ImpactCalculationViewModel(settingsDataStore, impactHistoryDao, extras.createSavedStateHandle()) as T
                        }
                    }
                )
                ImpactCalculationScreen(
                    viewModel = impactViewModel,
                    onOpenDrawer = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Settings> {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SettingsViewModel(settingsDataStore) as T
                        }
                    }
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onOpenDrawer = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}


