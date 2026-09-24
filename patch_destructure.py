import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = '''                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    val compSize = this.size'''

replacement = '''                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    val cw = this.size.width
                                    val ch = this.size.height'''

content = content.replace(target, replacement)
content = content.replace('compSize.width', 'cw')
content = content.replace('compSize.height', 'ch')

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Used cw and ch')