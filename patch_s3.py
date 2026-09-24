import codecs

path = 'app/src/main/java/com/example/egimhesabi/domain/StakeoutCalculationSource.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = '''    val label: String get() = "$qualifiedName – ${basis.label}"'''
repl = '''    val manholeName: String get() = qualifiedName.substringAfterLast(" / ")
    val label: String get() = "$qualifiedName – ${basis.label}"'''
content = content.replace(target, repl)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Updated StakeoutCalculationSource")