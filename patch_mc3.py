import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/components/ManholeCard.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Add headerAction to parameters
content = re.sub(r'onAkarKotuChange: \(String\) -> Unit,\n\s*modifier: Modifier = Modifier\n\)',
                 r'onAkarKotuChange: (String) -> Unit,\n    modifier: Modifier = Modifier,\n    headerAction: @Composable (() -> Unit)? = null\n)',
                 content)

# Add headerAction to UI
content = re.sub(r'(letterSpacing = 0\.5\.sp\n\s*\)\n\s*)\}',
                 r'\1    Spacer(modifier = Modifier.weight(1f))\n                if (headerAction != null) { headerAction() }\n            }',
                 content)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Regex patched ManholeCard globally")