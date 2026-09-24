import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/components/ManholeCard.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = r'letterSpacing = 0\.5\.sp\s*\)\s*\}'
repl = '''letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                if (headerAction != null) {
                    headerAction()
                }
            }'''
content = re.sub(target, repl, content)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Regex patched ManholeCard UI")