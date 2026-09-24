import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/screens/StakeoutScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target_state = '    var currentFilter by rememberSaveable(neighborhoodId) { mutableStateOf(StakeoutFilter.ALL) }'
replacement_state = target_state + '\n    var isMapView by rememberSaveable(neighborhoodId) { mutableStateOf(false) }'

if target_state in content:
    content = content.replace(target_state, replacement_state, 1)

target_filters = '''                                StakeoutFilterButton("Eksik Veri", currentFilter == StakeoutFilter.MISSING_DATA) { currentFilter = StakeoutFilter.MISSING_DATA }
                            }
                        }
                        
                        val matching'''

replacement_filters = '''                                StakeoutFilterButton("Eksik Veri", currentFilter == StakeoutFilter.MISSING_DATA) { currentFilter = StakeoutFilter.MISSING_DATA }
                                StakeoutFilterButton("Harita", isMapView) { isMapView = not isMapView }
                            }
                        }
                        
                        val matching'''.replace('not', '!')

if target_filters in content:
    content = content.replace(target_filters, replacement_filters, 1)
else:
    print('target_filters not found')

target_list = '''                        if (matching.isEmpty()) item {
                            Text("Seçili filtrelere uygun baca bulunamadı.", color = TextSecondary, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp))
                        }
                        items(matching, key = { it.id }) { record ->
                            StakeoutRecordCard(record, busy, onEdit = { editRecord = record }, onDelete = { deleteRecord = record })
                        }'''

replacement_list = '''                        if (isMapView) {
                            item {
                                com.example.egimhesabi.ui.components.StakeoutMapView(records = matching, modifier = Modifier.padding(top = 8.dp))
                            }
                        } else {
                            if (matching.isEmpty()) item {
                                Text("Seçili filtrelere uygun baca bulunamadı.", color = TextSecondary, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp))
                            }
                            items(matching, key = { it.id }) { record ->
                                StakeoutRecordCard(record, busy, onEdit = { editRecord = record }, onDelete = { deleteRecord = record })
                            }
                        }'''

if target_list in content:
    content = content.replace(target_list, replacement_list, 1)
else:
    print('target_list not found')

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Successfully patched StakeoutScreen.kt')