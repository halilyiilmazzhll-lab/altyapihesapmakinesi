import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/InterpolationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Add headerAction to CompactInput definition
target = r'keyboardActions: KeyboardActions\n\) \{\n    Column\(modifier = Modifier.fillMaxWidth\(\)\) \{\n        Text\(\n            text = label,\n            fontSize = 10.sp,\n            fontWeight = FontWeight.SemiBold,\n            color = TextSecondary.copy\(alpha = 0.7f\),\n            modifier = Modifier.padding\(start = 4.dp, bottom = 2.dp\)\n        \)'
repl = '''keyboardActions: KeyboardActions,
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
content = re.sub(target, repl, content)

# Add headerAction to the Kot 1 call
content = re.sub(r'(CompactInput\(\s*value = state\.kot1,[\s\S]*?label = "Ba[^"]+ Kotu",)',
                 r'\1\n                                    headerAction = { com.example.egimhesabi.ui.components.StakeoutPicker("Başlangıç", state.startSource, viewModel::importStart, fields = com.example.egimhesabi.ui.components.StakeoutTransferFields.INVERT) },',
                 content)

# Add headerAction to the Kot 2 call
content = re.sub(r'(CompactInput\(\s*value = state\.kot2,[\s\S]*?label = "Bitiş Kotu",)',
                 r'\1\n                                    headerAction = { com.example.egimhesabi.ui.components.StakeoutPicker("Bitiş", state.endSource, viewModel::importEnd, fields = com.example.egimhesabi.ui.components.StakeoutTransferFields.INVERT) },',
                 content)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Regex Patched InterpolationScreen")