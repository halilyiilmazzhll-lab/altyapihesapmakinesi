import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/StakeoutScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = '''            0 -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StakeoutField.entries) { field ->'''

repl = '''            0 -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    val rowChoices = (0 until minOf(20, sheet.rows.size)).map { i -> 
                        i.toLong() to "${i + 1}. Satır: " + sheet.rows[i].take(3).joinToString(" | ") { it.take(15) }
                    }
                    StakeoutChoice(
                        label = "Veri Başlangıç Satırı",
                        value = rowChoices.find { it.first == state.dataStartRow.toLong() }?.second ?: "${state.dataStartRow + 1}. Satır",
                        choices = rowChoices,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.setDataStartRow(it.toInt())
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(StakeoutField.entries) { field ->'''

content = content.replace(target, repl)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Updated StakeoutScreen with dataStartRow")