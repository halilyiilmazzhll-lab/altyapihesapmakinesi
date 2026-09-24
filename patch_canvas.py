import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Fix the Canvas part only
target = '''                                val scaleX = compSize.width / (adjMaxX - adjMinX).toFloat()
                                val scaleY = compSize.height / (adjMaxY - adjMinY).toFloat()
                                val baseScale = kotlin.math.min(scaleX, scaleY)
                                
                                val drawOffsetX = (compSize.width - drawWidth) / 2f
                                val drawOffsetY = (compSize.height - drawHeight) / 2f'''

replacement = '''                                val scaleX = size.width / (adjMaxX - adjMinX).toFloat()
                                val scaleY = size.height / (adjMaxY - adjMinY).toFloat()
                                val baseScale = kotlin.math.min(scaleX, scaleY)
                                
                                val drawOffsetX = (size.width - (adjMaxX - adjMinX).toFloat() * baseScale) / 2f
                                val drawOffsetY = (size.height - (adjMaxY - adjMinY).toFloat() * baseScale) / 2f'''

content = content.replace(target, replacement)
with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Fixed canvas sizes')