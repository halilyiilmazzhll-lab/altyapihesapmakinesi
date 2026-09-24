import codecs

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/StakeoutScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

content = content.replace('choices = rowChoices,', 'options = rowChoices,')

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Fixed options in StakeoutChoice")