import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Fix hypot
target_dist = 'val dist = kotlin.math.hypot(finalX - tapOffset.x, finalY - tapOffset.y)'
replacement_dist = 'val dist = kotlin.math.hypot((finalX - tapOffset.x).toDouble(), (finalY - tapOffset.y).toDouble()).toFloat()'
content = content.replace(target_dist, replacement_dist)

# Fix drawText call
target_drawtext = 'androidx.compose.ui.text.drawText(textLayout, topLeft = Offset(finalX + 10.dp.toPx() * scale, finalY - textLayout.size.height / 2f))'
replacement_drawtext = 'drawText(textLayout, topLeft = Offset(finalX + 10.dp.toPx() * scale, finalY - textLayout.size.height / 2f))'
content = content.replace(target_drawtext, replacement_drawtext)

# Add import
import_line = 'import androidx.compose.ui.text.drawText'
if import_line not in content:
    content = content.replace('import androidx.compose.ui.text.font.FontFamily', 'import androidx.compose.ui.text.font.FontFamily\n' + import_line)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Patched compile errors')