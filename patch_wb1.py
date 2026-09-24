import codecs

path = 'app/src/main/java/com/example/egimhesabi/util/StakeoutWorkbook.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target1 = '''    fun preview(sheet: StakeoutSheet, mapping: Map<StakeoutField, Int?>): StakeoutPreview {'''
repl1 = '''    fun preview(sheet: StakeoutSheet, mapping: Map<StakeoutField, Int?>, dataStartRow: Int = 3): StakeoutPreview {'''
content = content.replace(target1, repl1)

target2 = '''        sheet.rows.drop(HEADER_ROW + 1).forEachIndexed { offset, cells ->
            val excelRow = offset + HEADER_ROW + 2'''
repl2 = '''        sheet.rows.drop(dataStartRow).forEachIndexed { offset, cells ->
            val excelRow = offset + dataStartRow + 1'''
content = content.replace(target2, repl2)

target3 = '''        if (rows.isEmpty()) issues += "4. satırdan itibaren aktarılabilecek baca kaydı bulunamadı."'''
repl3 = '''        if (rows.isEmpty()) issues += "${dataStartRow + 1}. satırdan itibaren aktarılabilecek baca kaydı bulunamadı."'''
content = content.replace(target3, repl3)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Updated StakeoutWorkbook")