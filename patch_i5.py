import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/InterpolationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Remove the broken headerAction line
content = re.sub(r'\s*headerAction = \{ com\.example\.egimhesabi\.ui\.components\.\n', '\n', content)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Removed duplicate headerAction")