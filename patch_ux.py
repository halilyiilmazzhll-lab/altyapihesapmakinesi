import sys

path = "c:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/ui/screens/FoseptikScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace the INPUTS section with a more grouped and explanatory version.
inputs_start = content.find("// INPUTS")
inputs_end = content.find("// RESULTS")

new_inputs = """// INPUTS
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Hesaplama Değerleri", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 18.sp, 
                            color = TextPrimary
                        )
                        
                        Text(
                            "Bu sayfada, mevcut bir bacadan foseptiğe gidecek hattın eğimini ve foseptik kuyusunun derinlik/kot hesaplarını yapabilirsiniz. Değerleri girdikçe çizim otomatik güncellenir.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )

                        Divider(color = Color.LightGray.copy(alpha = 0.5f))

                        // GRUP 1: Başlangıç
                        Text("1. Başlangıç Noktası (Baca)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AccentOrange)
                        OutlinedTextField(
                            value = manholeInvertStr,
                            onValueChange = { manholeInvertStr = it },
                            label = { Text("Bacanın Akar Kotu (Başlangıç)") },
                            placeholder = { Text("Örn: 100.50") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = glassFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider(color = Color.LightGray.copy(alpha = 0.5f))

                        // GRUP 2: Hat (Mesafe ve Eğim)
                        Text("2. Hat Bilgileri (Mesafe ve Eğim)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AccentOrange)
                        OutlinedTextField(
                            value = distanceStr,
                            onValueChange = { distanceStr = it },
                            label = { Text("Baca ile Foseptik Arası Mesafe (m)") },
                            placeholder = { Text("Örn: 25.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = glassFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Text("Eğim Tipi Seçimi:", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = slopeType == 0, onClick = { slopeType = 0 }, colors = RadioButtonDefaults.colors(selectedColor = AccentOrange))
                                    Text("1/x", fontSize = 14.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = slopeType == 1, onClick = { slopeType = 1 }, colors = RadioButtonDefaults.colors(selectedColor = AccentOrange))
                                    Text("%", fontSize = 14.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = slopeType == 2, onClick = { slopeType = 2 }, colors = RadioButtonDefaults.colors(selectedColor = AccentOrange))
                                    Text("cm/m", fontSize = 14.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = slopeStr,
                            onValueChange = { slopeStr = it },
                            label = { Text("Eğim Değeri") },
                            placeholder = { Text("Seçilen tipe göre eğim girin") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = glassFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider(color = Color.LightGray.copy(alpha = 0.5f))

                        // GRUP 3: Foseptik Özellikleri
                        Text("3. Foseptik (Kuyu) Bilgileri", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AccentOrange)
                        
                        OutlinedTextField(
                            value = tankHeightStr,
                            onValueChange = { tankHeightStr = it },
                            label = { Text("Foseptik Toplam Boyu (m)") },
                            placeholder = { Text("Kuyunun iç yüksekliği") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = glassFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = coverToInletStr,
                            onValueChange = { coverToInletStr = it },
                            label = { Text("Kapak ile Akar Arası Mesafe (m)") },
                            placeholder = { Text("Borunun girdiği noktanın kapağa uzaklığı") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = glassFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = groundElevStr,
                            onValueChange = { groundElevStr = it },
                            label = { Text("Zemin Kotu (Opsiyonel)") },
                            placeholder = { Text("Kazı hesabı için foseptiğin kurulacağı zemin kotu") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = glassFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                """

# Replace RESULTS section with clearer text
results_start = content.find("// RESULTS")
results_end = content.find("} else {\n                    Spacer(modifier = Modifier.height(24.dp))")

new_results = """// RESULTS
                if (bottomElev != null) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        cornerRadius = 24.dp,
                        backgroundAlpha = 1f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AccentOrange.copy(alpha = 0.1f))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Sonuçlar", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AccentOrange)
                            
                            inletElev?.let {
                                Text("• Foseptik Giriş (Akar) Kotu: ${NumberParser.formatDecimal(it)} m", color = TextPrimary)
                            }
                            coverElev?.let {
                                Text("• Foseptik Kapak Kotu: ${NumberParser.formatDecimal(it)} m", color = TextPrimary)
                            }
                            Text("• Foseptik Taban Kotu (Kazı Alt Noktası): ${NumberParser.formatDecimal(bottomElev)} m", fontWeight = FontWeight.Bold, color = TextPrimary)
                            
                            excavationDepth?.let {
                                Text("• Kazı Derinliği (Zeminden Tabana): ${NumberParser.formatDecimal(it)} m", fontWeight = FontWeight.Medium, color = Color(0xFFD97706)) // slightly darker orange
                            }
                        }
                    }
                """

final_content = content[:inputs_start] + new_inputs + new_results + content[results_end:]

with open(path, "w", encoding="utf-8") as f:
    f.write(final_content)
