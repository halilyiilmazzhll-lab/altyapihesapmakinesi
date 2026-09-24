import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = '''                    node.depth?.let {
                        Text("Derinlik m", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (state.nodes.size > 2) {'''

replacement = '''                    node.depth?.let {
                        Text("Derinlik m", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    com.example.egimhesabi.ui.components.StakeoutPicker(
                        targetLabel = "Baca ",
                        source = node.stakeoutSource,
                        onSelect = { viewModel.importManhole(node.id, it) }
                    )
                    if (state.nodes.size > 2) {'''
content = content.replace(target, replacement)

target2 = '''            CompactField("Baca No / Adı", node.name, { viewModel.updateName(node.id, it) }, Modifier.fillMaxWidth(), keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
            StakeoutPicker(
                targetLabel = "Baca ",
                source = node.stakeoutSource,
                onSelect = { viewModel.importManhole(node.id, it) }
            )
            
            if (showCover'''

replacement2 = '''            CompactField("Baca No / Adı", node.name, { viewModel.updateName(node.id, it) }, Modifier.fillMaxWidth(), keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
            
            if (showCover'''
content = content.replace(target2, replacement2)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Patched Impact Picker UI')