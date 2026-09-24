**Uygulama incelemesi — 13 Eylül 2026**

İnceleme mevcut kaynak kodu, veri akışları, hesaplama sınıfları ve Gradle kontrolleri üzerinden yapıldı. Uygulama kaynakları değiştirilmedi. Çalışma klasöründe Git deposu bulunmadığından önceki sürüme göre değişiklik karşılaştırması yapılamadı. Cihaz/emülatör üzerinde görsel veya uçtan uca saha testi yapılmadı; aşağıdaki cihaz etkileri kaynak kodu ve lint bulgularına dayanır. GNSS klasörü ayrı bir Python tanılama aracıdır; fiziksel alıcıyla bağlantı denenmedi.

**Kontrol sonuçları**

- `testDebugUnitTest`: Test kodu derlenemedi; testler çalıştırılamadı. `MemoryStakeoutDao`, yeni `clearRecords` ve `updateConnection` metotlarını uygulamıyor.
- `lintDebug`: 2 hata, 64 uyarı, 3 bilgi/öneri. İki hata API uyumluluğuyla ilgili. Uyarıların tamamı çalışma zamanı hatası değildir.
- Mevcut uygulama sınıflarıyla bağımsız JVM kontrolü: 1/300 → yüzde → oran dönüşümünde hassasiyet kaybı doğrulandı. 1.000 m için kot sonucu 96,6666667 yerine 96,7 oluyor.
- Aynı kontrolde interpolasyon tablosunun 100.000 m / 1 m için 100.001 satır ürettiği doğrulandı. Çok büyük tabloyla uygulamayı çökertme testi yapılmadı.
- Kontrol kodu: [ReviewProbe.java](C:/Users/hll/Desktop/egimhesabi/tmp/ReviewProbe.java:1).
- [Lint raporu](C:/Users/hll/Desktop/egimhesabi/app/build/reports/lint-results-debug.html:1).

**Öncelikli düzeltmeler**

1. **P1 — Eski veritabanından yükseltmede kayıtlar silinebilir.** Şema 15; geçişler yalnızca 10'dan başlıyor ve eksik geçişlerde `fallbackToDestructiveMigration()` kullanılıyor. Örneğin sürüm 9 veritabanıyla yükseltme, tüm uygulama tablolarının yeniden oluşturulmasına yol açabilir. Eski şemalar için veri koruyan geçişler eklenmeli, şema dışa aktarımı açılmalı ve eski veritabanı örnekleriyle yükseltme testi yapılmalı. Bu, güncel her kurulumda veri silindiği anlamına gelmez.
   Kaynak: [AppDatabase](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/data/AppDatabase.kt:48).

2. **P1 — Etki hesabında eski Android sürümleriyle uyumsuz iki çağrı var.** Zincirden son bacayı çıkaran `removeLast()` çağrıları lint tarafından API 35 gerektiriyor olarak işaretlendi; uygulamanın alt sınırı API 24. Dolu liste kontrolüyle `removeAt(lastIndex)` kullanılmalı ve desteklenen eski cihazda bu iki işlem denenmeli.
   Kaynak: [İlk çağrı](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt:1737), [İkinci çağrı](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt:1972).

3. **P1 — Ters hesapta birim değiştirmek hesap sonucunu değiştiriyor.** `formatCompact` iki ondalığa yuvarlanan değeri yeniden hesap girdisi yapıyor. Başlangıç kotu 100, mesafe 1.000 m ve eğim 1/300 iken yüzdeye geçmek yaklaşık 3,33 cm fark yaratıyor; oran geri dönüşte 1/303,03 oluyor. Eğim tam hassasiyetle tek bir iç değerde tutulmalı; biçimlendirme yalnızca ekranda yapılmalı.
   Kaynak: [ReverseCalculationViewModel](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/viewmodel/ReverseCalculationViewModel.kt:80).

4. **P1 — Excel başlangıç satırı, sütun eşleme ve satır düzenleme sonrasında unutuluyor.** `setDataStartRow` seçimi kaydediyor; `mapColumn`, `editSourceRow` ve `selectSheet` önizlemeyi varsayılan 4. satırdan başlatıyor. Örneğin başlangıç 8. satır seçilip sütun eşlemesi değiştirilirse arayüzün gösterdiği seçimle aktarılacak satırlar ayrışıyor. Her önizlemeye seçili başlangıç satırı verilmeli; sayfa değişiminde sıfırlanacaksa arayüz durumu da birlikte sıfırlanmalı. Başlangıç satırı güncellemelerinin diğer önizleme işlemleriyle yarışması da engellenmeli.
   Kaynak: [StakeoutViewModel](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/viewmodel/StakeoutViewModel.kt:120).

5. **P1 — Aplikasyon dosyasını tekrar yüklemek mevcut hat bağlantılarını siliyor.** Excel'den üretilen kayıtların `connectedToNameKey` alanı boş. Yeniden içe aktarma mevcut kaydı sadece kimliğini koruyarak bu kayıtla değiştiriyor. Böylece koordinatları güncellemek için aynı dosyayı yüklemek çizilmiş bağlantıları kaldırıyor. İçe aktarılan sütunlar açıkça güncellenmeli, uygulamada kurulmuş bağlantılar korunmalı.
   Kaynak: [StakeoutDao](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/data/StakeoutDao.kt:86).

6. **P1 — Mevcut emri düzenlemek hakedişini değiştirebiliyor.** Her kaydetmede global aktif hakediş okunup mevcut kaydın hakedişinin üzerine yazılıyor. Örneğin 2. hakedişteki emrin notunu, aktif hakediş 3 iken düzenlemek emri 3'e taşır; aktif değer boşsa bekleyenlere döndürür. Varsayılan aktif hakediş yalnızca yeni kayıt için kullanılmalı; mevcut kaydı taşıma açık bir işlem olmalı.
   Kaynak: [ProjectDetailViewModel](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/viewmodel/ProjectDetailViewModel.kt:232), [WorkOrderDao](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/data/WorkOrderDao.kt:151).

7. **P1 — Emir kaydetme hataları kullanıcıya bildirilmiyor.** Fotoğraf taşıma hatasında sessizce dönülüyor; veritabanı hatasında da hata durumu gösterilmiyor. Diyalog kaydetme başlamasıyla kapanıyor. Kullanıcı kaydın tamamlandığını sanabilir. Kaydetme durumu ve hata mesajı eklenmeli; diyalog yalnızca başarıdan sonra kapanmalı. Fotoğraf dosyalarını taşıma ve veritabanına yazma adımları başarısızlıkta geri alınabilir olmalı.
   Kaynak: [Kaydetme](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/viewmodel/ProjectDetailViewModel.kt:215), [Diyaloğun kapanması](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/ui/screens/MapScreen.kt:762).

8. **P2 — Emir defteri Excel çıktısında dosya uzantısı ve MIME tipi bozuk.** Dosya `.xlesx` olarak üretiliyor ve MIME değeri `appleication/...spreadsheetmle.sheet` içeriyor. Bu, uygun uygulamayla açmayı/paylaşmayı bozabilir. Uzantı `.xlsx`, MIME `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` olmalı. Sayfa adları için geçersiz karakter ve aynı ada dönüşen başlıkların çakışma kontrolü de eklenmeli.
   Kaynak: [ExcelExportHelper](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/util/ExcelExportHelper.kt:27), [MIME](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/util/ExcelExportHelper.kt:82).

9. **P2 — İnterpolasyonda elle yazılan mesafeyle kullanılan mesafe ayrışıyor.** Hesap doğrudan elle girilen mesafe yerine slider oranını kullanıyor. 100 m hatta 150 m yazmak alanı 150 olarak bırakıp hesabı 100 m için yapıyor. 100 m hatta 25 m girdikten sonra toplamı 200 yapmak, elle giriş 25 kalırken hesabı 50 m'ye taşıyor. Elle mesafe asıl veri olmalı; slider ondan türemeli. Hat dışı ve geçersiz girdilere açık hata verilmeli.
   Kaynak: [Elle giriş](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/viewmodel/InterpolationViewModel.kt:83), [Hesap](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/viewmodel/InterpolationViewModel.kt:159).

10. **P2 — Ters hesap ve interpolasyon tablolarının boyutu sınırsız.** Kullanıcı mesafesi üzerinden ana iş parçacığında sınırsız liste üretiliyor. Büyük sayı yanlışlıkla girildiğinde ekran donabilir veya bellek tükenebilir. Nivelmandaki gibi satır sınırı uygulanmalı; üst sınırı aşan giriş reddedilmeli, büyük hesaplar arka planda yapılmalı. Girdilerin yanında hesap sonuçlarının da sonlu sayı olması kontrol edilmeli.
    Kaynak: [InterpolationCalculator](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/domain/InterpolationCalculator.kt:47), [ReverseCalculator](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/domain/ReverseCalculator.kt:71).

11. **P2 — Aplikasyon bağlantıları baca adına bağlı; yeniden adlandırmada kopuyor.** Bağlantı hedefi `nameKey` ile tutuluyor. Hedef bacanın adını değiştirmek onu işaret eden kayıtları güncellemiyor; silmek de gelen bağlantıları temizlemiyor. Hedef kayıt kimliği ve uygun yabancı anahtar davranışı kullanılmalı veya ad değişimi/silme aynı işlem içinde bağlantılara yansıtılmalı.
    Kaynak: [Kayıt güncelleme](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/data/StakeoutDao.kt:47), [Bağlantı çizimi](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt:179).

12. **P2 — Emir defterinde fotoğrafa dokunmak hiçbir şey yapmıyor.** Kartın fotoğraf tıklaması bağlı fakat navigation'dan boş `onOpenPhoto` gönderiliyor. Fotoğraf görüntüleyici açılmalı; yakınlaştırma ve kapatma eklenmeli.
    Kaynak: [Navigation](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/Navigation.kt:199).

13. **P2 — Fotoğraflar ve dışa aktarma ekranı bloke edebilir.** Küçük fotoğraf kartları tam boy bitmap'i composable içinde açıyor; yeniden çizimde tekrar okunabiliyor. PDF raporunda bütün fotoğraflar tam boy çözülüp base64'e çevriliyor ve işlem tıklama içinde senkron çalışıyor. Boyuta göre küçültülmüş, önbellekli resim yükleme; arka planda rapor üretimi; ilerleme ve hata durumu öneriyorum. CSV seçimi de ana iş parçacığında sınırsız `readBytes()` yapıyor; Excel okuyucusundaki gibi sınır uygulanmalı.
    Kaynak: [Fotoğraf kartı](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/ui/components/WorkOrderItemCard.kt:114), [Rapor çağrısı](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/ui/screens/OrderBookScreen.kt:122), [CSV okuma](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/ui/screens/MapScreen.kt:161).

14. **P2 — Rapor metinleri HTML olarak yorumlanıyor.** Proje adı, emir başlığı, baca adı ve not doğrudan HTML'e ekleniyor. Örneğin not içindeki `<b>test</b>` olduğu gibi basılmıyor, rapor biçimini değiştiriyor. Bütün kullanıcı metinleri HTML kaçışından geçirilmeli.
    Kaynak: [HtmlReportGenerator](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/util/HtmlReportGenerator.kt:38).

15. **P2 — Etki geçmişinden dönüş tüm bilgileri geri yüklemiyor.** Kaydedilen `isDepthMode` ve `depthText`, `fromData` içinde alınmıyor. Geçmişi açma da kayıttaki eğim ve minimum derinlik sınırlarını geri yüklemeyip mevcut ayarları kullanıyor. Tarihî sonucun aynısını açma ile güncel ayarlarla yeniden hesaplama ayrı olmalı; kaydedilmiş giriş modu da korunmalı.
    Kaynak: [fromData](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/viewmodel/ImpactCalculationViewModel.kt:102), [loadFromHistory](C:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/viewmodel/ImpactCalculationViewModel.kt:340).

16. **P1 — Test altyapısı mevcut değişikliklerle uyumsuz.** Yeni DAO metotları sahte DAO'ya eklenmeden hiçbir JVM testi çalışamıyor. Önce test doubles güncellenmeli, ardından tüm testler çalıştırılmalı. Özellikle yeni bağlantı oluşturma, yeniden içe aktarma ve silme senaryoları kapsanmalı.
    Kaynak: [StakeoutDaoTest](C:/Users/hll/Desktop/egimhesabi/app/src/test/java/com/example/egimhesabi/data/StakeoutDaoTest.kt:118).

17. **P2 — Yayın anahtarı ile parolalar aynı proje paketinde tutuluyor.** `release-key.jks` proje içinde ve parolalar Gradle dosyasında düz metin olarak tanımlı. Proje klasörünün paylaşılması bu bilgileri de taşır. Anahtar ve parolalar kaynak paketinden ayrılmalı; parolalar ortam değişkeni veya yerel, paylaşılmayan yapılandırmadan okunmalı. Bu inceleme anahtarın dışarı sızdığını göstermiyor.
    Kaynak: [İmzalama yapılandırması](C:/Users/hll/Desktop/egimhesabi/app/build.gradle.kts:20).

**Uygulama geneline yönelik geliştirme önerileri**

| Alan | Öneri |
| --- | --- |
| Eğim hesabı | Sonuç hassasiyetini, sıfır eğim gösterimini ve sınır yakınlığı toleransını etki hesabıyla tutarlı hale getir. Geçmişte hangi ayarların kullanıldığını sakla. |
| Nivelman / kot taşıma | Girdileri süreç yeniden oluşturulduğunda geri getir. Ana ekrandan bu modüllere girerken koşulsuz `clearAll()` çağrısı yerine “Devam et / Yeni ölçüm” akışı değerlendir. |
| Ters hesap / interpolasyon | Ortak doğrulama, mesafe sınırı, hassasiyet ve profil üretme bileşeni kullan. Hatalı alanı işaretleyip önceki sonucu temizle. |
| Etki hesabı | Geçmişten açılan kayıtla güncel ayarlarla yeniden hesaplanan kaydı ayırt et; derinlik ve eğim uygunluğunu ayrı göstergelerle sun. |
| Aplikasyon | İçe aktarma önizlemesinde “yeni / güncellenecek / korunacak” kayıt sayılarını göster; bağlantıların korunmasını güvenceye al. |
| Proje takibi | Koordinatlar CSV ile güncellendiğinde kayıtlı hat mesafelerinin ne olacağı açık olsun. Şu anda hat mesafesi bağlantı kurarken kaydediliyor, koordinat içe aktarma bunu yeniden hesaplamıyor. |
| Emir defteri / hakediş | Mevcut hakedişi kayıtta görünür yap; taşıma ayrı bir kullanıcı işlemi olsun. Kaydetme başarısızsa girişleri koru. |
| Yedekleme | Proje, aplikasyon, emir, fotoğraf ve ayarları kapsayan uygulama içi yedekleme/geri yükleme akışı ekle; Excel'i tam uygulama yedeği gibi sunma. |
| Metinler / erişilebilirlik | “Geçerlei”, “Bileinmeyen” gibi bozulmuş metinleri düzelt. Aynı dönüşüm dosya uzantısı ve MIME değerlerini de bozmuş; yalnızca görünür yazıları taramak yeterli değil. Metinleri kaynak dosyalarında merkezileştir. Büyük yazı ve dar ekran ayrıca cihazda kontrol edilmeli. |
| Mimari | Çok büyüyen MapScreen ve ImpactCalculationScreen dosyalarını ekran durumu, diyaloglar, çizim ve veri işlemleri olarak ayır. Navigasyon yaşam döngüsü ve taslak saklama politikasını modüller arasında birleştir. |
| Proje düzeni | Git ve uygun ignore kuralları ekle. Kök dizindeki patch/fix/deneme betiklerini ve build çıktısını kaynak kodundan ayır; kullanılmayanları inceleyerek arşivle. |
| GNSS yardımcı araçları | Android uygulamasından ayrı bakım/test alanı olarak ele al. Bozuk/parçalı NMEA, geçersiz yön ve koordinat sınırlarını örnek paketlerle test et; fiziksel cihaz testi ayrı aşama olsun. |

**Önerdiğim uygulama sırası**

1. Veri koruyan migration, DAO test derlemesi ve iki API uyumluluk hatası.
2. Hesap hassasiyeti, Excel başlangıç satırı ve bağlantıların korunması.
3. Hakediş düzenleme ve güvenilir emir/fotoğraf kaydetme.
4. Excel paylaşma, fotoğraf açma, rapor metinleri ve performans sınırları.
5. Süreç sonrası taslak geri getirme, tam yedekleme, metin/erişilebilirlik ve mimari düzenleme.

Bu aşamalardan sonra temiz kurulum ile eski veritabanından yükseltme ayrı ayrı denenmeli; desteklenen eski Android sürümünde zincir seçimi, dosya seçiciden geri dönüş, büyük fotoğraf listesi ve süreç kapatılıp geri açılması kontrol edilmeli.

