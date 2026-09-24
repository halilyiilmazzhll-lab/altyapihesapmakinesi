import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/domain/StakeoutCalculationSource.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = r'val label: String get\(\) ='
repl = r'val manholeName: String get() = qualifiedName.substringAfterLast(" / ")\n    val label: String get() ='
content = re.sub(target, repl, content)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Updated StakeoutCalculationSource with regex")