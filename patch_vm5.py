import codecs

path = 'app/src/main/java/com/example/egimhesabi/viewmodel/StakeoutViewModel.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = 'StakeoutImportState(targetNeighborhoodId, name, workbook, 0, mapping, StakeoutWorkbook.preview(sheet, mapping))'
repl = 'StakeoutImportState(targetNeighborhoodId, name, workbook, 0, mapping, 3, StakeoutWorkbook.preview(sheet, mapping, 3))'
content = content.replace(target, repl)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Fixed loadWorkbook in StakeoutViewModel")