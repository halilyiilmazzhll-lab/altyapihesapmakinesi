import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Replace Box with BoxWithConstraints
target_box = 'Box(modifier = Modifier.fillMaxSize()'
replacement_box = 'androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()'
content = content.replace(target_box, replacement_box, 1)

# Remove the broken variables inside pointerInput
target_vars = '''                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    val cw = this.size.width
                                    val ch = this.size.height
                                    detectTapGestures(onTap = { tapOffset ->'''
replacement_vars = '''                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    detectTapGestures(onTap = { tapOffset ->
                                        val cw = constraints.maxWidth
                                        val ch = constraints.maxHeight'''
content = content.replace(target_vars, replacement_vars)

# Fix detectTransformGestures which might also need it if I put one there?
# Actually, detectTransformGestures is its own block, but it doesn't use cw or ch.
# The Canvas is inside BoxWithConstraints. It uses size.width, which works because DrawScope.size is fine.

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Used BoxWithConstraints')