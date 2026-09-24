import codecs

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/InterpolationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read().replace('\r\n', '\n')

target = '''private fun CompactInput(
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
repl = '''private fun CompactInput(
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
content = content.replace(target, repl)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Patched CompactInput signature")