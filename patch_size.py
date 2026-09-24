import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Fix size.width and size.height ambiguity
content = content.replace('size.width', 'this.size.width')
content = content.replace('size.height', 'this.size.height')

# But wait, inside Canvas, size is also a property of DrawScope (of type Size).
# 	his.size will work for both PointerInputScope and DrawScope because both have size as a property.

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Fixed size reference')