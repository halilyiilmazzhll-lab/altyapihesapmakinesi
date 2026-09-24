import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Revert my bad replacement
content = content.replace('this.size.width', 'size.width')
content = content.replace('this.size.height', 'size.height')

# Now selectively fix the PointerInputScope size accesses
# They look like:
# val scaleX = size.width / (adjMaxX - adjMinX).toFloat()
# val scaleY = size.height / (adjMaxY - adjMinY).toFloat()
# val drawOffsetX = (size.width - drawWidth) / 2f
# val drawOffsetY = (size.height - drawHeight) / 2f

content = content.replace('val scaleX = size.width', 'val scaleX = this.size.width')
content = content.replace('val scaleY = size.height', 'val scaleY = this.size.height')
content = content.replace('val drawOffsetX = (size.width', 'val drawOffsetX = (this.size.width')
content = content.replace('val drawOffsetY = (size.height', 'val drawOffsetY = (this.size.height')

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Fixed broad replace')