import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/InterpolationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# 1. Modify CompactInput
target_input = '''private fun CompactInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    suffix: String? = null,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )'''
repl_input = '''private fun CompactInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    suffix: String? = null,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    headerAction: @Composable (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            if (headerAction != null) {
                headerAction()
            }
        }'''
content = content.replace(target_input, repl_input)

# 2. Add headerAction to Kot 1
target_kot1 = '''                                CompactInput(
                                    value = state.kot1,
                                    onValueChange = { viewModel.updateKot1(it) },
                                    label = "Başlangıç Kotu",'''
repl_kot1 = '''                                CompactInput(
                                    value = state.kot1,
                                    onValueChange = { viewModel.updateKot1(it) },
                                    label = "Başlangıç Kotu",
                                    headerAction = { com.example.egimhesabi.ui.components.StakeoutPicker("Başlangıç", state.startSource, viewModel::importStart, fields = com.example.egimhesabi.ui.components.StakeoutTransferFields.INVERT) },'''
# Wait, let's use regex in case encoding makes "Başlangıç Kotu" mismatch
content = re.sub(r'CompactInput\(\s*value = state\.kot1,\s*onValueChange = \{ viewModel\.updateKot1\(it\) \},\s*label = "[^"]+",',
                 r'CompactInput(\n                                    value = state.kot1,\n                                    onValueChange = { viewModel.updateKot1(it) },\n                                    label = "Başlangıç Kotu",\n                                    headerAction = { com.example.egimhesabi.ui.components.StakeoutPicker("Başlangıç", state.startSource, viewModel::importStart, fields = com.example.egimhesabi.ui.components.StakeoutTransferFields.INVERT) },',
                 content)

# 3. Add headerAction to Kot 2
content = re.sub(r'CompactInput\(\s*value = state\.kot2,\s*onValueChange = \{ viewModel\.updateKot2\(it\) \},\s*label = "[^"]+",',
                 r'CompactInput(\n                                    value = state.kot2,\n                                    onValueChange = { viewModel.updateKot2(it) },\n                                    label = "Bitiş Kotu",\n                                    headerAction = { com.example.egimhesabi.ui.components.StakeoutPicker("Bitiş", state.endSource, viewModel::importEnd, fields = com.example.egimhesabi.ui.components.StakeoutTransferFields.INVERT) },',
                 content)

# 4. Remove standalone StakeoutPicker lines
content = re.sub(r'\s*StakeoutPicker\("Ba[^"]+", state\.startSource.*?\n', '\n', content)
content = re.sub(r'\s*StakeoutPicker\("Biti[^"]+", state\.endSource.*?\n', '\n', content)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Patched InterpolationScreen")