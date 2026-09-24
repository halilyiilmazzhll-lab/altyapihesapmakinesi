import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = '''                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    detectTapGestures(onTap = { tapOffset ->
                                        val minX = validRecords.minOf { it.manhole.projectX!! }'''

replacement = '''                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    val compSize = this.size
                                    detectTapGestures(onTap = { tapOffset ->
                                        val minX = validRecords.minOf { it.manhole.projectX!! }'''
content = content.replace(target, replacement)

content = content.replace('val scaleX = size.width', 'val scaleX = compSize.width')
content = content.replace('val scaleY = size.height', 'val scaleY = compSize.height')
content = content.replace('val drawOffsetX = (size.width', 'val drawOffsetX = (compSize.width')
content = content.replace('val drawOffsetY = (size.height', 'val drawOffsetY = (compSize.height')

# Make sure we didn't accidentally break Canvas size
# The Canvas size is accessed as size.width in Canvas. Let's make sure Canvas still uses size.width

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Fixed compSize capture')