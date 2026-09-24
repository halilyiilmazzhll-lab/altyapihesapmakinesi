import codecs

path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = '''                    node.depth?.let {
                        Text("Derinlik ${format(it, 2)}m", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }'''
repl = '''                    if (node.isDepthMode) {
                        node.invert?.let {
                            Text("Akar ${format(it, 2)}m", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        node.depth?.let {
                            Text("Derinlik ${format(it, 2)}m", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }'''
content = content.replace(target, repl)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print("Updated header to show Invert or Depth based on mode")