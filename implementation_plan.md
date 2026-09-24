# Hakediş Bazlı Emir Defteri (Progress Payment Order Book)

Bu plan, kullanıcıların hakediş numaralarına göre gruplandırılmış emir defteri (iş emri/imalat notları) kayıtlarını görebilmelerini sağlamak için hazırlanmıştır.

## Background & Context
Şu anki sistemde "Emir Defteri" kayıtları (`work_orders` tablosu) spesifik bir bacaya (`manholeId`) bağlı olarak tutulmaktadır. Aynı zamanda bacaların ve boru hatlarının `progressPaymentNumber` (Hakediş Numarası) alanı bulunmaktadır. Kullanıcı, seçili bir hakedişe ait tüm imalat notlarını (emir defteri) ve fotoğraflarını toplu bir şekilde, "Hakediş Bazlı" olarak görebilmek istemektedir.

> [!IMPORTANT]
> **Kullanıcı Onayı Gerekiyor**
> Bu ekranın projeye özel bir ekran olacağı varsayılmıştır. Proje haritası (`MapScreen`) veya boru hattı listesi (`ProjectListScreen`) üzerinden yeni bir butona tıklayarak bu ekrana geçiş yapılması planlanmıştır. Ayrıca bu ekranda sadece "notlar ve fotoğraflar" mı, yoksa hakedişe dahil edilen "boru metrajları ve baca sayıları" (özet bilgiler) da gösterilmeli mi?

## Open Questions

> [!CAUTION]
> Lütfen aşağıdaki soruları netleştirin:
> 1. **Erişim Noktası:** Hakediş bazlı emir defteri ekranına nereden ulaşılmalı? Harita ekranının sağ üst köşesine veya yan menüye bir "Hakedişler" butonu koymamız uygun mudur?
> 2. **İçerik:** Sadece o hakedişteki bacalara yazılan "Emir Defteri (notlar/fotoğraflar)" kayıtları mı listelenecek, yoksa o hakedişteki toplam boru metrajı ve tamamlanan baca sayısı gibi hakediş özet bilgileri de gösterilsin mi?
> 3. **Filtreleme:** Hakediş numarası "girilmemiş" (progressPaymentNumber = null) olan taslak veya bekleyen emir defteri kayıtları için ayrı bir filtre gösterilmeli mi?

## Proposed Changes

### 1. Data Layer (`WorkOrderDao.kt` & `ManholeDao.kt`)
Yeni SQL sorguları eklenecek:
#### [MODIFY] [WorkOrderDao.kt](file:///c:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/data/WorkOrderDao.kt)
- `observeByProgressPayment(projectId: Long, paymentNumber: Int)`: Belirli bir hakediş numarasına sahip bacalara ait emir defteri kayıtlarını getiren sorgu.
- `observeUnassignedWorkOrders(projectId: Long)`: Henüz bir hakedişe bağlanmamış kayıtlara ulaşmak için.

#### [MODIFY] [ManholeDao.kt](file:///c:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/data/ManholeDao.kt) (and PipelineDao)
- `getDistinctProgressPayments(projectId: Long)`: Projede bulunan mevcut hakediş numaralarının (1. Hakediş, 2. Hakediş vb.) listesini getiren sorgu.

### 2. View Model (`ProjectDetailViewModel.kt`)
#### [MODIFY] [ProjectDetailViewModel.kt](file:///c:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/viewmodel/ProjectDetailViewModel.kt)
- Hakediş numaralarının listesini ve seçili hakedişe ait emir defteri kayıtlarını UI'a sunacak yeni State veya Flow değişkenleri tanımlanacak.

### 3. UI / Screens (`OrderBookScreen.kt` & `Navigation.kt`)
#### [NEW] [OrderBookScreen.kt](file:///c:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/ui/screens/OrderBookScreen.kt)
- Üstte hakediş numaralarının (Örn: "1. Hakediş", "2. Hakediş", "Tümü") seçilebileceği bir yatay sekme (Tab) dizilimi.
- Altta ise seçilen hakedişe ait `WorkOrderWithPhotos` verilerinin listelenmesi. Kartlarda bacanın adı, notlar, tarih ve ilgili fotoğraflar yer alacak.

#### [MODIFY] [Navigation.kt](file:///c:/Users/hll/Desktop/egimhesabi/app/src/main/java/com/example/egimhesabi/Navigation.kt)
- `ProjectOrderBook` adlı yeni bir `NavKey` rotası eklenecek.
- Harita ekranından (`MapScreen`) bu ekrana geçiş sağlanacak.

## Verification Plan

### Manual Verification
- Bir proje içerisindeki bazı bacalara emir defteri notları ve fotoğraflar eklenecek.
- Bu bacalar "Hakediş" durumuna (Örn: 1. Hakediş) alınacak.
- Hakediş Bazlı Emir Defteri ekranına girilip, "1. Hakediş" seçildiğinde sadece o hakedişe ait notların listelendiği doğrulanacak.
