# Aplikasyon CAD düzeltmeleri — 2.0.2

Çalışma Aplikasyon Tutanağı'nın CAD canvas'ı ve bağlantı kaydıyla sınırlı tutuldu. İmalat Takibi değiştirilmedi.

İmzalı kurulum dosyası: [2.0.2 CAD APK](dist/Altyapi-Hesap-Makinesi-v2.0.2-CAD.apk).

SHA-256: `66700326BCA43380883FC32F759AC049A6AEBFD65F4ECC57483B487A95F35A23`

## Yapılanlar

- CAD, ayrı tam ekran çalışma alanı oldu. Açma düğmesi yatay filtre şeridinden çıkarılıp tam genişlikte sunuldu.
- Arama, bütün mahalle ağı üzerinde bacayı seçip odaklıyor; diğer bacaları ve bağlantıları filtreleyerek kaybettirmiyor.
- Ölçek çubuğu, çizim ve seçim aynı koordinat dönüşümünü kullanıyor. Kesirli metreler gösteriliyor; oklar bağlantı yönünü doğru işaret ediyor.
- Kaydırma ve yakınlaştırma birlikte yapılırken parmak odağı korunuyor. Tümünü göster, yakınlaştır/uzaklaştır ve kuzey/eksen açıklamaları eklendi. Kamera durumu mahalleye göre saklanıyor.
- Güncel veriyle dokunma seçimi sağlandı; seçimler kayıt nesnesi yerine ID ile tutuluyor. Etiketler de dokunularak seçilebiliyor.
- Aynı bölgede birden fazla baca varsa aday listesi açılıyor. Baca bul/seç listesi çizim için de kullanılabiliyor.
- Bağlantı kaydı sürerken yeni çizim engelleniyor. Başlangıç yalnızca başarıdan sonra ilerliyor; hata halinde korunuyor.
- Mevcut bağlantıyı değiştirme ve kaldırma onayı eklendi. Son bağlantı işlemleri geri alınabiliyor.
- Hat seçimi; başlangıç/hedef, yatay mesafe ve yeterli kot verisi varsa bağlantı yönünde kot düşümü gösteriyor. Bağlantı oku otomatik akış yönü olarak sunulmuyor.
- Proje/arazi kotları, adlar, yön okları ve dünya koordinatlarına bağlı ızgara için katman kontrolleri eklendi.
- Yanlış “Emir Defteri Var” ifadesi CAD'de kaldırıldı; durum gerçek anlamıyla kot farkı uyarısı olarak sunuluyor. Eksik kot ve kot farkı birlikte açıklanabiliyor.
- Araçlar birbirini örtmeyen, satıra taşabilen alanda; bilgi kartı canvas'ın dışında kaydırılabilir panelde yer alıyor.
- Etiket çakışmaları azaltıldı, seçili kayıt önceliklendirildi, uzun etiketler sınırlandı. Ekran dışındaki noktalara metin çizilmiyor; bağlantı hedefleri indeks üzerinden bulunuyor.

## Doğrulama

- **98 birim testi geçti.** Yeni geometri testleri düşey/yatay saha ölçeğini, dört yönde okları, birlikte pan/zoom davranışını ve hat seçim mesafesini kapsıyor.
- **9 Android veritabanı testi geçti.** Yeni CAD testi kayıt başarısını, meşgul durumda açık başarısızlık dönüşünü, geçersiz hedefte mevcut bağlantının korunmasını ve hedefin geri yüklenmesini kontrol ediyor.
- Emülatörde ayrı `CADReview / Canvas` test mahallesine dört baca aktarıldı. B2 ve D4 aynı koordinatta: aday seçimi çalıştı.
- B2→C3 oluşturuldu ve geri alındı. A1→B2 bağlantısı A1→C3 olarak değiştirilirken onay açıldı; uygulama ve geri alma sonrasında bilgi panelinde tekrar **Bağlı baca: B2** görüldü.
- Normal yazı ve 360 dp / %160 yazı koşullarında CAD araçları ile bilgi paneli incelendi.
- Debug ve release derlemeleri, lint ve imza doğrulaması yapıldı. Lint'te hata yok; mevcut uyarıların tamamı bu çalışmanın kapsamında değildir.

[CAD görünümü](tmp/cad-final-overview.png) · [Geri alma sonrası bağlantı](tmp/cad-connection-restored.png)

## Sınırlar

- Mevcut veri modeli baca başına **tek çıkış** bağlantısı tutuyor; bir bacaya birden fazla giriş mümkün. Çoklu çıkış için ayrı bağlantı tablosu gerekir.
- Geri alma geçmişi açık CAD oturumundaki son 50 işlemle sınırlı; kalıcı işlem günlüğü değildir.
- Yoğun görünümde çakışan etiketler gizlenir; en fazla 200 etiket yerleştirilir. Bacalar korunur; seçerek veya yakınlaştırarak ayrıntı görülebilir.
- X kuzey, Y doğu ve koordinat birimi metre kabul edilir. CRS dönüşümü/GPS altlık haritası eklenmedi.
- Fiziksel cihaz, tam TalkBack görev testi ve binlerce kayıtla performans ölçümü yapılmadı. Test mahallesi yalnızca emülatördedir.

## İlgili kaynaklar

[CAD canvas](app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt), [ortak geometri](app/src/main/java/com/example/egimhesabi/domain/CadGeometry.kt), [sayfa entegrasyonu](app/src/main/java/com/example/egimhesabi/ui/screens/StakeoutScreen.kt), [bağlantı kaydı](app/src/main/java/com/example/egimhesabi/viewmodel/StakeoutViewModel.kt).
