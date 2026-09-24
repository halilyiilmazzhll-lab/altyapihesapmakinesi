import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# 1. Add showMapPicker state
target_state = '''    var showHistory by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }'''
replacement_state = '''    var showHistory by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }'''
content = content.replace(target_state, replacement_state, 1)

# 2. Add TopAppBar action
target_appbar = '''                    actions = {
                        IconButton(
                            onClick = { showHistory = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Geçmiş",
                                tint = AccentOrange
                            )
                        }'''
replacement_appbar = '''                    actions = {
                        IconButton(onClick = { showMapPicker = true }) {
                            Icon(androidx.compose.material.icons.Icons.Default.androidx.compose.material.icons.filled.Map, "Haritadan Seç", tint = AccentOrange)
                        }
                        IconButton(
                            onClick = { showHistory = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Geçmiş",
                                tint = AccentOrange
                            )
                        }'''
# Wait, syntax is Icons.Default.Map . Let me fix it.
replacement_appbar = '''                    actions = {
                        IconButton(onClick = { showMapPicker = true }) {
                            Icon(androidx.compose.material.icons.Icons.Default.Map, "Haritadan Seç", tint = AccentOrange)
                        }
                        IconButton(
                            onClick = { showHistory = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Geçmiş",
                                tint = AccentOrange
                            )
                        }'''

content = content.replace(target_appbar, replacement_appbar, 1)

# 3. Render ImpactMapPicker
target_picker = '''    if (showHistory) {
        ImpactHistoryDialog('''
replacement_picker = '''    if (showMapPicker) {
        ImpactMapPicker(
            onDismiss = { showMapPicker = false },
            onImport = { chain -> viewModel.importChain(chain) }
        )
    }

    if (showHistory) {
        ImpactHistoryDialog('''
content = content.replace(target_picker, replacement_picker, 1)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Wired ImpactMapPicker')