import re

path = 'app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt'
with open(path, 'rb') as f:
    raw = f.read()
content = raw.decode('utf-8', errors='replace')

# Also fix: dot radius should not grow too big at zoom
content = content.replace(
    'val dotRadius = (6f * scale).coerceIn(4f, 18f)',
    'val dotRadius = (5f * scale).coerceIn(3f, 10f)'
)

# Connection stroke should not grow too big
content = content.replace(
    'val strokeW = (2.5f * scale).coerceIn(1.5f, 6f)',
    'val strokeW = (1.5f * scale).coerceIn(1f, 4f)'
)

# Arrow size
content = content.replace(
    'val arrowLen = (14f * scale).coerceIn(8f, 28f)',
    'val arrowLen = (10f * scale).coerceIn(6f, 18f)'
)

with open(path, 'wb') as f:
    f.write(content.encode('utf-8'))
print("Fixed dot and stroke scaling")