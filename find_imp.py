with open('app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt', 'rb') as f:
    lines = f.readlines()
for i, line in enumerate(lines):
    if b'ImpactMapPicker' in line:
        print(f"Line {i+1}: {line.decode('utf-8', errors='replace').rstrip()}")