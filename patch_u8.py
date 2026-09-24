import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# 1. Update CompactField signature
target_compact = r'keyboardActions: KeyboardActions = KeyboardActions\.Default\n\s*\) \{\n\s*OutlinedTextField'
repl_compact = r'keyboardActions: KeyboardActions = KeyboardActions.Default,\n    trailingIcon: @Composable (() -> Unit)? = null\n) {\n    OutlinedTextField'
content = re.sub(target_compact, repl_compact, content)

target_outlined = r'suffix = suffix\?\.let \{ \{ Text\(it, color = TextTertiary, fontSize = 9\.sp\) \} \},\n\s*singleLine = true'
repl_outlined = r'suffix = suffix?.let { { Text(it, color = TextTertiary, fontSize = 9.sp) } },\n        trailingIcon = trailingIcon,\n        singleLine = true'
content = re.sub(target_outlined, repl_outlined, content)

# 2. Update invert field
target_invert = r'val invertLabel = if \(node\.deltaCm != 0\.0\) "Sabit Akar \(Aktif: \$\{node\.invert\?\.let \{ format\(it, 2\) \}\}\)" else "Akar kotu"\n\s*CompactField\(invertLabel, node\.invertText, \{ viewModel\.updateInvert\(node\.id, it\) \}, Modifier\.fillMaxWidth\(\), suffix = "m"\)'

repl_invert = '''val invertLabel = if (node.deltaCm != 0.0) "Sabit Akar (Aktif: ${node.invert?.let { format(it, 2) }})" else if (node.isDepthMode) "Derinlik" else "Akar kotu"
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
content = re.sub(target_invert, repl_invert, content)

# 3. Add SwapVert import
if 'import androidx.compose.material.icons.filled.SwapVert' not in content:
    content = content.replace('import androidx.compose.material.icons.filled.Close', 'import androidx.compose.material.icons.filled.Close\nimport androidx.compose.material.icons.filled.SwapVert')

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Regex patched Impact UI")