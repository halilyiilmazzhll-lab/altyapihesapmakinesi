import re

path = 'app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt'
with open(path, 'rb') as f:
    raw = f.read()
content = raw.decode('utf-8', errors='replace')

# Fix 1: Kot bilgileri sadece cok yakin zoom'da ciksin
# baseScale * scale > 3 oldugunda goster (ekran piksel/metre oranina bagli)
content = content.replace(
    'val showDetails = scale > 5f',
    'val showDetails = (baseScale.toFloat() * scale) > 8f'
)

# Fix 2: Turkce karakterler
content = content.replace('"Baglanti Ciz"', '"Ba\u011flant\u0131 \u00c7iz"')
content = content.replace('"Sifirla"', '"S\u0131f\u0131rla"')
content = content.replace('"Baglantinin baslayacagi bacaya dokunun"', '"Ba\u011flant\u0131n\u0131n ba\u015flayaca\u011f\u0131 bacaya dokunun"')
content = content.replace('"Hedef bacaya dokunun (iptal icin kendisine tekrar dokun)"', '"Hedef bacaya dokunun (iptal i\u00e7in kendisine tekrar dokun)"')
content = content.replace('"Koordinatli baca bulunamadi."', '"Koordinatl\u0131 baca bulunamad\u0131."')

with open(path, 'wb') as f:
    f.write(content.encode('utf-8'))
print("Fixed showDetails and Turkish chars")