import codecs

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

content = content.replace('targetLabel = "Baca "', 'targetLabel = "Baca ${index + 1}"')

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Fixed missing formatting in ImpactCalculationScreen 2")