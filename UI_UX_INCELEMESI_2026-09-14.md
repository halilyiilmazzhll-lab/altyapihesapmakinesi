# UI / UX incelemesi — 14 Eylül 2026

Bu tur inceleme amaçlıdır; uygulama kaynakları değiştirilmedi. Önceki teknik düzeltmelerden sonra kalan kullanılabilirlik sorunlarına odaklanıldı.

**Yöntem:** Android emülatöründe normal yazıyla Ayarlar ve Etki; 360 dp genişlik ve %160 yazıyla Etki, Ters Hesap ve ana menü incelendi. Önceki turdaki proje/kroki/emir görüntüleri yardımcı kanıt olarak kullanıldı. Diğer ekranlar kaynak üzerinden tarandı. Son aşamada emülatör bağlantısı kesildi; kalan ekranların bu turda görsel olarak test edildiği iddia edilmiyor. TalkBack, fiziksel cihaz ve güneş altında saha testi yapılmadı.

P1: Kullanıcının işlemi görmesini/yapmasını doğrudan engelliyor veya beklenmedik ayar değişikliği yaratıyor. P2: Kullanımı, anlaşılabilirliği veya erişilebilirliği belirgin zorlaştırıyor. P3: Tutarlılık ve görsel düzen iyileştirmesi.

## Öncelikli bulgular

### 1. P1 — Büyük yazıda kot ve mesafe değerleri kesiliyor

**Görsel olarak doğrulandı.** Etki Hesabı, 360 dp / %160 yazıda kapak kotu, akar kotu ve sonraki bacaya mesafe alanlarının sayısal metnini dikey olarak kesiyor. Değer modelde bulunuyor fakat kullanıcı tamamını okuyamıyor. Kaynakta `CompactField` 50 dp sabit yüksekliğe sahip; büyüyen etiket ve değer aynı alana sığmıyor.

**Öneri:** Sabit yüksekliği kaldır; minimum yükseklik ve içeriğe göre büyüme kullan. Etiket ve sayısal değer için ayrı alan bırak. Kabul kontrolü: %100, %130, %160 ve %200 yazıda etiket, tam değer ve birim görünmeli.

[Ekran görüntüsü](tmp/audit-impact-large.png) · [Kaynak](app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt:1390)

### 2. P1 — Etki kartında seçim ve silme kontrolleri kayboluyor

**Görsel olarak doğrulandı.** Aynı büyük yazı koşulunda “1. Baca (Sabit)” ile derinlik metni başlık satırını dolduruyor. Normal yazıda görülen tutanaktan seçme ve silme kontrolleri artık görünmüyor; taze arayüz hiyerarşisinde de yer almıyor. Kart 280 dp genişliğinde; metin ve işlemler aynı yatay satırda sıkıştırılıyor.

**Öneri:** Başlık ve derinliği üst satırlara; seçim ve silmeyi ayrı, sabit erişilebilir işlem satırına taşı. Kart genişliğini ekrana göre belirle. Yatay kartlar korunacaksa “1 / 3 baca” göstergesi ve ileri/geri kontrolü ekle.

[Ekran görüntüsü](tmp/audit-impact-large.png) · [Kaynak](app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt:851)

### 3. P1 — Ayar sıfırlamanın kapsamı yanlış anlaşılıyor

**Ekran yerleşimi + kaynakla doğrulandı; gerçek ayar sıfırlama yapılmadı.** “Varsayılana Sıfırla” eğim limitleri kartında yer alıyor. Metot eğim/derinliğe ek olarak ayrı “İmalat Takibi” bölümündeki kot farkı uyarı eşiğini de sıfırlıyor. Onay veya geri alma seçeneği yok.

**Öneri:** Düğmeyi yalnızca bulunduğu bölüme sınırla. Tüm hesap ayarlarını sıfırlama ayrı bir işlem olsun; değişecek ayarları ve varsayılan değerlerini göstersin.

[Yerleşim](app/src/main/java/com/example/egimhesabi/ui/screens/SettingsScreen.kt:235) · [Davranış](app/src/main/java/com/example/egimhesabi/viewmodel/SettingsViewModel.kt:206)

### 4. P2 — Etki ekranı örnek değerleri gerçek çalışma gibi sunuyor

**Ekran + kaynakla doğrulandı.** Varsayılan durum üç dolu baca ve “Tüm hatlar dengede” sonucuyla açılıyor. 100/98, 99/97,40 ve 98/97 değerlerinin örnek olduğu belirtilmiyor. Kullanıcı henüz kendi ölçümünü girmeden geçerli bir saha sonucu gördüğünü düşünebilir.

**Öneri:** İlk açılışta “Baca ekle / Tutanaktan seç / Örnekle dene” seçenekleri göster. Örnek seçilmişse görünür “Örnek veri” işareti kullan; kayıt sırasında bu niteliği açık tut.

[Görüntü](tmp/audit-impact.png) · [Varsayılan değerler](app/src/main/java/com/example/egimhesabi/viewmodel/ImpactCalculationViewModel.kt:118)

### 5. P2 — Ters Hesap iki sütunda sıkışıyor; eğim birimi belirsiz

**Görsel olarak doğrulandı.** %160 yazıda “1/X Yap” iki satıra bölünüp “Hedef Eğim” etiketine dayanıyor. “İstenen Mesafe” de iki satıra geçerek alanları hizasız bırakıyor. Birim seçimi, seçili durumu göstermek yerine karşı moda geçiş komutu olarak sunuluyor.

**Öneri:** Dar ekran/büyük yazıda tek sütuna geç. “Yüzde (%) / Oran (1/X)” seçicisiyle etkin birimi göster. Başlangıç, yön, eğim ve mesafeyi sıralı alanlar yap.

[Görüntü](tmp/audit-reverse-large.png) · [Kaynak](app/src/main/java/com/example/egimhesabi/ui/screens/ReverseCalculationScreen.kt:202)

### 6. P2 — Küçük ve soluk metinler hesaplama ekranını okumayı zorlaştırıyor

**Görsel gözlem + kaynak.** Etki ekranında önerilen kot aralığının başlığı 8 sp; yardımcı değerler ve bazı kontroller 9–11 sp. `TextTertiary` açık zemin üzerinde soluk kalıyor. Ters Hesap etiketleri de görsel olarak geri planda. Büyük yazı açmak ise 1. bulgudaki kesilmeyi tetikliyor.

**Öneri:** Form etiketi ve önemli yardımcı metinleri ortak, daha büyük tipografiyle sun; düşük kontrastlı rengi yalnızca dekoratif/ikincil öğelere ayır. Sayısal sonuç, birim ve geçerlilik metni ayrı görsel öneme sahip olsun. Gerçek saha ışığında okunabilirlik ayrıca ölçülmeli.

[Etki öneri alanı](app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt:964) · [Renkler](app/src/main/java/com/example/egimhesabi/theme/Color.kt:23)

### 7. P2 — Küçük işlem hedefleri ve yetersiz erişilebilir adlar var

**Kaynak bulgusu.** Etki kartında silme ve mod değiştirme 24 dp; artış/azalış yüzeyleri 30 dp olarak tanımlı. Aplikasyon satırında düzenleme/silme 34 dp. Compose bazı hedefleri görünür boyutun ötesine genişletebilir; bu yüzden yalnızca ikon boyutundan kesin dokunma başarısızlığı sonucu çıkarılmıyor. Ancak sık yerleşimde hedef çakışması riski ve “Mod”, “Sil” gibi bağlamı eksik adlar var.

**Öneri:** İşlem kapsayıcılarını en az 48 dp olarak açıkça ayır. “Baca 2'yi sil”, “Derinlik girişine geç”, “Akar kotunu 1 cm artır” gibi işlem ve bağlamı anlatan erişilebilir adlar kullan.

[Etki kartı](app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt:878) · [Artış/azalış](app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt:1426) · [Aplikasyon](app/src/main/java/com/example/egimhesabi/ui/screens/StakeoutScreen.kt:397)

### 8. P2 — Kroki etkileşimleri için erişilebilir eşdeğer akış eksik

**Kaynak bulgusu; TalkBack testi yapılmadı.** Baca/hat çizimi Canvas ve koordinat tabanlı dokunma işlemleriyle yürütülüyor. Çizimdeki her nesne için semantik odak ve işlem listesi görünmüyor. Mevcut arama/liste düğmeleri yararlı, fakat çizerek bağlantı kurmanın bütün adımlarının erişilebilir karşılığı doğrulanmış değil.

**Öneri:** “Başlangıç bacası → hedef baca → bağlantıyı oluştur” metin tabanlı akışı ekle; her çizim nesnesini seçilebilir listeyle eşleştir. Sonrasında TalkBack ile bağımsız görev testi yap.

[Kroki](app/src/main/java/com/example/egimhesabi/ui/screens/MapScreen.kt:394) · [Aplikasyon çizimi](app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt:156)

### 9. P2 — Eğim limitlerinde ifade ve kaydetme davranışı yeterince açık değil

**Ekran + kaynak.** “Minimum Eğim Oranı (X)” ve “Maksimum Eğim Oranı (X)” adları altında minimuma 200, maksimuma 7 giriliyor. Yardım metni “Min X >= Max X” kuralını açıklıyor; kullanıcıya gerçek eğimi doğrudan göstermiyor. Geçerli alan değişiklikleri otomatik kaydediliyor; aktif hakediş alanının boşaltılması da hemen seçimi kaldırıyor.

**Öneri:** “En düşük eğim: 1/200 = %0,50” ve “En yüksek eğim: 1/7 = %14,29” biçiminde canlı karşılık göster. Otomatik kayıt tercih edilecekse durumunu belirt; kritik alanlarda düzenlemeyi “Uygula” ile tamamla. Boş hakedişin “Yeni kayıtlar bekleyenlere gider” anlamını açıkla.

[Görüntü](tmp/audit-settings.png) · [Alan değişiklikleri](app/src/main/java/com/example/egimhesabi/viewmodel/SettingsViewModel.kt:99)

### 10. P2 — Fotoğrafta yükleniyor ve hata durumları aynı mesaj

**Kaynak bulgusu.** Görüntüleyici null bitmap için “Fotoğraf yüklenemedi veya hazırlanıyor.” gösteriyor. Kullanıcı beklemeli mi, tekrar denemeli mi anlayamıyor. Yakınlaştırılmış fotoğrafın kaydırılması da sınırsız; görüntü tamamen ekran dışına taşınabilir. Sıfırlama düğmesi var, ancak bunu fark etmek gerekiyor.

**Öneri:** Yükleniyor, yüklendi ve hata durumlarını ayır. Hata durumunda tekrar deneme sun; kaydırmayı fotoğraf sınırlarına göre kısıtla. Yakınlaştırma hareketine erişilebilir düğme alternatifi ekle.

[Kaynak](app/src/main/java/com/example/egimhesabi/ui/components/PhotoViewer.kt:24)

### 11. P3 — Ana menü okunuyor fakat sık kullanılan işlemlere erişim uzuyor

**Büyük yazıda gözlendi.** Tek sütunlu büyük kartlar kesilmeyi azaltıyor; fakat ekranın sabit başlık ve aktif tutanak bölümü önemli alan kaplıyor. Alt hesaplama modüllerine ulaşmak için birkaç ekran kaydırmak gerekiyor.

**Öneri:** “Saha kayıtları” ve “Hesaplamalar” grupları, kompakt satır alternatifi ve son kullanılanlar sun. Aktif tutanağı daha kısa bir özet satırına dönüştür. Hangi modülün öne alınacağı gerçek kullanım sıklığıyla belirlenmeli.

[Önceki büyük yazı görüntüsü](tmp/ux-home-large-font.png) · [Kaynak](app/src/main/java/com/example/egimhesabi/ui/screens/HomeScreen.kt)

### 12. P3 — Gezinme ve eylem adları tutarsız

**Görsel + kaynak.** Geri okunun adı Ayarlar'da “Menü”, İmalat Takibi'nde “Menüyü aç”, diğer ekranlarda “Geri”. Ana menüde “Tersine Hesap”, ekran içinde “Ters Hesaplama” kullanılıyor. Bazı önemli işlemler yalnızca ikonla gösteriliyor; örneğin Etki'de kaydetme onay işaretiyle temsil ediliyor.

**Öneri:** Ortak ekran adları ve “Geri” davranışını standartlaştır. Özellikle ilk kullanımda kaydetme/haritadan seçme işlemlerini metinle destekle; simge açıklamalarını bağlamlı yap.

[Ayarlar](app/src/main/java/com/example/egimhesabi/ui/screens/SettingsScreen.kt:106) · [Takip](app/src/main/java/com/example/egimhesabi/ui/screens/TrackingScreen.kt:165)

## Kapsam ve önerilen sıra

Önce Etki'nin büyüyen metinle yerleşimi ve ayar sıfırlama kapsamı düzeltilmeli. Ardından Ters Hesap yerleşimi, örnek veri ayrımı, okunabilirlik ve ayarların kaydedilme davranışı ele alınmalı. Son turda çizim erişilebilirliği, fotoğraf durumları ve menü tutarlılığı tamamlanmalı.

Nivelman ve Kot Taşıma kaynaklarında hata metinleri ve alan semantiği mevcut; bunlara sırf diğer ekranlarda sorun bulunduğu için görsel hata atanmıyor. Sabit yükseklikli girişleri büyük yazıda ayrıca denenmeli. Aplikasyonun dolu tablo/Excel eşleme ekranı, Emir Defteri'nin uzun not/çoklu fotoğraf senaryoları ve geçmiş diyalogları için bu turda tam görsel kapsama ulaşılamadı.

Bu bulgular mevcut teknik testlerin geçmesiyle çelişmez: hesaplama/veritabanı testleri metnin kesilmesini, düğmenin görünmemesini veya işlemin yanlış anlaşılmasını doğrulamaz.
