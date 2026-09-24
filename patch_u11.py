import codecs

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

content = content.replace('Text("Derinlik m", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)', 'Text("Derinlik ${format(it, 2)}m", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)')

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Fixed missing formatting in ImpactCalculationScreen")