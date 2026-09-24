import codecs

path = 'app/src/main/java/com/example/egimhesabi/ui/components/ManholeCard.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

content = content.replace('modifier: Modifier = Modifier', 'modifier: Modifier = Modifier,\n    headerAction: @Composable (() -> Unit)? = null')

content = content.replace('letterSpacing = 0.5.sp\n                )\n            }', 'letterSpacing = 0.5.sp\n                )\n                Spacer(modifier = Modifier.weight(1f))\n                if (headerAction != null) { headerAction() }\n            }')

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Replaced substrings in ManholeCard")