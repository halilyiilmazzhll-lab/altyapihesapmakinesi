import codecs

path = 'app/src/main/java/com/example/egimhesabi/viewmodel/ImpactCalculationViewModel.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = 'newNodes.add(ManholeNode(coverText = lastNode?.coverText ?: "", invertText = lastNode?.invertText ?: "", distanceToNextText = ""))'
replacement = 'newNodes.add(ManholeNode(coverText = "", invertText = "", distanceToNextText = ""))'

if target in content:
    content = content.replace(target, replacement)
    with codecs.open(path, 'w', 'utf-8') as f:
        f.write(content)
    print("Fixed addManhole")
else:
    print("Target not found in ImpactCalculationViewModel.kt")