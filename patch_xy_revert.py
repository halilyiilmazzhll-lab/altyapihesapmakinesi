import re

path = 'app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt'
with open(path, 'rb') as f:
    raw = f.read()
content = raw.decode('utf-8', errors='replace')

# Revert swap - restore original axis mapping
# projectX = Easting  -> horizontal (canvas X)
# projectY = Northing -> vertical   (canvas Y)

content = content.replace(
    'val minX = remember(validRecords) { validRecords.minOf { it.projectY!! } }',
    'val minX = remember(validRecords) { validRecords.minOf { it.projectX!! } }'
)
content = content.replace(
    'val maxX = remember(validRecords) { validRecords.maxOf { it.projectY!! } }',
    'val maxX = remember(validRecords) { validRecords.maxOf { it.projectX!! } }'
)
content = content.replace(
    'val minY = remember(validRecords) { validRecords.minOf { it.projectX!! } }',
    'val minY = remember(validRecords) { validRecords.minOf { it.projectY!! } }'
)
content = content.replace(
    'val maxY = remember(validRecords) { validRecords.maxOf { it.projectX!! } }',
    'val maxY = remember(validRecords) { validRecords.maxOf { it.projectY!! } }'
)

# Revert tap detection
content = content.replace(
    'val canvasX = drawOffsetX + ((record.projectY!! - adjMinX) * baseScale).toFloat()\n                        val canvasY = drawOffsetY + ((adjMaxY - record.projectX!!) * baseScale).toFloat()',
    'val canvasX = drawOffsetX + ((record.projectX!! - adjMinX) * baseScale).toFloat()\n                        val canvasY = drawOffsetY + ((adjMaxY - record.projectY!!) * baseScale).toFloat()'
)

# Revert worldToScreen calls
content = content.replace(
    'val pos = worldToScreen(record.projectY!!, record.projectX!!)',
    'val pos = worldToScreen(record.projectX!!, record.projectY!!)'
)
content = content.replace(
    'val p1 = worldToScreen(record.projectY!!, record.projectX!!)',
    'val p1 = worldToScreen(record.projectX!!, record.projectY!!)'
)
content = content.replace(
    'val p2 = worldToScreen(target.projectY!!, target.projectX!!)',
    'val p2 = worldToScreen(target.projectX!!, target.projectY!!)'
)

with open(path, 'wb') as f:
    f.write(content.encode('utf-8'))
print("Reverted XY swap - back to original axes")