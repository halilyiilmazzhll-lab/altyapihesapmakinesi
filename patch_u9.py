import codecs

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read().replace('\r\n', '\n')

target_invert = '''            val invertLabel = if (node.deltaCm != 0.0) "Sabit Akar (Aktif: ${node.invert?.let { format(it, 2) }})" else "Akar kotu"
            CompactField(invertLabel, node.invertText, { viewModel.updateInvert(node.id, it) }, Modifier.fillMaxWidth(), suffix = "m")'''

repl_invert = '''            val invertLabel = if (node.deltaCm != 0.0) "Sabit Akar (Aktif: ${node.invert?.let { format(it, 2) }})" else if (node.isDepthMode) "Derinlik" else "Akar kotu"
            val invertValue = if (node.isDepthMode) node.depthText else node.invertText
            val onInvertChange = { it: String -> if (node.isDepthMode) viewModel.updateDepth(node.id, it) else viewModel.updateInvert(node.id, it) }
            
            CompactField(
                label = invertLabel, 
                value = invertValue, 
                onValueChange = onInvertChange, 
                modifier = Modifier.fillMaxWidth(), 
                suffix = "m",
                trailingIcon = {
                    IconButton(onClick = { viewModel.toggleDepthMode(node.id) }, modifier = Modifier.size(24.dp)) {
                        Icon(androidx.compose.material.icons.Icons.Default.SwapVert, "Mod", tint = AccentOrange)
                    }
                }
            )'''

content = content.replace(target_invert, repl_invert)

target_compact = '''private fun CompactField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(50.dp),
        label = { Text(label, fontSize = 9.sp) },
        suffix = suffix?.let { { Text(it, color = TextTertiary, fontSize = 9.sp) } },
        singleLine = true,'''

repl_compact = '''private fun CompactField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(50.dp),
        label = { Text(label, fontSize = 9.sp) },
        suffix = suffix?.let { { Text(it, color = TextTertiary, fontSize = 9.sp) } },
        trailingIcon = trailingIcon,
        singleLine = true,'''
content = content.replace(target_compact, repl_compact)

if 'import androidx.compose.material.icons.filled.SwapVert' not in content:
    content = content.replace('import androidx.compose.material.icons.filled.Close', 'import androidx.compose.material.icons.filled.Close\nimport androidx.compose.material.icons.filled.SwapVert')

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Replaced substrings in ImpactCalculationScreen")