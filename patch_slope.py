import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/SlopeCalculatorScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Remove standalone StakeoutPicker lines
content = re.sub(r'\s*StakeoutPicker\("Baca 1".*?\n', '\n', content)
content = re.sub(r'\s*StakeoutPicker\("Baca 2".*?\n', '\n', content)

# Inject headerAction into ManholeCard 1
target_card1 = '''                    ManholeCard(
                        bacaNumber = 1,
                        bacaName = state.baca1Name,
                        kapakKotu = state.baca1KapakKotu,
                        akarKotu = state.baca1AkarKotu,
                        derinlik = state.baca1Depth,
                        isNegativeDepth = state.baca1NegativeDepth,
                        onNameChange = { viewModel.updateState { copy(baca1Name = it) } },
                        onKapakKotuChange = { viewModel.updateState { copy(baca1KapakKotu = it) } },
                        onAkarKotuChange = { viewModel.updateState { copy(baca1AkarKotu = it) } },
                        modifier = Modifier.weight(1f)
                    )'''
replacement_card1 = '''                    ManholeCard(
                        bacaNumber = 1,
                        bacaName = state.baca1Name,
                        kapakKotu = state.baca1KapakKotu,
                        akarKotu = state.baca1AkarKotu,
                        derinlik = state.baca1Depth,
                        isNegativeDepth = state.baca1NegativeDepth,
                        onNameChange = { viewModel.updateState { copy(baca1Name = it) } },
                        onKapakKotuChange = { viewModel.updateState { copy(baca1KapakKotu = it) } },
                        onAkarKotuChange = { viewModel.updateState { copy(baca1AkarKotu = it) } },
                        modifier = Modifier.weight(1f),
                        headerAction = { com.example.egimhesabi.ui.components.StakeoutPicker("Baca 1", state.baca1Source, viewModel::importBaca1) }
                    )'''
content = content.replace(target_card1, replacement_card1)

# Inject headerAction into ManholeCard 2
target_card2 = '''                    ManholeCard(
                        bacaNumber = 2,
                        bacaName = state.baca2Name,
                        kapakKotu = state.baca2KapakKotu,
                        akarKotu = state.baca2AkarKotu,
                        derinlik = state.baca2Depth,
                        isNegativeDepth = state.baca2NegativeDepth,
                        onNameChange = { viewModel.updateState { copy(baca2Name = it) } },
                        onKapakKotuChange = { viewModel.updateState { copy(baca2KapakKotu = it) } },
                        onAkarKotuChange = { viewModel.updateState { copy(baca2AkarKotu = it) } },
                        modifier = Modifier.weight(1f)
                    )'''
replacement_card2 = '''                    ManholeCard(
                        bacaNumber = 2,
                        bacaName = state.baca2Name,
                        kapakKotu = state.baca2KapakKotu,
                        akarKotu = state.baca2AkarKotu,
                        derinlik = state.baca2Depth,
                        isNegativeDepth = state.baca2NegativeDepth,
                        onNameChange = { viewModel.updateState { copy(baca2Name = it) } },
                        onKapakKotuChange = { viewModel.updateState { copy(baca2KapakKotu = it) } },
                        onAkarKotuChange = { viewModel.updateState { copy(baca2AkarKotu = it) } },
                        modifier = Modifier.weight(1f),
                        headerAction = { com.example.egimhesabi.ui.components.StakeoutPicker("Baca 2", state.baca2Source, viewModel::importBaca2) }
                    )'''
content = content.replace(target_card2, replacement_card2)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Patched SlopeCalculatorScreen.kt')