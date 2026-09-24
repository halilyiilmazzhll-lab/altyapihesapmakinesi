import codecs
import re

path = 'app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = r'''                val textLayoutResult = textMeasurer.measure\(
                    text = record.name,
                    style = TextStyle\(
                        color = if \(isSelected \|\| isSource\) TextPrimary else TextSecondary,
                        fontSize = \(11\.sp\.toPx\(\) \* scale\.coerceAtMost\(1\.5f\)\)\.toSp\(\),
                        fontWeight = if \(isSelected \|\| isSource\) FontWeight\.Bold else FontWeight\.SemiBold
                    \)
                \)
                
                drawText\(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset\(finalX \+ 8\.dp\.toPx\(\) \* scale, finalY - textLayoutResult\.size\.height / 2f\)
                \)'''

repl = '''                val fontSize = (11.sp.toPx() * scale.coerceAtMost(1.5f)).toSp()
                val fontWeight = if (isSelected || isSource) FontWeight.Bold else FontWeight.SemiBold
                val topLeft = Offset(finalX + 8.dp.toPx() * scale, finalY - (fontSize.toPx() / 2f))
                
                // Draw Halo (White stroke)
                val haloLayoutResult = textMeasurer.measure(
                    text = record.name,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 4.dp.toPx() * scale.coerceAtMost(1.5f),
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                )
                drawText(
                    textLayoutResult = haloLayoutResult,
                    topLeft = Offset(finalX + 8.dp.toPx() * scale, finalY - haloLayoutResult.size.height / 2f)
                )
                
                // Draw actual text
                val textLayoutResult = textMeasurer.measure(
                    text = record.name,
                    style = TextStyle(
                        color = if (isSelected || isSource) TextPrimary else TextSecondary,
                        fontSize = fontSize,
                        fontWeight = fontWeight
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(finalX + 8.dp.toPx() * scale, finalY - textLayoutResult.size.height / 2f)
                )'''

content = re.sub(target, repl, content)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Updated MapView text rendering")