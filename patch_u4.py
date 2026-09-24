import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

pattern = re.compile(r'            StakeoutPicker\(\s*targetLabel = "Baca \$\{index \+ 1\}",\s*source = node\.stakeoutSource,\s*onSelect = \{ viewModel\.importManhole\(node\.id, it\) \}\s*\)\n\s*')
content = pattern.sub('', content)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Regex removed picker')