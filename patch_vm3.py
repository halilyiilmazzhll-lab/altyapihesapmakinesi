import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/viewmodel/StakeoutViewModel.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target1 = '''data class StakeoutImportState(
    val neighborhoodId: Long,
    val fileName: String,
    val workbook: StakeoutWorkbookData,
    val sheetIndex: Int,
    val mapping: Map<StakeoutField, Int?>,
    val preview: StakeoutPreview
)'''
repl1 = '''data class StakeoutImportState(
    val neighborhoodId: Long,
    val fileName: String,
    val workbook: StakeoutWorkbookData,
    val sheetIndex: Int,
    val mapping: Map<StakeoutField, Int?>,
    val dataStartRow: Int = 3,
    val preview: StakeoutPreview
)'''
content = content.replace(target1, repl1)

target2 = '''            val preview = StakeoutWorkbook.preview(sheet, mapping)
            _importState.value = state.copy(mapping = mapping, preview = preview)'''
repl2 = '''            val preview = StakeoutWorkbook.preview(sheet, mapping, state.dataStartRow)
            _importState.value = state.copy(mapping = mapping, preview = preview)'''
content = content.replace(target2, repl2)

target3 = '''        val preview = StakeoutWorkbook.preview(sheet, mapping)
        _importState.value = StakeoutImportState(
            neighborhoodId = target,
            fileName = fileName,
            workbook = workbook,
            sheetIndex = 0,
            mapping = mapping,
            preview = preview
        )'''
repl3 = '''        val preview = StakeoutWorkbook.preview(sheet, mapping, 3)
        _importState.value = StakeoutImportState(
            neighborhoodId = target,
            fileName = fileName,
            workbook = workbook,
            sheetIndex = 0,
            mapping = mapping,
            dataStartRow = 3,
            preview = preview
        )'''
content = content.replace(target3, repl3)

target4 = '''            val preview = StakeoutWorkbook.preview(sheet, mapping)
            _importState.value = state.copy(sheetIndex = index, mapping = mapping, preview = preview)'''
repl4 = '''            val preview = StakeoutWorkbook.preview(sheet, mapping, state.dataStartRow)
            _importState.value = state.copy(sheetIndex = index, mapping = mapping, preview = preview)'''
content = content.replace(target4, repl4)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Updated StakeoutViewModel")