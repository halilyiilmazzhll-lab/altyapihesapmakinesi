import codecs

path = 'app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt'
with open(path, 'rb') as f:
    raw = f.read()
content = raw.decode('utf-8', errors='replace')

# Fix 1: Show details only at very high zoom
content = content.replace('val showDetails = scale > 2.5f', 'val showDetails = scale > 5f')

# Fix 2: Fixed font size (not scaling with zoom)
content = content.replace(
    'val nameFontPx = (11f.sp.toPx() * scale.coerceAtMost(1.8f))',
    'val nameFontPx = 11f.sp.toPx()'
)
content = content.replace(
    'val kotFontPx = (8.5f.sp.toPx() * scale.coerceAtMost(1.6f))',
    'val kotFontPx = 8.5f.sp.toPx()'
)

# Fix 3: Fix halo width
content = content.replace(
    'width = (3.5f * scale).coerceIn(2f, 6f)',
    'width = 3.5f'
)

# Fix 4: Fix label offset
content = content.replace(
    'val lx = pos.x + dotRadius + (6f * scale).coerceAtMost(12f)',
    'val lx = pos.x + dotRadius + 6f'
)

with open(path, 'wb') as f:
    f.write(content.encode('utf-8'))
print("Fixed canvas text scaling")