import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/viewmodel/ImpactCalculationViewModel.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

content = content.replace('parseDouble', 'parseDecimal')

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Fixed parseDecimal')