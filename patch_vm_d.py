import codecs

path = 'app/src/main/java/com/example/egimhesabi/viewmodel/ImpactCalculationViewModel.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target_cover = '''    fun updateCover(id: String, value: String) {
        updateNode(id) { it.copy(coverText = value, stakeoutSource = null) }
    }'''
repl_cover = '''    fun updateCover(id: String, value: String) {
        updateNode(id) { node ->
            var newInvert = node.invertText
            if (node.isDepthMode) {
                val c = com.example.egimhesabi.util.NumberParser.parseDouble(value)
                val d = com.example.egimhesabi.util.NumberParser.parseDouble(node.depthText)
                if (c != null && d != null) {
                    newInvert = com.example.egimhesabi.util.NumberParser.formatDecimal(c - d)
                }
            }
            node.copy(coverText = value, invertText = newInvert, stakeoutSource = null)
        }
    }'''
content = content.replace(target_cover, repl_cover)

target_invert = '''    fun updateInvert(id: String, value: String) {
        updateNode(id) { it.copy(invertText = value, deltaCm = 0.0, stakeoutSource = null) }
    }'''
repl_invert = '''    fun updateInvert(id: String, value: String) {
        updateNode(id) { it.copy(invertText = value, deltaCm = 0.0, stakeoutSource = null) }
    }
    
    fun updateDepth(id: String, value: String) {
        updateNode(id) { node ->
            var newInvert = node.invertText
            val c = com.example.egimhesabi.util.NumberParser.parseDouble(node.coverText)
            val d = com.example.egimhesabi.util.NumberParser.parseDouble(value)
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
    }'''
content = content.replace(target_invert, repl_invert)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Patched VM methods')