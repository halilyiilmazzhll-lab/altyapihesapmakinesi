import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

content = content.replace('.androidx.compose.ui.input.pointer.pointerInput(Unit) {', '.pointerInput(Unit) {')

import_line = 'import androidx.compose.ui.input.pointer.pointerInput\n'
if 'import androidx.compose.ui.input.pointer.pointerInput' not in content:
    content = content.replace('import androidx.compose.ui.text.drawText', 'import androidx.compose.ui.text.drawText\n' + import_line)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Fixed pointerInput')