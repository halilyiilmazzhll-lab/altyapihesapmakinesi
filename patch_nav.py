import sys

path = "c:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/Navigation.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Add SepticTank to HomeScreen call
content = content.replace("onNavigateSettings = { navigateTo(Settings) }", "onNavigateSepticTank = { navigateTo(SepticTank) },\n                    onNavigateSettings = { navigateTo(Settings) }")

# Add entry<SepticTank> before entry<Settings>
entry_code = """
            entry<SepticTank> {
                FoseptikScreen(
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
"""
content = content.replace("            entry<Settings> {", entry_code.lstrip("\n") + "            entry<Settings> {")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
