import codecs

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with open(path, 'rb') as f:
    raw = f.read()
content = raw.decode('utf-8', errors='replace')

# 1. Add Icons.Default.Map import if not present
if "import androidx.compose.material.icons.filled.Map" not in content:
    content = content.replace(
        "import androidx.compose.material.icons.filled.Close",
        "import androidx.compose.material.icons.filled.Close\nimport androidx.compose.material.icons.filled.Map"
    )

with open(path, 'wb') as f:
    f.write(content.encode('utf-8'))
print("Added Map import")