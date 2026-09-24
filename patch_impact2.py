import codecs

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with open(path, 'rb') as f:
    raw = f.read()
content = raw.decode('utf-8', errors='replace')

# 1. State and top bar in ImpactCalculationScreen
old_states = """    var showAsRatio by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }"""

new_states = """    var showAsRatio by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var mapTargetNodeId by remember { mutableStateOf<String?>(null) }
    var mapTargetLabel by remember { mutableStateOf<String?>(null) }"""

content = content.replace(old_states, new_states)

# Add map action to topbar
old_actions = """                actions = {
                    IconButton(
                        onClick = { showHistory = true }
                    ) {"""

new_actions = """                actions = {
                    IconButton(
                        onClick = {
                            mapTargetNodeId = null
                            mapTargetLabel = null
                            showMapPicker = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Haritadan Hat Seç",
                            tint = AccentOrange
                        )
                    }
                    IconButton(
                        onClick = { showHistory = true }
                    ) {"""

content = content.replace(old_actions, new_actions)

# Update ManholeCard call
old_call = """                itemsIndexed(state.nodes, key = { _, node -> node.id }) { index, node ->
                    ManholeCard(index, node, state, viewModel)
                }"""

new_call = """                itemsIndexed(state.nodes, key = { _, node -> node.id }) { index, node ->
                    ManholeCard(
                        index = index,
                        node = node,
                        state = state,
                        viewModel = viewModel,
                        onOpenMap = {
                            mapTargetNodeId = node.id
                            mapTargetLabel = "${index + 1}. Baca"
                            showMapPicker = true
                        }
                    )
                }"""

content = content.replace(old_call, new_call)

# Add MapChainButton next to AddManholeButton in LazyRow
old_add_btn = """                item {
                    AddManholeButton(onClick = viewModel::addManhole)
                }"""

new_add_btn = """                item {
                    AddManholeButton(onClick = viewModel::addManhole)
                }
                item {
                    Surface(
                        onClick = {
                            mapTargetNodeId = null
                            mapTargetLabel = null
                            showMapPicker = true
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
                        modifier = Modifier.height(180.dp).width(120.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(Color(0xFFFFF2ED), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Map, null, tint = AccentOrange, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Haritadan\\nHat Seç",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }"""

content = content.replace(old_add_btn, new_add_btn)

# Add showMapPicker dialog rendering right after Scaffold
old_scaffold_open = """        containerColor = GradientStart,
        modifier = modifier
    ) { paddingValues ->"""

new_scaffold_open = """        containerColor = GradientStart,
        modifier = modifier
    ) { paddingValues ->
        if (showMapPicker) {
            ImpactMapPicker(
                targetNodeId = mapTargetNodeId,
                targetLabel = mapTargetLabel,
                onDismiss = { showMapPicker = false },
                onImportChain = { chain ->
                    viewModel.importChain(chain)
                    showMapPicker = false
                },
                onImportSingle = { source ->
                    mapTargetNodeId?.let { id ->
                        viewModel.importManhole(id, source)
                    }
                    showMapPicker = false
                }
            )
        }"""

content = content.replace(old_scaffold_open, new_scaffold_open)

# Update ManholeCard signature and add Map button in header
old_manhole_card_def = """@Composable
private fun ManholeCard(
    index: Int,
    node: ManholeNode,
    state: ImpactCalculationState,
    viewModel: ImpactCalculationViewModel
) {"""

new_manhole_card_def = """@Composable
private fun ManholeCard(
    index: Int,
    node: ManholeNode,
    state: ImpactCalculationState,
    viewModel: ImpactCalculationViewModel,
    onOpenMap: () -> Unit
) {"""

content = content.replace(old_manhole_card_def, new_manhole_card_def)

old_header_picker = """                    com.example.egimhesabi.ui.components.StakeoutPicker(
                        targetLabel = "Baca ${index + 1}",
                        source = node.stakeoutSource,
                        onSelect = { viewModel.importManhole(node.id, it) }
                    )"""

new_header_picker = """                    IconButton(
                        onClick = onOpenMap,
                        modifier = Modifier
                            .background(Color(0xFFF0F4F9), RoundedCornerShape(8.dp))
                            .size(32.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Haritadan seç", tint = AccentOrange, modifier = Modifier.size(18.dp))
                    }
                    com.example.egimhesabi.ui.components.StakeoutPicker(
                        targetLabel = "Baca ${index + 1}",
                        source = node.stakeoutSource,
                        onSelect = { viewModel.importManhole(node.id, it) }
                    )"""

content = content.replace(old_header_picker, new_header_picker)

with open(path, 'wb') as f:
    f.write(content.encode('utf-8'))
print("ImpactCalculationScreen updated with map triggers")