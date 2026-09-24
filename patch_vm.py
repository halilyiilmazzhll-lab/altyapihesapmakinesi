import codecs

path = 'app/src/main/java/com/example/egimhesabi/viewmodel/ImpactCalculationViewModel.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

new_func = '''
    fun importChain(chain: List<Pair<com.example.egimhesabi.domain.StakeoutCalculationSource, Double?>>) {
        if (chain.isEmpty()) return
        _uiState.update { state ->
            val newNodes = chain.mapIndexed { index, pair ->
                val source = pair.first
                val dist = pair.second
                ManholeNode(
                    name = source.qualifiedName,
                    coverText = source.upperText,
                    invertText = source.invertText,
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
        recalculate()
    }

    fun removeManhole(id: String) {'''

content = content.replace('    fun removeManhole(id: String) {', new_func, 1)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Patched VM')