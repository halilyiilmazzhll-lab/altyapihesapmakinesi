package com.example.egimhesabi.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.egimhesabi.domain.StakeoutCalculationSource
import com.example.egimhesabi.data.ImpactHistoryDao
import com.example.egimhesabi.data.ImpactHistoryEntry
import com.example.egimhesabi.data.SettingsDataStore
import com.example.egimhesabi.util.NumberParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

private const val CALCULATION_EPSILON = 1e-9

private fun normalizedRange(minimum: Double, maximum: Double): Pair<Double, Double>? {
    if (minimum > maximum + CALCULATION_EPSILON) return null
    if (minimum <= maximum) return minimum to maximum

    // Matematiksel olarak aynı olan iki sınır, kayan nokta gürültüsüyle çok
    // az yer değiştirdiyse aralığı tek bir ortak değerde birleştir.
    val midpoint = (minimum + maximum) / 2.0
    return midpoint to midpoint
}

enum class ImpactSlopeStatus {
    VALID,
    TOO_FLAT,
    TOO_STEEP,
    REVERSE,
    INCOMPLETE
}

data class ImpactSegmentAnalysis(
    val dropMeters: Double? = null,
    val slopePercent: Double? = null,
    val slopeRatio: Double? = null,
    val status: ImpactSlopeStatus = ImpactSlopeStatus.INCOMPLETE
)

data class InvertRangeRecommendation(
    val minimumInvert: Double,
    val maximumInvert: Double,
    val recommendedInvert: Double,
    val recommendedDeltaCm: Double,
    val minimumDepth: Double?,
    val maximumDepth: Double?,
    val recommendedDepth: Double?
)

@Serializable
data class ManholeNodeData(
    val stakeoutSource: StakeoutCalculationSource? = null,
    val name: String = "",
    val coverText: String = "",
    val invertText: String = "",
    val isDepthMode: Boolean = false,
    val depthText: String = "",
    val deltaCm: Double = 0.0,
    val distanceToNextText: String = ""
)

data class ManholeNode(
    val stakeoutSource: StakeoutCalculationSource? = null,
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val coverText: String = "",
    val invertText: String = "",
    val isDepthMode: Boolean = false,
    val depthText: String = "",
    val deltaCm: Double = 0.0,
    val distanceToNextText: String = ""
) {
    val baseInvert: Double? get() = NumberParser.parseDecimal(invertText)
    val invert: Double? get() = baseInvert?.plus(deltaCm / 100.0)
    val cover: Double? get() = NumberParser.parseDecimal(coverText)
    val depth: Double? get() = if (cover != null && invert != null) cover!! - invert!! else null

    fun toData(): ManholeNodeData = ManholeNodeData(
        stakeoutSource = stakeoutSource,
        name = name,
        coverText = coverText,
        invertText = invertText,
        deltaCm = deltaCm,
        isDepthMode = isDepthMode,
        depthText = depthText,
        distanceToNextText = distanceToNextText
    )

    companion object {
        fun fromData(data: ManholeNodeData): ManholeNode = ManholeNode(
            stakeoutSource = data.stakeoutSource,
            name = data.name,
            coverText = data.coverText,
            invertText = data.invertText,
            deltaCm = data.deltaCm,
            isDepthMode = data.isDepthMode,
            depthText = data.depthText,
            distanceToNextText = data.distanceToNextText
        )
    }
}

data class ImpactCalculationState(
    val nodes: List<ManholeNode> = listOf(
        ManholeNode(coverText = "100.00", invertText = "98.00", distanceToNextText = "30.00"),
        ManholeNode(coverText = "99.00", invertText = "97.40", distanceToNextText = "30.00"),
        ManholeNode(coverText = "98.00", invertText = "97.00", distanceToNextText = "")
    ),
    val minSlopePercent: Double = 0.5,
    val maxSlopePercent: Double = 5.0,
    val minManholeDepthMeters: Double = 1.0,
    val usingHistoryLimits: Boolean = false
) {
    companion object {
        fun inferFlowLeftToRight(nodes: List<ManholeNode>): Boolean? {
            val firstInvert = nodes.firstOrNull()?.invert ?: return null
            val lastInvert = nodes.lastOrNull()?.invert ?: return null
            return when {
                firstInvert > lastInvert -> true
                firstInvert < lastInvert -> false
                else -> null
            }
        }
    }

    /** Profilin genel akışı, uçlardaki yüksek akar kotundan düşük kota doğrudur. */
    val flowLeftToRight: Boolean?
        get() = inferFlowLeftToRight(nodes)

    /** Her hattaki su oku daima o hattın yüksek kotundan düşük kotuna akar. */
    fun getSegmentFlowLeftToRight(index: Int): Boolean? {
        if (index < 0 || index >= nodes.size - 1) return null
        val current = nodes[index].invert ?: return null
        val next = nodes[index + 1].invert ?: return null
        return when {
            current > next -> true
            current < next -> false
            else -> null
        }
    }

    val allInputsValid: Boolean
        get() = nodes.all { it.invert != null } && 
                nodes.dropLast(1).all { (NumberParser.parseDecimal(it.distanceToNextText) ?: 0.0) > 0.0 }

    val allSegmentsValid: Boolean
        get() = flowLeftToRight != null &&
            (0 until nodes.size - 1).all { i ->
                getSegmentAnalysis(i).status == ImpactSlopeStatus.VALID
            }

    fun getSegmentAnalysis(index: Int): ImpactSegmentAnalysis {
        if (index < 0 || index >= nodes.size - 1) return ImpactSegmentAnalysis()
        val current = nodes[index].invert
        val next = nodes[index + 1].invert
        val distance = NumberParser.parseDecimal(nodes[index].distanceToNextText)?.takeIf { it > 0.0 }
        
        if (current == null || next == null || distance == null) return ImpactSegmentAnalysis()
        
        val profileFlowsLeftToRight = flowLeftToRight
        val drop = when (profileFlowsLeftToRight) {
            true -> current - next
            false -> next - current
            null -> abs(current - next)
        }
        val percent = abs(drop / distance * 100.0)
        
        val effectiveMinSlope = min(minSlopePercent, maxSlopePercent)
        val effectiveMaxSlope = max(minSlopePercent, maxSlopePercent)
        val status = when {
            drop < -CALCULATION_EPSILON -> ImpactSlopeStatus.REVERSE
            percent <= CALCULATION_EPSILON -> ImpactSlopeStatus.TOO_FLAT
            percent < effectiveMinSlope - CALCULATION_EPSILON -> ImpactSlopeStatus.TOO_FLAT
            percent > effectiveMaxSlope + CALCULATION_EPSILON -> ImpactSlopeStatus.TOO_STEEP
            else -> ImpactSlopeStatus.VALID
        }
        return ImpactSegmentAnalysis(
            dropMeters = drop,
            slopePercent = percent,
            slopeRatio = if (percent > 0.0) 100.0 / percent else null,
            status = status
        )
    }

    fun getRecommendationFor(index: Int): InvertRangeRecommendation? {
        if (index <= 0 || index >= nodes.size) return null
        val profileFlowsLeftToRight = flowLeftToRight ?: return null
        val targetNode = nodes[index]
        val currentInvert = targetNode.invert ?: return null
        val baseInvert = targetNode.baseInvert ?: return null

        val effectiveMinSlope = min(minSlopePercent, maxSlopePercent) / 100.0
        val effectiveMaxSlope = max(minSlopePercent, maxSlopePercent) / 100.0

        // Önceki komşuyla olan aralık, uç kotlardan bulunan doğal akış
        // yönüne göre yalnızca düşüş yönündeki kotları kabul eder.
        val upNode = nodes[index - 1]
        val upInvert = upNode.invert ?: return null
        val distUp = NumberParser.parseDecimal(upNode.distanceToNextText)
            ?.takeIf { it > 0.0 } ?: return null
        
        val upRange = if (profileFlowsLeftToRight) {
            upInvert - distUp * effectiveMaxSlope to
                upInvert - distUp * effectiveMinSlope
        } else {
            upInvert + distUp * effectiveMinSlope to
                upInvert + distUp * effectiveMaxSlope
        }

        // Sonraki komşuyla olan aralık da aynı seçili yönü korur.
        val downRange = if (index < nodes.size - 1) {
            val downNode = nodes[index + 1]
            val downInvert = downNode.invert ?: return null
            val distDown = NumberParser.parseDecimal(targetNode.distanceToNextText)
                ?.takeIf { it > 0.0 } ?: return null
            if (profileFlowsLeftToRight) {
                downInvert + distDown * effectiveMinSlope to
                    downInvert + distDown * effectiveMaxSlope
            } else {
                downInvert - distDown * effectiveMaxSlope to
                    downInvert - distDown * effectiveMinSlope
            }
        } else null

        val validIntersections = mutableListOf<Pair<Double, Double>>()
        
        if (downRange != null) {
            val minInv = max(upRange.first, downRange.first)
            val maxInv = min(upRange.second, downRange.second)
            normalizedRange(minInv, maxInv)?.let(validIntersections::add)
        } else {
            validIntersections.add(upRange)
        }

        if (validIntersections.isEmpty()) return null

        val cover = targetNode.cover
        val depthConstrainedIntersections = if (cover != null) {
            val absoluteMaxInvert = cover - minManholeDepthMeters
            validIntersections.mapNotNull { range ->
                normalizedRange(range.first, min(range.second, absoluteMaxInvert))
            }
        } else {
            validIntersections
        }

        if (depthConstrainedIntersections.isEmpty()) return null

        val bestIntersection = depthConstrainedIntersections.minByOrNull { range ->
            when {
                currentInvert < range.first -> range.first - currentInvert
                currentInvert > range.second -> currentInvert - range.second
                else -> 0.0
            }
        } ?: depthConstrainedIntersections.first()

        val minimumInvert = bestIntersection.first
        val maximumInvert = bestIntersection.second
        val recommendedInvert = currentInvert.coerceIn(minimumInvert, maximumInvert)

        return InvertRangeRecommendation(
            minimumInvert = minimumInvert,
            maximumInvert = maximumInvert,
            recommendedInvert = recommendedInvert,
            recommendedDeltaCm = (recommendedInvert - baseInvert) * 100.0,
            minimumDepth = cover?.minus(maximumInvert),
            maximumDepth = cover?.minus(minimumInvert),
            recommendedDepth = cover?.minus(recommendedInvert)
        )
    }
}

class ImpactCalculationViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val impactHistoryDao: ImpactHistoryDao,
    private val savedState: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private var currentLimits = com.example.egimhesabi.data.SlopeLimits()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    fun clearMessage() { _message.value = null }
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _uiState = MutableStateFlow(restoreState())
    private fun restoreState(): ImpactCalculationState {
        val nodes = savedState.get<String>("impactNodes")?.let { text ->
            runCatching { json.decodeFromString<List<ManholeNodeData>>(text).map(ManholeNode::fromData) }.getOrNull()
        }
        return ImpactCalculationState(
            nodes = nodes?.takeIf { it.size >= 2 } ?: ImpactCalculationState().nodes,
            minSlopePercent = savedState["impactMin"] ?: 0.5,
            maxSlopePercent = savedState["impactMax"] ?: 5.0,
            minManholeDepthMeters = savedState["impactDepth"] ?: 1.0,
            usingHistoryLimits = savedState["impactHistoryLimits"] ?: false
        )
    }

    fun useCurrentLimits() {
        _uiState.update { it.copy(minSlopePercent = currentLimits.minSlopePercent,
            maxSlopePercent = currentLimits.maxSlopePercent, minManholeDepthMeters = currentLimits.minManholeDepthMeters,
            usingHistoryLimits = false) }
    }
    val uiState: StateFlow<ImpactCalculationState> = _uiState.asStateFlow()

    val impactHistory: StateFlow<List<ImpactHistoryEntry>> = impactHistoryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _uiState.collect { state ->
                savedState["impactNodes"] = json.encodeToString(state.nodes.map { it.toData() })
                savedState["impactMin"] = state.minSlopePercent
                savedState["impactMax"] = state.maxSlopePercent
                savedState["impactDepth"] = state.minManholeDepthMeters
                savedState["impactHistoryLimits"] = state.usingHistoryLimits
            }
        }
        viewModelScope.launch {
            settingsDataStore.slopeLimits.collect { limits ->
                currentLimits = limits
                _uiState.update {
                    if (it.usingHistoryLimits) return@update it
                    it.copy(
                        minSlopePercent = limits.minSlopePercent,
                        maxSlopePercent = limits.maxSlopePercent,
                        minManholeDepthMeters = limits.minManholeDepthMeters
                    )
                }
            }
        }
    }

    fun saveCurrentCalculations() {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
            val state = _uiState.value
            val nodesData = state.nodes.map { it.toData() }
            val nodesJson = json.encodeToString(nodesData)

            val segmentCount = state.nodes.size - 1
            val allValid = state.allInputsValid && state.allSegmentsValid

            val summaryParts = mutableListOf<String>()
            summaryParts.add("${state.nodes.size} baca")
            for (i in 0 until segmentCount) {
                val seg = state.getSegmentAnalysis(i)
                val pct = seg.slopePercent?.let { NumberParser.formatDecimal(it, 2) } ?: "?"
                summaryParts.add("Hat${i+1}: %$pct")
            }
            val summaryText = summaryParts.joinToString(" · ")

            val entry = ImpactHistoryEntry(
                nodesJson = nodesJson,
                minSlopePercent = state.minSlopePercent,
                maxSlopePercent = state.maxSlopePercent,
                minManholeDepthMeters = state.minManholeDepthMeters,
                segmentCount = segmentCount,
                allValid = allValid,
                summaryText = summaryText
            )
            impactHistoryDao.insert(entry)
            _message.value = "Hesaplamalar geçmişe kaydedildi."
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { _message.value = error.message ?: "Hesap kaydedilemedi." }
            finally { _saving.value = false }
        }
    }

    fun loadFromHistory(entry: ImpactHistoryEntry) {
        try {
            val nodesData: List<ManholeNodeData> = json.decodeFromString(entry.nodesJson)
            val nodes = nodesData.map { ManholeNode.fromData(it) }
            if (nodes.size >= 2) {
                _uiState.update { state ->
                    state.copy(nodes = nodes, minSlopePercent = entry.minSlopePercent, maxSlopePercent = entry.maxSlopePercent, minManholeDepthMeters = entry.minManholeDepthMeters, usingHistoryLimits = true)
                }
            }
        } catch (_: Exception) {
            _message.value = "Geçmiş kaydı okunamadı. Mevcut girdileriniz korundu."
        }
    }

    fun deleteHistoryEntry(entry: ImpactHistoryEntry) {
        viewModelScope.launch {
            impactHistoryDao.delete(entry)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            impactHistoryDao.deleteAll()
        }
    }

    fun addManhole() {
        _uiState.update { state ->
            val newNodes = state.nodes.toMutableList()
            val lastNode = newNodes.lastOrNull()
            if (lastNode != null) {
                // Ensure the previous last node has a default distance
                newNodes[newNodes.lastIndex] = lastNode.copy(distanceToNextText = lastNode.distanceToNextText.ifBlank { "30.00" })
            }
            newNodes.add(ManholeNode(coverText = "", invertText = "", depthText = "", isDepthMode = false, distanceToNextText = ""))
            state.copy(nodes = newNodes)
        }
    }


    fun importChain(chain: List<Pair<com.example.egimhesabi.domain.StakeoutCalculationSource, Double?>>) {
        if (chain.isEmpty()) return
        _uiState.update { state ->
            val newNodes = chain.mapIndexed { index, pair ->
                val source = pair.first
                val dist = pair.second
                ManholeNode(
                    name = source.manholeName,
                    coverText = source.upperText,
                    invertText = source.invertText,
                    isDepthMode = false,
                    depthText = "",
                    distanceToNextText = dist?.let { com.example.egimhesabi.util.NumberParser.formatDecimal(it) } ?: if (index < chain.size - 1) "30.00" else "",
                    stakeoutSource = source,
                    deltaCm = 0.0
                )
            }.toMutableList()
            
            while (newNodes.size < 2) {
                newNodes.add(ManholeNode())
            }
            
            val lastIdx = newNodes.lastIndex
            newNodes[lastIdx] = newNodes[lastIdx].copy(distanceToNextText = "")
            
            state.copy(nodes = newNodes)
        }
    }

    fun removeManhole(id: String) {
        _uiState.update { state ->
            if (state.nodes.size <= 2) return@update state // Minimum 2 manholes
            val newNodes = state.nodes.filterNot { it.id == id }.toMutableList()
            // Clear distance of the new last node
            if (newNodes.isNotEmpty()) {
                val lastIdx = newNodes.lastIndex
                newNodes[lastIdx] = newNodes[lastIdx].copy(distanceToNextText = "")
            }
            state.copy(nodes = newNodes)
        }
    }

    fun updateName(id: String, value: String) {
        updateNode(id) { it.copy(name = value, stakeoutSource = null) }
    }

    fun updateCover(id: String, value: String) {
        updateNode(id) { node ->
            var newInvert = node.invertText
            if (node.isDepthMode) {
                val c = com.example.egimhesabi.util.NumberParser.parseDecimal(value)
                val d = com.example.egimhesabi.util.NumberParser.parseDecimal(node.depthText)
                if (c != null && d != null) {
                    newInvert = com.example.egimhesabi.util.NumberParser.formatDecimal(c - d)
                }
            }
            node.copy(coverText = value, invertText = newInvert, stakeoutSource = null)
        }
    }

    fun updateInvert(id: String, value: String) {
        updateNode(id) { it.copy(invertText = value, deltaCm = 0.0, stakeoutSource = null) }
    }
    
    fun updateDepth(id: String, value: String) {
        updateNode(id) { node ->
            var newInvert = node.invertText
            val c = com.example.egimhesabi.util.NumberParser.parseDecimal(node.coverText)
            val d = com.example.egimhesabi.util.NumberParser.parseDecimal(value)
            if (c != null && d != null) {
                newInvert = com.example.egimhesabi.util.NumberParser.formatDecimal(c - d)
            } else if (value.isEmpty()) {
                newInvert = ""
            }
            node.copy(depthText = value, invertText = newInvert, deltaCm = 0.0, stakeoutSource = null)
        }
    }
    
    fun toggleDepthMode(id: String) {
        updateNode(id) { node ->
            val newMode = !node.isDepthMode
            var newDepthText = node.depthText
            if (newMode) {
                val d = node.depth
                if (d != null) newDepthText = com.example.egimhesabi.util.NumberParser.formatDecimal(d)
            }
            node.copy(isDepthMode = newMode, depthText = newDepthText)
        }
    }

    fun importManhole(id: String, source: StakeoutCalculationSource) {
        if (source.invertLevel == null) return
        updateNode(id) {
            it.copy(
                name = source.manholeName,
                coverText = source.upperText,
                invertText = source.invertText,
                isDepthMode = false,
                depthText = "",
                deltaCm = 0.0,
                stakeoutSource = source
            )
        }
    }

    fun updateDistance(id: String, value: String) {
        updateNode(id) { it.copy(distanceToNextText = value) }
    }

    fun adjustDelta(id: String, deltaCm: Double) {
        updateNode(id) { it.copy(deltaCm = it.deltaCm + deltaCm) }
    }

    fun resetDelta(id: String) {
        updateNode(id) { it.copy(deltaCm = 0.0) }
    }

    fun applyRecommendation(id: String) {
        val state = _uiState.value
        val index = state.nodes.indexOfFirst { it.id == id }
        if (index > 0) { // First node cannot be adjusted based on upstream
            val rec = state.getRecommendationFor(index)
            if (rec != null) {
                updateNode(id) { it.copy(deltaCm = rec.recommendedDeltaCm) }
            }
        }
    }

    private fun updateNode(id: String, updater: (ManholeNode) -> ManholeNode) {
        _uiState.update { state ->
            val index = state.nodes.indexOfFirst { it.id == id }
            if (index != -1) {
                val newNodes = state.nodes.toMutableList()
                newNodes[index] = updater(newNodes[index])
                state.copy(nodes = newNodes)
            } else state
        }
    }
}
