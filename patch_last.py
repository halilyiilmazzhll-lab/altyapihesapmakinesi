import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Replace any lingering this.size.width/height or size.width/height inside PointerInputScope.
# Let's just fix the variables directly.
target_lines = '''                                        val scaleX = size.width / (adjMaxX - adjMinX).toFloat()
                                        val scaleY = size.height / (adjMaxY - adjMinY).toFloat()
                                        val baseScale = kotlin.math.min(scaleX, scaleY)
                                        
                                        val drawWidth = (adjMaxX - adjMinX).toFloat() * baseScale
                                        val drawHeight = (adjMaxY - adjMinY).toFloat() * baseScale
                                        val drawOffsetX = (size.width - drawWidth) / 2f
                                        val drawOffsetY = (size.height - drawHeight) / 2f'''
# It might have this.size.width or compSize.width. Let's just do regex

import re
content = re.sub(r'val scaleX = .*', 'val scaleX = compSize.width / (adjMaxX - adjMinX).toFloat()', content)
content = re.sub(r'val scaleY = .*', 'val scaleY = compSize.height / (adjMaxY - adjMinY).toFloat()', content)
content = re.sub(r'val drawOffsetX = .*\(size\.width.*', 'val drawOffsetX = (compSize.width - drawWidth) / 2f', content)
content = re.sub(r'val drawOffsetY = .*\(size\.height.*', 'val drawOffsetY = (compSize.height - drawHeight) / 2f', content)
content = re.sub(r'val drawOffsetX = .*\(this\.size\.width.*', 'val drawOffsetX = (compSize.width - drawWidth) / 2f', content)
content = re.sub(r'val drawOffsetY = .*\(this\.size\.height.*', 'val drawOffsetY = (compSize.height - drawHeight) / 2f', content)


with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Fixed lines')