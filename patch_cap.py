import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = '''                        androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()
                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    androidx.compose.foundation.gestures.detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 10f)
                                        offset += pan
                                    }
                                }
                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    detectTapGestures(onTap = { tapOffset ->
                                        val cw = constraints.maxWidth
                                        val ch = constraints.maxHeight'''

replacement = '''                        androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val cw = constraints.maxWidth
                            val ch = constraints.maxHeight
                            Box(modifier = Modifier.fillMaxSize()
                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    androidx.compose.foundation.gestures.detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 10f)
                                        offset += pan
                                    }
                                }
                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    detectTapGestures(onTap = { tapOffset ->'''
content = content.replace(target, replacement)
content = content.replace('                    } // BoxWithConstraints', '                    }\n                    } // BoxWithConstraints')
# Let's fix the closing brace manually.
# Since I replaced Box(...) with BoxWithConstraints(...) before, let's just make it BoxWithConstraints { val cw = constraints.maxWidth; val ch = constraints.maxHeight; Box(...) { Canvas(...) } }

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Fixed constraints capture')