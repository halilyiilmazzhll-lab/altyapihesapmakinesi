import codecs

path = 'app/src/main/java/com/example/egimhesabi/ui/components/ManholeCard.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target1 = '''    onAkarKotuChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {'''
replacement1 = '''    onAkarKotuChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    headerAction: @Composable () -> Unit = {}
) {'''
content = content.replace(target1, replacement1, 1)

target2 = '''                Text(
                    text = if (bacaNumber == 1) "1. BACA" else "2. BACA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange,
                    letterSpacing = 0.5.sp
                )
            }'''
replacement2 = '''                Text(
                    text = if (bacaNumber == 1) "1. BACA" else "2. BACA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                headerAction()
            }'''
content = content.replace(target2, replacement2, 1)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Patched ManholeCard.kt')