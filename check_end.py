with open('app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt', 'rb') as f:
    lines = f.readlines()
print('Total lines:', len(lines))
for i in range(max(0, len(lines)-80), len(lines)):
    print(f"{i+1}: {lines[i].decode('utf-8', errors='replace').rstrip()}")