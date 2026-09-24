import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Replace Box with BoxWithConstraints and capture the pixel sizes
target_box = '''                        Box(
                            modifier = Modifier.fillMaxSize()
                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 10f)
                                        offset += pan
                                    }
                                }
                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    detectTapGestures(onTap = { tapOffset ->
                                        val cw = constraints.maxWidth
                                        val ch = constraints.maxHeight'''

replacement_box = '''                        androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val cw = constraints.maxWidth
                            val ch = constraints.maxHeight
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(0.5f, 10f)
                                            offset += pan
                                        }
                                    }
                                    .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                        detectTapGestures(onTap = { tapOffset ->'''
content = content.replace(target_box, replacement_box)

# Now find where this whole Box ends and add one more closing brace.
# BoxWithConstraints { ... Box { Canvas { ... } ... Row { ... } } } 
# Let's just find TextButton(onClick = { selectedChain.clear() }) { Text("Sıfırla", color = AccentOrange) } } } } }
target_end = '''                                    TextButton(onClick = { selectedChain.clear() }) { Text("Sıfırla", color = AccentOrange) }
                                }
                            }
                        }
                    }'''
replacement_end = '''                                    TextButton(onClick = { selectedChain.clear() }) { Text("Sıfırla", color = AccentOrange) }
                                }
                            }
                        }
                    }
                    }'''
content = content.replace(target_end, replacement_end)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Applied BoxWithConstraints')