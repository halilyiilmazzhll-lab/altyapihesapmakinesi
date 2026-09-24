import codecs

path = 'app/src/main/java/com/example/egimhesabi/ui/components/ManholeCard.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = '''    onAkarKotuChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {'''
repl = '''    onAkarKotuChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    headerAction: @Composable (() -> Unit)? = null
) {'''
content = content.replace(target, repl)

target_ui = '''                Text(
                    text = if (bacaNumber == 1) "1. BACA" else "2. BACA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange,
                    letterSpacing = 0.5.sp
                )
            }'''
repl_ui = '''                Text(
                    text = if (bacaNumber == 1) "1. BACA" else "2. BACA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                if (headerAction != null) {
                    headerAction()
                }
            }'''
content = content.replace(target_ui, repl_ui)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Patched ManholeCard globally")