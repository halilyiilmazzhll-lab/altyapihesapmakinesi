import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

content = content.replace('androidx.compose.foundation.gestures.detectTapGestures(onTap = { tapOffset ->', 'detectTapGestures(onTap = { tapOffset ->')
content = content.replace('androidx.compose.foundation.gestures.detectTransformGestures {', 'detectTransformGestures {')

import_lines = '''import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures'''

if 'detectTapGestures' not in content[:500]:
    content = content.replace('import androidx.compose.ui.text.drawText', 'import androidx.compose.ui.text.drawText\n' + import_lines)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Fixed imports')