import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = '''                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 10f)
                                        offset += pan
                                    }'''
repl = '''                                    detectTransformGestures { centroid, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(0.2f, 50f)
                                        val actualZoom = newScale / scale
                                        offset = (offset + pan - centroid) * actualZoom + centroid
                                        scale = newScale
                                    }'''
content = content.replace(target, repl)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Patched ImpactCalculationScreen zoom")