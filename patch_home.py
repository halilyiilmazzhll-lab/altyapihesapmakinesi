import sys

path = "c:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/ui/screens/HomeScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Add to the function signature
content = content.replace("    onNavigateElevationTransfer: () -> Unit,\n    onNavigateSettings: () -> Unit\n) {", "    onNavigateElevationTransfer: () -> Unit,\n    onNavigateSepticTank: () -> Unit,\n    onNavigateSettings: () -> Unit\n) {")

# Add new item to LazyVerticalGrid
item_code = """
            item {
                HomeGridCard(
                    title = "Foseptik",
                    subtitle = "Foseptik hesapla ve görselleştir",
                    icon = Icons.Default.WaterDamage,
                    onClick = onNavigateSepticTank
                )
            }
"""
content = content.replace("        }\n    }\n}", item_code.lstrip("\n") + "        }\n    }\n}")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
