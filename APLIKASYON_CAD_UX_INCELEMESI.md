# Aplikasyon Tutanağı — CAD canvas incelemesi

Bu incelemenin odağı yalnızca **Aplikasyon Tutanağı** içindeki `StakeoutMapView` ve onun sayfaya bağlanmasıdır. İmalat Takibi kapsam dışıdır. Kaynak kodu değiştirilmedi.

Emülatör bu turda bağlı değildi. Aşağıdaki bulgular kaynak akışı ve bağımsız sayısal kontroller üzerinden elde edildi; yeni canlı ekran testi yapıldığı iddia edilmiyor. Yoğun veri performansı ve dokunma çakışmaları ayrıca cihazda ölçülmeli.

## Önce düzeltilmesi gereken davranışlar

### 1. Ölçek çubuğu bazı hat geometrilerinde yanlış mesafe gösteriyor — P1

Harita nesneleri `min(genişlik / yatayAralık, yükseklik / düşeyAralık)` ölçeğiyle çiziliyor. Ölçek çubuğu ise yalnızca genişlik/yatayAralık kullanıyor. Düşey yönde uzun bir hat, çubuğun gösterdiğinden farklı ölçekte çiziliyor.

**Sayısal kontrol:** Çizim alanı 720×840 px, toplam yatay/düşey aralıklar 100/1000 m olduğunda çubuk 72 px için **10 m** yazıyor; aynı ekran mesafesi haritada **85,71 m** karşılığına geliyor. Bu, estetik değil ölçüye güven sorunudur. Ayrıca çubuk uzunluğu sonradan sınırlandırılıyor fakat etiketi yeniden hesaplanmıyor; 1 m altındaki değerlerde `toInt()` etiketi **0 m** yapabilir.

**Çözüm:** Nesneler, dokunma dönüşümü ve ölçek çubuğu tek bir kamera/dönüşüm modelini kullanmalı. Etiket uzunluğa tam karşılık gelmeli; kesirli metre gösterimi desteklenmeli.

[Kaynak](app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt:263)

### 2. Çizilen bağlantı okları ters yönü işaret ediyor — P1

Ok kanatları hedef noktanın gerisine değil ilerisine yerleştiriliyor. A=(0,0), B=(100,0) örneğinde kanatların X değeri 107,07; uç X=100. A→B bağlantısının oku böylece A tarafına bakıyor. Üstelik uç hedef bacanın merkezinde ve baca daha sonra çizildiği için okun bir kısmı noktanın altında kalıyor.

**Çözüm:** Ok kanatlarını yön vektörünün gerisinde oluştur; oku hattın ortasına veya hedef sembolünden önceye koy. “Bağlantı yönü” ile kotlardan hesaplanabilecek “akış yönü”nü ayrı adlandır.

[Kaynak](app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt:187)

### 3. Hızlı çizimde bağlantı sessizce atlanabiliyor — P1

Canvas her hedef dokunuşunda yeni kaydı ister ve hemen o hedefi sonraki başlangıç yapar. ViewModel, başka işlem sürüyorsa isteği `return` ile yok sayar. Canvas'a `busy` veya işlemin sonucu verilmediği için arayüz zincire devam ederken veritabanı bir halkayı kaydetmemiş olabilir. Hata halinde de başlangıç ilerlemiş olur.

**Senaryo:** A→B kaydı sürerken C'ye dokunulur. B→C isteği atlanabilir; canvas başlangıcı C yapar.

**Çözüm:** Kayıt tamamlanana kadar hedef seçimini beklet veya işlemleri sıralı kuyruğa al. Başlangıcı yalnızca başarıdan sonra ilerlet; başarısızlıkta aynı başlangıcı koru. Kaydedilen A→B bağlantısını kısa bir geri bildirimle göster.

[Canvas](app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt:144) · [İşlem koruması](app/src/main/java/com/example/egimhesabi/viewmodel/StakeoutViewModel.kt:200)

### 4. Arama haritada bulmak yerine geometrinin bağlamını siliyor — P1

Sayfa, canvas'a bütün mahalleyi değil yalnızca arama ve durum filtresinden geçen `matching` listesini veriyor. Hat hedefi bu listede yoksa çizilmiyor. Haritanın sınırları da kalan kayıtlarla yeniden hesaplanıyor.

**Senaryo:** A→B hattında A'yı aramak B'yi ve hattı görünümden kaldırır. Kullanıcı A'ya odaklanmak isterken ağı kaybeder. Mevcut yakınlaştırma/kaydırma korunurken temel sınırların değişmesi ayrıca haritanın yer değiştirmesine yol açabilir.

**Çözüm:** “Bul” ve “Filtrele” ayrı davranışlar olsun. Arama sonucu seçilen bacaya kamerayı götürsün; ağ görünür kalsın. Durum filtresinde diğer kayıtları soluk göstermek veya bağlantı uçlarını bağlam olarak korumak tercih edilebilir.

[Sayfa entegrasyonu](app/src/main/java/com/example/egimhesabi/ui/screens/StakeoutScreen.kt:249) · [Hat hedefi arama](app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt:180)

### 5. Veri güncellenince dokunma ve çizim farklı kayıtlarla çalışabilir — P1

Dokunma işleminin `pointerInput` anahtarları yalnızca bağlantı modu ve başlangıç kaydıdır. İçeride kullanılan kayıt listesi, harita sınırları ve boyutlar değişse bile bu anahtarlar değişmeyebilir. Bu durumda çizim yeni veriyi kullanırken çalışan dokunma işleyicisi eski yakalanmış veriyi kullanabilir. Seçili baca da kimlik yerine entity nesnesi olarak tutulduğundan bilgi kartı eski kot/ad değerini göstermeye devam edebilir.

**Çözüm:** İşaretçi işlemlerinde güncel veri/dönüşümü açıkça kullan; seçimleri ID ile tut ve güncel kaydı listeden çöz. Silinen seçimi temizle. Arama/aktarım sonrasında görünen bacaya dokunmanın aynı ID'yi seçtiğini test et.

[Kaynak](app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt:77) · [Dokunma işleyicisi](app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt:111)

### 6. Mevcut bağlantı fark ettirmeden değiştiriliyor; geri alma yok — P1

A zaten B'ye bağlıyken çizim modunda A→C seçimi doğrudan hedef alanını değiştirir. Önceki bağlantı gösterilerek değişiklik açıklanmıyor. Canvas'ta bağlantıyı seçme, bağlantıyı kaldırma veya son çizimi geri alma akışı yok. Alt metindeki “iptal için kendisine tekrar dokun” yalnızca başlangıç seçimini temizliyor; kaydedilmiş bağlantıyı geri almıyor.

**Çözüm:** Mevcut A→B bağlantısını belirgin göster. Değiştirme için açıklayıcı adım; başarılı işlemden sonra **Geri al** sun. Hat seçildiğinde ayrıntı ve **Bağlantıyı kaldır** eylemi göster. Tek çıkışlı veri modelinin kullanıcıya uygun olup olmadığı ayrıca netleştirilmeli; çoklu çıkış gerekiyorsa yalnızca UI değişikliği yetmez.

[Çizim davranışı](app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt:137) · [Veritabanı güncellemesi](app/src/main/java/com/example/egimhesabi/data/StakeoutDao.kt:133)

## Kullanımı zorlaştıran diğer sorunlar

| Öncelik | Bulgu | Kullanıcı etkisi ve öneri |
| --- | --- | --- |
| P2 | “Emir Defteri Var” gerçek emir kaydına dayanmıyor. | Turuncu durum yalnızca proje akar > arazi akar karşılaştırması. Gerçek emir varmış gibi sunuluyor. “Kot farkı uyarısı” olarak adlandır; gerçek emir ilişkisi varsa ayrı göster. Eksik veri ile kot farkı aynı anda varsa ikisini de göster. |
| P2 | 420 dp gömülü canvas; tam ekran yok. | Üst filtreler, canvas kontrolleri ve alt bilgi kartı çizim alanını daraltıyor. Sayfanın dikey kaydırmasıyla haritanın tek parmak sürüklemesi yarışabilir; bu cihazda denenmeli. Ayrı tam ekran CAD çalışma alanı aç. |
| P2 | Lejant ve araçlar bağımsız olarak aynı üst alana yerleştiriliyor. | Sol üst lejant ve sağ üst bağlantı/sıfırlama satırı dar ekranda veya büyük yazıda üst üste gelebilir. Araçları canvas dışındaki tek bir uyarlanabilir çubukta topla; lejantı açılır yap. |
| P2 | Yoğun kayıtta seçim belirsiz. | 40 dp yarıçap içinde sadece en yakın merkez seçiliyor. Etikete dokunma yok; aynı koordinattaki kayıtlarda ilk eşit aday kazanıyor. Birden fazla yakın aday varsa isimli seçim listesi sun; seçileni görünür biçimde vurgula. |
| P2 | Etiket çakışması ve kenardan kesilme yönetilmiyor. | Bütün adlar noktanın sağına çiziliyor; seçili kayıt en üste çizilmiyor. Yoğun ağda adlar birbirini örtebilir. Çakışma önleme, seçili kayda öncelik ve etiket katmanı kontrolü ekle. |
| P2 | Yakınlaştırma ve kaydırma birlikte yapıldığında sürükleme büyütülüyor. | Formül pan değerini de zoom ile çarpıyor. Tek bir dönüşüm adımında centroid=100, pan=10, zoom=2 için ofset -80 çıkarken hareketi sabitleyen ifade -90 verir. Parmak odağını koruyan dönüşüm kullan. |
| P2 | Kamera ve çalışma bağlamı korunmuyor. | Kamera yalnızca `remember`; liste/harita arasında gidip dönmek görünümü kaybettirebilir. Mahalleye göre kamera sakla. “Sıfırla” yerine ne yaptığı açık “Tümünü göster” kullan; seçili bacaya odaklan eylemi ekle. |
| P2 | Kuzey/yön ve koordinat açıklaması yok; ızgara ölçüsel değil. | Ekran yatayında Y, düşeyinde X kullanılıyor; bu bir haritacılık kabulü olabilir, tek başına hata değildir. Ancak açıklanmıyor. Sabit ekran ızgarası pan/zoom ile hareket etmiyor. Kuzey oku, X/Y anlamı ve dünya koordinatlarına bağlı ızgara sun veya ızgarayı yalnızca dekoratif olarak ayır. |
| P2 | Hat ayrıntısı yok. | Çizgiye dokunma, uzunluk ve uç baca özeti bulunmuyor. Hat seçimiyle A→B, yatay mesafe ve uygun veriler varsa eğim göster; hesap varsayımlarını belirt. |
| P2 | Bilgi kartı çizimi ve ölçek çubuğunu kaplıyor. | Alt kart dört değeri tek satıra koyuyor; uzun ad için de yer sınırı yok. Dar ekranda taşma riski var. Canvas dışında açılır alt panel, iki sütunlu bilgi grubu ve seçili noktayı panelin üstünde tutan kamera kullan. |
| P2 | Canvas nesneleri ekran okuyucu/klavye ile seçilemiyor. | Koordinat tabanlı jestler dışında nesne odakları yok. Baca arama/seçme listesi ve başlangıç→hedef formu, görsel çizimle aynı işlemleri yapabilmeli. |
| P3 | Yoğun veri çizim maliyeti yüksek. | Her bağlantı hedefi için listede `find` çağrısı; ekran dışında kalan bacalara da çizim ve metin ölçümü yapılıyor. Donma bu turda ölçülmedi. ID/ad indeksi, görünür alan elemesi ve etiket önbelleğiyle iyileştir; 100/1.000/5.000 kayıtla ölç. |

Bu tablonun kaynakları: [CAD canvas](app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt), [canvas'ı barındıran sayfa](app/src/main/java/com/example/egimhesabi/ui/screens/StakeoutScreen.kt).

## Önerilen CAD çalışma akışı

**Açılış:** Mahalle seçili → “CAD haritayı aç” → tam ekran ağ. Üstte mahalle adı, baca arama ve katmanlar; altta küçük durum satırı.

**Gezinme:** Tek parmak kaydırır, iki parmak yakınlaştırır. Görünür “Tümünü göster”, “+ / −” ve kuzey göstergesi bulunur. Arama bacayı ağdan ayırmaz; ona odaklanır.

**Seçim:** Bacaya dokun → belirgin seçim → alt panelde ad, koordinatlar, kotlar ve bağlı baca. Birden fazla aday varsa kullanıcı seçer. Hat seçimi ayrı bir ayrıntı paneli açar.

**Bağlantı çizimi:** Açıkça “Bağlantı çiz” moduna geç → başlangıç seç → hedef seç → A→B kaydet → başarıdan sonra zincire devam et. Ekranda başlangıç adı, “Bitir” ve “Geri al” sürekli görünür. Kayıt hatası başlangıcı değiştirmez.

**Katmanlar:** Baca adları, proje kotları, arazi kotları, bağlantı yönleri ve ızgara ayrı açılıp kapatılır. Uyarılar renk yanında simge/metinle anlatılır.

## Uygulama sırası ve kabul kontrolleri

1. **Güvenilir temel:** Ortak koordinat dönüşümü, doğru ölçek/ok, güncel dokunma verisi, sıralı bağlantı kaydı ve geri alma.
2. **Çalışma alanı:** Tam ekran, aramayla odaklanma, mahalle bazlı kamera ve seçilebilir hat/baca paneli.
3. **Yoğun ağ:** Etiket çakışması, aday seçimi, katmanlar, görünür alan çizimi ve erişilebilir alternatifler.

Kontrol veri kümeleri: tek baca; aynı koordinatta iki baca; uzun düşey/yatay hat; çapraz ve döngülü ağ; çok uzun baca adları; eksik kotlu kayıtlar; 1 m altı ve kilometre ölçeği; 1.000+ baca.

Özellikle A→B→C hızlı çizimi, mevcut A→B'yi A→C yapıp geri alma, kayıt hatası, arama sırasında seçim, veri güncellemesi sonrası dokunma ve ekran döndürme denenmeli. Tam ekran/dar ekran ile %100–%200 yazı ölçeği ayrıca kontrol edilmeli.
