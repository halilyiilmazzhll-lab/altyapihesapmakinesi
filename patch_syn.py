import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

target = '''                                        if (closest != null) {
                                            if (selectedChain.lastOrNull()?.manhole?.id == closest.manhole.id) {
                                                selectedChain.removeLast()
                                            } else if (selectedChain.any { it.manhole.id == closest.manhole.id }) {
                                                // Already in chain but not last, ignore or remove? Let's ignore for safety.
                                            } else {
                                                selectedChain.add(closest)
                                            }
                                        }
                                    }
                                }'''

replacement = '''                                        if (closest != null) {
                                            if (selectedChain.lastOrNull()?.manhole?.id == closest.manhole.id) {
                                                selectedChain.removeLast()
                                            } else if (selectedChain.any { it.manhole.id == closest.manhole.id }) {
                                                // Already in chain but not last, ignore or remove? Let's ignore for safety.
                                            } else {
                                                selectedChain.add(closest)
                                            }
                                        }
                                    })
                                }'''
content = content.replace(target, replacement)
with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Fixed syntax error')