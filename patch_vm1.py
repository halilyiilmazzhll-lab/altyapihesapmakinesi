import codecs

path = 'app/src/main/java/com/example/egimhesabi/viewmodel/SlopeViewModel.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

content = content.replace('source.qualifiedName', 'source.manholeName')

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Updated SlopeViewModel")