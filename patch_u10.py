import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = r'node\.depth\?\.let \{\s*Text\("Derinlik \$\{format\(it, 2\)\}m", color = TextSecondary, fontSize = 9\.sp, fontFamily = FontFamily\.Monospace\)\s*\}'
repl = '''node.depth?.let {
                        Text("Derinlik m", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    com.example.egimhesabi.ui.components.StakeoutPicker(
                        targetLabel = "Baca ",
                        source = node.stakeoutSource,
                        onSelect = { viewModel.importManhole(node.id, it) }
                    )'''
content = re.sub(target, repl, content)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Regex patched StakeoutPicker into Impact")