import codecs

path = 'app/src/main/java/com/example/egimhesabi/viewmodel/StakeoutViewModel.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = '''    fun mapColumn(field: StakeoutField, columnIndex: Int?) {'''
repl = '''    fun setDataStartRow(rowIndex: Int) {
        val state = _importState.value ?: return
        if (state.dataStartRow == rowIndex) return
        viewModelScope.launch(Dispatchers.Default) {
            val sheet = state.workbook.sheets[state.sheetIndex]
            val preview = StakeoutWorkbook.preview(sheet, state.mapping, rowIndex)
            _importState.value = state.copy(dataStartRow = rowIndex, preview = preview)
        }
    }

    fun mapColumn(field: StakeoutField, columnIndex: Int?) {'''
content = content.replace(target, repl)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Added setDataStartRow to StakeoutViewModel")