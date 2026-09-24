import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Fix 1: Show details only at very high zoom
content = content.replace('val showDetails = scale > 2.5f', 'val showDetails = scale > 5f')

# Fix 2: Fixed font size (not scaling with zoom - only geometry scales)
content = content.replace(
    'val nameFontPx = (11f.sp.toPx() * scale.coerceAtMost(1.8f))',
    'val nameFontPx = 11f.sp.toPx()'
)
content = content.replace(
    'val kotFontPx = (8.5f.sp.toPx() * scale.coerceAtMost(1.6f))',
    'val kotFontPx = 8.5f.sp.toPx()'
)

# Fix 3: Fix halo width not scaling with text size
content = content.replace(
    'width = (3.5f * scale).coerceIn(2f, 6f)',
    'width = 3.5f'
)

# Fix 4: Fix label offset not scaling excessively
content = content.replace(
    'val lx = pos.x + dotRadius + (6f * scale).coerceAtMost(12f)',
    'val lx = pos.x + dotRadius + 6f'
)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Fixed canvas text scaling")