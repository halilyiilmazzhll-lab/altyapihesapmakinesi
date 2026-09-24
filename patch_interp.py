import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/InterpolationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

content = re.sub(r'\s*com\.example\.egimhesabi\.ui\.components\.StakeoutPicker\("Başlangıç".*?\n', '\n', content)
content = re.sub(r'\s*com\.example\.egimhesabi\.ui\.components\.StakeoutPicker\("Bitiş".*?\n', '\n', content)

target = '''                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Başlangıç Kotu",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                    )
                                }'''
replacement = '''                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Başlangıç Kotu",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    com.example.egimhesabi.ui.components.StakeoutPicker("Başlangıç", state.startSource, viewModel::importStart, fields = com.example.egimhesabi.ui.components.StakeoutTransferFields.INVERT)
                                }'''
content = content.replace(target, replacement)


target2 = '''                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Bitiş Kotu",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                    )
                                }'''
replacement2 = '''                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Bitiş Kotu",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    com.example.egimhesabi.ui.components.StakeoutPicker("Bitiş", state.endSource, viewModel::importEnd, fields = com.example.egimhesabi.ui.components.StakeoutTransferFields.INVERT)
                                }'''
content = content.replace(target2, replacement2)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Patched InterpolationScreen.kt')