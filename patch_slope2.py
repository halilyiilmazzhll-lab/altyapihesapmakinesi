import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/SlopeCalculatorScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Add headerAction to ManholeCard 1
target1 = '''                        onAkarKotuChange = { viewModel.updateBaca1AkarKotu(it) },
                        modifier = Modifier.weight(1f)
                    )'''
repl1 = '''                        onAkarKotuChange = { viewModel.updateBaca1AkarKotu(it) },
                        modifier = Modifier.weight(1f),
                        headerAction = { com.example.egimhesabi.ui.components.StakeoutPicker("Baca 1", state.baca1Source, viewModel::importBaca1) }
                    )'''
content = content.replace(target1, repl1)

# Add headerAction to ManholeCard 2
target2 = '''                        onAkarKotuChange = { viewModel.updateBaca2AkarKotu(it) },
                        modifier = Modifier.weight(1f)
                    )'''
repl2 = '''                        onAkarKotuChange = { viewModel.updateBaca2AkarKotu(it) },
                        modifier = Modifier.weight(1f),
                        headerAction = { com.example.egimhesabi.ui.components.StakeoutPicker("Baca 2", state.baca2Source, viewModel::importBaca2) }
                    )'''
content = content.replace(target2, repl2)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Patched SlopeCalculatorScreen")