import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

# Fix threshold
content = content.replace('var scale by remember { mutableStateOf(1f) }', 'val density = androidx.compose.ui.platform.LocalDensity.current\n                        val thresholdPx = with(density) { 40.dp.toPx() }\n                        var scale by remember { mutableStateOf(1f) }')
content = content.replace('val threshold = 40.dp.toPx()', 'val threshold = thresholdPx')

# Fix detectTapGestures
content = content.replace('androidx.compose.foundation.gestures.detectTapGestures { tapOffset ->', 'androidx.compose.foundation.gestures.detectTapGestures(onTap = { tapOffset ->')

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Fixed threshold and tapOffset')