# Düzeltmeler ve uygulama geneli UX incelemesi

14 Eylül 2026 — sürüm **2.0.1**, versionCode **3**.

Önceki incelemedeki 17 somut hata için düzeltmeler uygulandı. Ayrıca hesap girdilerinin korunması, temizleme onayı, okunabilirlik, büyük yazı, tablo boyutu ve işlem geri bildirimi iyileştirildi. Bu rapor, kaynak incelemesini ve gerçekten çalıştırılan kontrolleri ayırır; bütün cihazlarda eksiksiz saha testi yapıldığı anlamına gelmez.

## Teslim

- İmzalı APK: [Altyapi-Hesap-Makinesi-v2.0.1.apk](dist/Altyapi-Hesap-Makinesi-v2.0.1.apk), 2.282.283 bayt.
- SHA-256: `FD6AA939C1AEA822430BC6E7B04CDA4CF99B1D79823B9D641D748DE3AD704685`
- Önceki bulgular: [13 Eylül incelemesi](INCELEME_RAPORU_2026-09-13.md).
- [Birim testi raporu](app/build/reports/tests/testDebugUnitTest/index.html), [lint raporu](app/build/reports/lint-results-debug.html).

## Önceki bulguların durumu

| No | Sorun | Uygulanan düzeltme |
| --- | --- | --- |
| 1 | Yükseltmede veri silinmesi | Destructive migration kaldırıldı. Şema 16 ve 9→16 geçiş zinciri eklendi/düzeltildi. Eksik sütunlar koşullu ekleniyor; gerçek sıfır kot değerleri korunuyor. |
| 2 | API 35 isteyen liste çağrıları | Etki ekranındaki çağrılar alt Android sürümleriyle uyumlu liste işlemlerine çevrildi. Lint API hataları giderildi. |
| 3 | Oran/yüzde dönüşümünde hassasiyet kaybı | İki ondalığa yuvarlanan gösterim yeniden hesap girdisi yapılmıyor. Tekrarlı birim geçişleri regresyon testiyle doğrulandı. |
| 4 | Excel başlangıç satırının unutulması | Sütun eşleme, sayfa seçimi ve satır düzenleme önizlemeleri seçili başlangıcı kullanıyor. Geçersiz satır reddediliyor; önizleme işlemleri sıralanıyor. |
| 5 | Yeniden aktarımın bağlantıları silmesi | Mevcut kayıt kimliğiyle birlikte uygulamada kurulmuş bağlantı da korunuyor. |
| 6 | Emir düzenlerken hakedişin değişmesi | Mevcut emrin hakedişi hem ViewModel hem DAO düzeyinde korunuyor. Aktif hakediş yeni emir için varsayılan olmaya devam ediyor. |
| 7 | Sessiz kaydetme hataları | Kaydetme/bekleme/hata durumları eklendi. Diyalog başarıdan sonra kapanıyor. Fotoğraf kopyalama başarısızsa yeni kalıcı dosyalar geri alınıyor; taslak korunuyor. |
| 8 | Bozuk Excel uzantısı/MIME | `.xlsx` ve doğru MIME kullanılıyor. Sayfa adları temizleniyor, 31 karakter sınırı ve büyük/küçük harften bağımsız çakışma kontrolü uygulanıyor. |
| 9 | Elle mesafenin hesapla ayrışması | Hesap elle girilen mesafeyi kullanıyor, kaydırıcı bu değerden türetiliyor. Hat dışı giriş hata gösteriyor ve geçerli sonuç gibi sunulmuyor. |
| 10 | Sınırsız profil tablosu | Ortak profil doğrulaması, sonlu sayı kontrolleri ve en fazla 15.001 nokta sınırı eklendi. Arayüz 100 satırlık sayfalar gösteriyor. |
| 11 | Yeniden adlandırma/silmede kopan bağlantılar | Gelen bağlantılar aynı veritabanı işlemi içinde yeniden hedefleniyor veya temizleniyor. Kendine ve geçersiz hedefe bağlantı reddediliyor. |
| 12 | Fotoğraf dokunmasının işlem yapmaması | Emir defterine tam ekran fotoğraf görüntüleyici bağlandı; yakınlaştırma, kaydırma ve kapatma eklendi. |
| 13 | Fotoğraf/dosya işlemlerinde ekran bloklama | Boyuta göre küçültme, EXIF yönü ve bellek önbelleği eklendi. Fotoğraf okuma, CSV okuma ve rapor üretimi arka planda yapılıyor. Dosya boyutları sınırlandırılıyor. |
| 14 | Rapor metninin HTML olarak yorumlanması | Kullanıcı metinleri HTML kaçışından geçiriliyor; biçim bozulması regresyon testiyle kontrol edildi. |
| 15 | Etki geçmişinin eksik yüklenmesi | Derinlik modu ve metni geri yükleniyor. Tarihî sınırlar korunuyor; kullanıcı güncel sınırlarla yeniden hesaplamayı açıkça seçebiliyor. |
| 16 | Testlerin derlenememesi | Sahte DAO güncellendi; içe aktarma, bağlantı, hesaplama, dışa aktarma ve veritabanı testleri çalışır hale getirildi. |
| 17 | İmzalama sırlarının kaynakta bulunması | Anahtar ve parolalar proje dışındaki kullanıcı Gradle dizinine taşındı. Gradle yerel yapılandırma/ortam değişkenlerinden okuyor; ignore kuralları eklendi. |

Excel testleri sırasında ek bir uyumluluk sorunu da bulundu: kullanılan üreticinin geçerli ZIP64 çıktısı akış okuyucusunda hata veriyordu. Okuyucu merkezi ZIP dizinini kullanacak şekilde değiştirildi; boyut, giriş sayısı ve CRC kontrolleri korundu/eklendi. Geçici dosya işlem sonunda temizleniyor.

## UX değerlendirmesi

| Alan | Sorun / kullanıcı etkisi | Değişiklik ve doğrulama |
| --- | --- | --- |
| Ana menü | Sabit kart geometrisi büyük yazıda metni sıkıştırıyordu. | Uyarlanabilir sütunlar ve minimum yükseklik kullanıldı. 360 dp genişlik, %160 yazıda tek sütun ve kaydırma emülatörde kontrol edildi. |
| Eğim, ara kot, ters hesap | Alan etiketi, düşük kontrast ve hatalı girdi sonrası eski sonuç yanlış yorumlanabiliyordu. | Ortak girişlerin etiketleri/okunabilirliği iyileştirildi, seçili hata akışlarında eski sonuç temizleniyor. Ara Kot 100→90, toplam 100 m, ara 25 m için 97,500 m doğrulandı. |
| Ara kot / ters hesap tabloları | Çok büyük liste ekranı ve belleği zorlayabiliyordu. | Üretim sınırı ve sayfalama eklendi. Hatalı veya aşırı örnekleme isteği sessizce kabul edilmiyor. |
| Nivelman / kot taşıma | Ana menüden yeniden giriş ölçümü koşulsuz temizliyordu. | Otomatik temizleme kaldırıldı; girdiler SavedStateHandle ile korunuyor. Kullanıcı temizlemeyi açıkça başlatıyor. |
| Etki hesabı | Geçmiş kaydı güncel ayarlarla sessizce farklı sonuç verebiliyordu. | Kaydedilmiş sınırları belirten durum ve güncel sınırlara geçiş eklendi. Kaydetme bildirimi gerçek başarıdan sonra geliyor. |
| Temizleme / silme | Yanlış dokunmayla veri kaybı riski vardı. | Hesap temizlemede onay; emir silmede onay; ilçe silmede bağlı proje, aplikasyon, baca, hat ve emirlerin kapsamını açıklayan metin eklendi. Vazgeç akışı girdileri korudu. |
| Aplikasyon | Önizleme seçimiyle aktarımın ayrışması güveni bozuyordu. | Seçili başlangıç satırı korunuyor; yeni/güncellenecek kayıt özeti ve bağlantıların korunacağı bilgisi gösteriliyor. |
| İlçe / proje | Büyük yazıda diyalog yerleşimi ve yarı saydam zemin okumayı zorlaştırıyordu. | Proje formu opak ve kaydırılabilir yapıldı; form değerleri saveable. Büyük yazıda ilçe→proje→baca oluşturma akışı çalıştırıldı. |
| Kroki | Üst çubuk dar alanda uzun proje adını kısaltıyor. | İşlem düğmeleri erişilebilir kaldı; gözlenen kısaltma kayda geçirildi. Uzun adın tamamını gösteren ayrı proje ayrıntısı gelecekte yararlı olur. |
| Emir oluşturma | Şablona bağımlılık, erken kapanma ve kayıt sırasında değiştirilebilir alanlar vardı. | Serbest başlık, bekleme/hata geri bildirimi ve kayıt sırasında alan kilidi eklendi. Emir oluşturulup aktif hakediş sekmesinde görüldü. |
| Fotoğraflar | Küçük silme hedefi ve çalışmayan görüntüleme vardı. | Silme hedefi 48 dp; küçültülmüş yükleme ve tam ekran görüntüleyici eklendi. Gerçek çoklu fotoğraf seçimi ve düşük bellekli cihaz stres testi yapılmadı. |
| Dışa aktarma | Bekleme geri bildirimi ve geçerli dosya paylaşımı eksikti. | Arka planda üretim, ilerleme/hata durumu, güvenli HTML ve geçerli XLSX metadata eklendi. Harici Excel uygulaması/yazıcı ile uçtan uca kontrol yapılmadı. |
| Ayarlar / geçmiş | Ayarların eski hesap üzerindeki etkisi belirsizdi. | Etki geçmişinde kaydedilmiş/güncel sınırlar ayrıldı. Bütün ayarlar ve geçmiş türleri için ortak sürümlü kayıt modeli henüz oluşturulmadı. |

### Girdi devamlılığı denemesi

Ara Kot ekranına 100 ve 90 kotları, 100 m toplam mesafe, 25 m ara mesafe girildi. Temizleme onayından vazgeçildi. Uygulama arka plana alınıp süreci kapatıldı ve yeniden açıldı; süreç kimliği değişti. Girdiler ve 97,500 m sonucu geri yüklendi. Bu deneme bütün fotoğraf taslaklarının veya zorla durdurma sonrası her ekranın korunacağını garanti etmez.

### Kalan geliştirme alanları

1. **Tam yedekleme/geri yükleme:** Proje, aplikasyon, emir, fotoğraf ve ayarları birlikte taşıyan uygulama içi akış henüz yok. Excel tam uygulama yedeği değildir.
2. **Kroki erişilebilirliği:** Canvas üzerindeki bacalar ve hatlar için TalkBack odakları ve eşdeğer erişilebilir liste işlemleri kapsamlı ele alınmalı. Büyük yazı kontrolü, tam ekran okuyucu testi yerine geçmez.
3. **Taslak politikası:** Hesap girdileri korunuyor; açık emir diyaloğunun bütün fotoğraf taslakları için süreç ölümü sonrası kurtarma akışı ayrıca geliştirilmelidir.
4. **Koordinat güncellemesi:** CSV sonrası mevcut hat mesafelerinin yeniden hesaplanması mı, ölçüm değeri olarak korunması mı gerektiği ürün davranışı olarak netleştirilmeli. Bu çalışmada sessiz bir mesafe değiştirme davranışı eklenmedi.
5. **Tutarlılık:** Bazı ekranlarda küçük yardımcı metinler ve eski gezinme etiketleri bulunuyor. Metinleri kaynak dosyalarında toplama, bütün dokunma hedefleri ve TalkBack sırası için ayrı erişilebilirlik turu yararlı olur.
6. **Bakım:** Büyük MapScreen/ImpactCalculationScreen dosyalarını bölme, kalan lint uyarıları ve bağımlılık güncellemeleri ayrı iş olarak duruyor. Git deposu oluşturulmadı; ignore kuralları hazırlandı.
7. **GNSS:** Yardımcı Python araçları ve fiziksel GNSS alıcısı bu düzeltme turunda cihazla doğrulanmadı.

## Doğrulama sonuçları

| Kontrol | Sonuç |
| --- | --- |
| `testDebugUnitTest` | 94 test, 0 hata, 0 başarısız, 0 atlanan |
| Android instrumentation | API 36 emülatörde 8 test geçti |
| Eski veritabanı | Gerçek SQLite üzerinde dışa aktarılmış 9, 10 ve 11 şemalarından 16'ya geçiş; kayıt ve sıfır kot koruma doğrulandı |
| DAO regresyonu | Bağlantı davranışları ve hakediş düzenleme; başarısız çapraz proje kaydında mevcut emir/fotoğraf yollarının korunması doğrulandı |
| Lint | 0 hata, 77 uyarı, 5 öneri; uyarıların tamamı giderilmiş değildir |
| Derleme | Debug, androidTest ve küçültülmüş release başarılı |
| İmza | `apksigner verify --verbose` başarılı, v2 imza doğrulandı |
| UX | Normal ekran ve 360 dp / %160 yazı; menü, proje/baca/emir, temizleme ve Ara Kot süreç geri yükleme denendi |

Emülatörde yalnızca bu inceleme için oluşturulan ilçe/proje/baca/emir kayıtları kaldırıldı; önceki kayıtlar bırakıldı. Ekran boyutu, yoğunluk ve yazı ölçeği geri alındı. Fiziksel telefon kullanılmadı.

API 24–35 cihazlarda ayrı çalışma zamanı testi yapılmadı. Şema 9'dan önceki veritabanları için doğrulanmış geçmiş şema bulunmadığından otomatik veri silen geçiş uygulanmaz; bu kurulumlar ayrıca incelenmelidir.

## Derleme ve imzalama

Kontrol komutu: `gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease`.

Yayın imzası `%USERPROFILE%/.gradle/egimhesabi-signing/signing.properties` dosyasından veya `EGIM_KEYSTORE_FILE`, `EGIM_KEYSTORE_PASSWORD`, `EGIM_KEY_ALIAS`, `EGIM_KEY_PASSWORD` ortam değişkenlerinden alınır. Yerel properties anahtarları: `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. Parolalar rapora eklenmemiştir.

Çalışma öncesi yerel yedek ve tanılama çıktıları `tmp` altındadır. Bu dizin eski yapılandırmalar içerebildiği için kaynak teslim paketine dahil edilmemelidir; ignore kapsamındadır.
