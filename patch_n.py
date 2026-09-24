import codecs

path = 'app/src/main/java/com/example/egimhesabi/viewmodel/ImpactCalculationViewModel.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Add isDepthMode and depthText to ManholeNode and ManholeNodeData
target_data = '''data class ManholeNodeData(
    val stakeoutSource: StakeoutCalculationSource? = null,
    val name: String = "",
    val coverText: String = "",
    val invertText: String = "",
    val deltaCm: Double = 0.0,
    val distanceToNextText: String = ""
)'''
repl_data = '''data class ManholeNodeData(
    val stakeoutSource: StakeoutCalculationSource? = null,
    val name: String = "",
    val coverText: String = "",
    val invertText: String = "",
    val isDepthMode: Boolean = false,
    val depthText: String = "",
    val deltaCm: Double = 0.0,
    val distanceToNextText: String = ""
)'''
content = content.replace(target_data, repl_data)

target_node = '''data class ManholeNode(
    val stakeoutSource: StakeoutCalculationSource? = null,
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val coverText: String = "",
    val invertText: String = "",
    val deltaCm: Double = 0.0,
    val distanceToNextText: String = ""
)'''
repl_node = '''data class ManholeNode(
    val stakeoutSource: StakeoutCalculationSource? = null,
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val coverText: String = "",
    val invertText: String = "",
    val isDepthMode: Boolean = false,
    val depthText: String = "",
    val deltaCm: Double = 0.0,
    val distanceToNextText: String = ""
)'''
content = content.replace(target_node, repl_node)

target_todata = '''        distanceToNextText = distanceToNextText
    )'''
repl_todata = '''        isDepthMode = isDepthMode,
        depthText = depthText,
        distanceToNextText = distanceToNextText
    )'''
content = content.replace(target_todata, repl_todata)

target_fromdata = '''                coverText = d.coverText,
                invertText = d.invertText,
                deltaCm = d.deltaCm,
                distanceToNextText = d.distanceToNextText
            )'''
repl_fromdata = '''                coverText = d.coverText,
                invertText = d.invertText,
                isDepthMode = d.isDepthMode,
                depthText = d.depthText,
                deltaCm = d.deltaCm,
                distanceToNextText = d.distanceToNextText
            )'''
content = content.replace(target_fromdata, repl_fromdata)

target_add = '''newNodes.add(ManholeNode(coverText = "", invertText = "", distanceToNextText = ""))'''
repl_add = '''newNodes.add(ManholeNode(coverText = "", invertText = "", depthText = "", isDepthMode = false, distanceToNextText = ""))'''
content = content.replace(target_add, repl_add)

target_importchain = '''                    invertText = source.invertText,
                    distanceToNextText = dist?.let { com.example.egimhesabi.util.NumberParser.formatDecimal(it) } ?: if (index < chain.size - 1) "30.00" else "",
                    stakeoutSource = source,
                    deltaCm = 0.0
                )'''
repl_importchain = '''                    invertText = source.invertText,
                    isDepthMode = false,
                    depthText = "",
                    distanceToNextText = dist?.let { com.example.egimhesabi.util.NumberParser.formatDecimal(it) } ?: if (index < chain.size - 1) "30.00" else "",
                    stakeoutSource = source,
                    deltaCm = 0.0
                )'''
content = content.replace(target_importchain, repl_importchain)

target_import = '''                name = source.qualifiedName,
                coverText = source.upperText,
                invertText = source.invertText,
                deltaCm = 0.0,
                stakeoutSource = source
            )'''
repl_import = '''                name = source.qualifiedName,
                coverText = source.upperText,
                invertText = source.invertText,
                isDepthMode = false,
                depthText = "",
                deltaCm = 0.0,
                stakeoutSource = source
            )'''
content = content.replace(target_import, repl_import)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Patched ManholeNode structure')