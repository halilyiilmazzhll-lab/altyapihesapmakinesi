import re

path = 'app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt'
with open(path, 'rb') as f:
    raw = f.read()
content = raw.decode('utf-8', errors='replace')

# Swap X and Y axes:
# projectX = Northing (kuzey) -> vertical (canvas Y)
# projectY = Easting  (dogu)  -> horizontal (canvas X)

# Fix world bounds: use projectY for horizontal, projectX for vertical
content = content.replace(
    'val minX = remember(validRecords) { validRecords.minOf { it.projectX!! } }',
    'val minX = remember(validRecords) { validRecords.minOf { it.projectY!! } }'
)
content = content.replace(
    'val maxX = remember(validRecords) { validRecords.maxOf { it.projectX!! } }',
    'val maxX = remember(validRecords) { validRecords.maxOf { it.projectY!! } }'
)
content = content.replace(
    'val minY = remember(validRecords) { validRecords.minOf { it.projectY!! } }',
    'val minY = remember(validRecords) { validRecords.minOf { it.projectX!! } }'
)
content = content.replace(
    'val maxY = remember(validRecords) { validRecords.maxOf { it.projectY!! } }',
    'val maxY = remember(validRecords) { validRecords.maxOf { it.projectX!! } }'
)

# Fix tap detection: swap projectX and projectY
content = content.replace(
    'val canvasX = drawOffsetX + ((record.projectX!! - adjMinX) * baseScale).toFloat()\n                        val canvasY = drawOffsetY + ((adjMaxY - record.projectY!!) * baseScale).toFloat()',
    'val canvasX = drawOffsetX + ((record.projectY!! - adjMinX) * baseScale).toFloat()\n                        val canvasY = drawOffsetY + ((adjMaxY - record.projectX!!) * baseScale).toFloat()'
)

# Fix worldToScreen call: swap projectX and projectY
content = content.replace(
    'val pos = worldToScreen(record.projectX!!, record.projectY!!)',
    'val pos = worldToScreen(record.projectY!!, record.projectX!!)'
)

# Fix connection drawing: swap projectX and projectY
content = content.replace(
    'val p1 = worldToScreen(record.projectX!!, record.projectY!!)',
    'val p1 = worldToScreen(record.projectY!!, record.projectX!!)'
)
content = content.replace(
    'val p2 = worldToScreen(target.projectX!!, target.projectY!!)',
    'val p2 = worldToScreen(target.projectY!!, target.projectX!!)'
)

with open(path, 'wb') as f:
    f.write(content.encode('utf-8'))
print("Swapped X/Y axes in StakeoutMapView")