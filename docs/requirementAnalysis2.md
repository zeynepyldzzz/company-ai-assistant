# Gereksinim Analizi
## Yapay Zeka Destekli Şirket İçi Asistan Mobil ve Web Uygulaması

---

# 1. Proje Genel Bakış

## Proje Adı
**Yapay Zeka Destekli Şirket İçi Asistan**

## Proje Açıklaması

Yapay Zeka Destekli Şirket İçi Asistan, bir organizasyon içinde iç iletişimi ve operasyonel verimliliği artırmak için tasarlanan mobil ve web tabanlı bir uygulamadır. Uygulama; çalışanların şirket bilgilerine erişebileceği, yapay zeka destekli chatbot ile etkileşime geçebileceği, günlük hizmetleri görüntüleyebileceği, ulaşım bilgilerini yönetebileceği, kendi çalışma düzenini güncelleyebileceği, anketlere katılabileceği ve şirket içi kaynaklara ulaşabileceği merkezi bir platform sunar.

Sistem ayrıca yöneticilerin çalışanları, departmanları, duyuruları, ulaşım bilgilerini, yemek menülerini, şirket araçlarını, anketleri ve yapay zeka bilgi tabanını yönetmesini sağlayan kapsamlı bir web tabanlı yönetim paneli içerir.

---

# 2. Problem Tanımı

Çalışanlar, aşağıdaki şirket içi bilgilere ulaşmak için sıklıkla önemli ölçüde zaman harcar:

- Şirket kişi bilgileri
- Departman sorumlulukları
- Günlük yemek menüleri
- Servis güzergahları
- Uygun şirket araçları
- Şirket duyuruları
- Şirket içi dokümanlar
- Yeni işe başlayan çalışanların bilmesi gereken şirket prosedürleri
- İzin, fazla mesai ve mazeret izni gibi İK süreçleri
- Haftalık ofis, uzaktan çalışma ve izin günü düzenleri

Bu süreçler genellikle e-postalar, elektronik tablolar, mesajlaşma platformları ve manuel iletişim kanalları arasında dağınık durumdadır. Bu durum çalışanlar için zaman kaybına ve operasyonel verimsizliğe neden olur.

Önerilen sistem, tüm şirket hizmetlerini yapay zeka destekli tek bir akıllı platformda merkezileştirir.

---

# 3. Hedefler

- Çalışan verimliliğini artırmak
- Şirket içi iletişim gecikmelerini azaltmak
- Şirket bilgilerini merkezi hale getirmek
- Yapay zeka destekli akıllı destek sağlamak
- Çalışanların şirket prosedürleri ve İK süreçleri hakkında hızlı bilgi almasını sağlamak
- Ulaşım yönetimini kolaylaştırmak
- Çalışanların kendi haftalık çalışma düzenlerini mobil ve web uygulamaları üzerinden yönetmesini sağlamak
- Anketler aracılığıyla çalışan katılımını artırmak
- Yöneticilerin şirket kaynaklarını verimli şekilde yönetmesini sağlamak

---

# 4. Paydaşlar

## Birincil Paydaşlar

- Çalışanlar
- İnsan Kaynakları Departmanı
- Bilgi Teknolojileri Departmanı
- Şirket Yönetimi
- Servis Koordinatörleri
- Yemekhane Yönetimi
- Araç Sorumluları

## İkincil Paydaşlar

- Sistem Yöneticileri
- Yazılım Geliştiriciler
- Yapay Zeka Mühendisleri

---

# 5. Kullanıcı Rolleri

## Çalışan

Çalışanlar şunları yapabilir:

- Güvenli şekilde giriş yapmak
- Yapay zeka chatbotunu kullanmak
- İşe giriş prosedürleri, izin kuralları, fazla mesai ve mazeret izni hakkında soru sormak
- Yemek menüsünü görüntülemek
- Çalışan aramak
- Departman aramak
- Telefon rehberini görüntülemek
- Uygun şirket araçlarını bulmak
- Servis bilgilerini görüntülemek
- Kendi haftalık çalışma düzenini yönetmek
- Duyuruları almak
- Anketlere katılmak
- Yorum göndermek

---

## Yönetici

Yöneticiler şunları yapabilir:

- Çalışanları yönetmek
- Departmanları yönetmek
- Yemek menülerini yüklemek
- Servis güzergahlarını yönetmek
- Servis plaka bilgilerini güncellemek
- Araçları yönetmek
- Duyuru yayımlamak
- Anket oluşturmak
- Chatbot bilgi tabanını yönetmek
- İK prosedürlerini ve politika dokümanlarını yönetmek
- Çalışanların çalışma düzeni verilerini yönetim panelinde görüntülemek
- Bildirimleri yönetmek
- Rapor oluşturmak

---

# 6. Fonksiyonel Gereksinimler

## Kimlik Doğrulama

FR-01 Kullanıcılar şirketin kimlik doğrulama sistemi ile giriş yapabilmelidir.

FR-02 Sistem güvenli giriş işlemini desteklemelidir.

FR-03 Sistem kullanıcı oturumlarını yönetmelidir.

---

## Ana Panel

FR-04 Sistem kişiselleştirilmiş ana panel göstermelidir.

FR-05 Sistem hızlı erişim kartlarını göstermelidir.

FR-06 Sistem bildirimleri göstermelidir.

FR-07 Sistem profil bilgilerini göstermelidir.

---

## Yapay Zeka Chatbotu

FR-08 Kullanıcılar yapay zeka chatbotu ile iletişim kurabilmelidir.

FR-09 Chatbot şirketle ilgili soruları yanıtlayabilmelidir.

FR-10 Chatbot şirket içi bilgi tabanından bilgi getirebilmelidir.

FR-11 Chatbot, işe yeni başlayan bir çalışanın bilmesi gereken şirket prosedürlerini açıklayabilmelidir.

FR-12 Chatbot, çalışanların "Bu prosedüre göre nasıl izin alabilirim?" gibi prosedür bazlı sorularına yanıt verebilmelidir.

FR-13 Chatbot; yıllık izin, mazeret izni, fazla mesai ve benzeri İK süreçleri hakkında bilgi verebilmelidir.

FR-14 Chatbot, cevaplarını yalnızca onaylı şirket prosedürleri ve güncel İK dokümanlarına dayandırmalıdır.

FR-15 Kullanıcılar yetkileri varsa dosya yükleyebilmelidir.

FR-16 Kullanıcılar sesli giriş kullanabilmelidir.

---

## Yemek Menüsü

FR-17 Sistem bugünün menüsünü göstermelidir.

FR-18 Sistem haftalık menüyü göstermelidir.

FR-19 Sistem kalori bilgilerini göstermelidir.

FR-20 Sistem alerjen bilgilerini göstermelidir.

FR-21 Yöneticiler Excel menü dosyaları yükleyebilmelidir.

---

## Servis Yönetimi

FR-22 Sistem servis güzergahlarını göstermelidir.

FR-23 Sistem servis duraklarını göstermelidir.

FR-24 Sistem servis saatlerini göstermelidir.

FR-25 Sistem güncel plaka bilgilerini göstermelidir.

FR-26 Sistem çalışanın varış noktasına göre en uygun servis güzergahını önermelidir.

FR-27 Sistem tahmini varış süresini göstermelidir.

---

## Çalışan Rehberi

FR-28 Kullanıcılar çalışan arayabilmelidir.

FR-29 Kullanıcılar çalışanları filtreleyebilmelidir.

FR-30 Sistem çalışan bilgilerini göstermelidir.

FR-31 Sistem çalışanın ofis durumunu göstermelidir.

FR-32 Sistem telefon numaralarını göstermelidir.

FR-33 Sistem e-posta adreslerini göstermelidir.

---

## Departman Rehberi

FR-34 Sistem departmanları göstermelidir.

FR-35 Sistem departman sorumluluklarını göstermelidir.

FR-36 Sistem departman yöneticisini göstermelidir.

FR-37 Sistem departman iletişim bilgilerini göstermelidir.

---

## Şirket Araçları

FR-38 Sistem uygun araçları göstermelidir.

FR-39 Kullanıcılar şirket araçlarını rezerve edebilmelidir.

FR-40 Kullanıcılar rezervasyon durumunu görüntüleyebilmelidir.

FR-41 Sistem bakım durumunu göstermelidir.

---

## Anketler

FR-42 Kullanıcılar anketlere katılabilmelidir.

FR-43 Kullanıcılar anonim geri bildirim gönderebilmelidir.

FR-44 Yetkili kullanıcılar anket sonuçlarını görüntüleyebilmelidir.

---

## Duyurular

FR-45 Sistem duyuruları göstermelidir.

FR-46 Kullanıcılar anlık bildirim alabilmelidir.

FR-47 Sistem önemli duyuruların sabitlenmesini desteklemelidir.

---

## Telefon Rehberi

FR-48 Kullanıcılar şirket içi kişileri arayabilmelidir.

FR-49 Sistem telefon numaralarını göstermelidir.

FR-50 Sistem dahili numaraları göstermelidir.

---

## İK Prosedürleri ve Çalışan Politikaları

FR-51 Sistem, işe yeni başlayan çalışanlar için bilinmesi gereken temel prosedürleri listelemelidir.

FR-52 Sistem, oryantasyon, şirket kuralları, çalışma saatleri, departman iletişimi ve iç kaynak kullanımı gibi işe giriş süreçlerini açıklamalıdır.

FR-53 Sistem, çalışanların yıllık izin, mazeret izni, hastalık izni ve diğer izin türleri hakkında bilgi almasını sağlamalıdır.

FR-54 Sistem, "Bu prosedüre göre nasıl izin alabilirim?" gibi sorular için adım adım yönlendirme sunmalıdır.

FR-55 Sistem, fazla mesai prosedürünü, fazla mesai talep/onay sürecini ve fazla mesai kullanım veya ödeme kurallarını açıklamalıdır.

FR-56 Sistem, mazeret izni koşullarını, gerekli belgeleri ve başvuru adımlarını göstermelidir.

FR-57 Sistem, ilgili prosedür için sorumlu departman veya iletişime geçilecek kişiyi göstermelidir.

FR-58 Sistem, prosedürlerin güncel versiyonlarını, geçerlilik tarihlerini ve varsa doküman eklerini saklamalıdır.

---

## My Work Schedule (Çalışma Düzenim)


FR-59 Çalışanlar kendi haftalık çalışma düzenlerini yönetebilmelidir.

FR-60 Sistem, her iş günü (Pazartesi–Cuma) için "Ofiste", "Uzaktan" ve "İzinli" durumlarından birinin seçilmesine izin vermelidir.

FR-61 Sistem, seçilen günlere göre haftalık özet göstermelidir (ofis, uzaktan ve izin gün sayıları).

FR-62 Sistem, çalışma düzeni seçimlerini kaydedebilmeli ve kaydedilen düzeni kalıcı olarak saklamalıdır.

FR-63 Sistem, bu ekranda yalnızca giriş yapan çalışanın kendi çalışma düzenini göstermeli ve düzenlemesine izin vermelidir.

FR-64 Sistem, çalışma düzeni verilerini tüm ekranlarda tek kaynak üzerinden tutarlı şekilde göstermelidir.

---

## Bildirimler

FR-65 Kullanıcılar sistem bildirimleri alabilmelidir.

FR-66 Kullanıcılar acil durum bildirimleri alabilmelidir.

FR-67 Kullanıcılar bildirim tercihlerini yönetebilmelidir.

---

## Yönetim Paneli

FR-68 Yöneticiler çalışan oluşturabilmelidir.

FR-69 Yöneticiler çalışan bilgilerini güncelleyebilmelidir.

FR-70 Yöneticiler çalışan silebilmelidir.

FR-71 Yöneticiler departmanları yönetebilmelidir.

FR-72 Yöneticiler yemek menülerini yükleyebilmelidir.

FR-73 Yöneticiler servis güzergahlarını yönetebilmelidir.

FR-74 Yöneticiler şirket araçlarını yönetebilmelidir.

FR-75 Yöneticiler duyuru yayımlayabilmelidir.

FR-76 Yöneticiler anket oluşturabilmelidir.

FR-77 Yöneticiler chatbot bilgi tabanını yönetebilmelidir.

FR-78 Yöneticiler İK prosedür dokümanlarını ve politika versiyonlarını yönetebilmelidir.

FR-79 Yöneticiler çalışma düzeni verilerini "Çalışma Düzenleri" tablosunda görüntüleyebilmelidir.

FR-80 Yöneticiler kullanıcı izinlerini yönetebilmelidir.

FR-81 Yöneticiler rapor oluşturabilmelidir.

FR-82 Yöneticiler raporları dışa aktarabilmelidir.
---

# 7. Fonksiyonel Olmayan Gereksinimler

## Performans

NFR-01 Uygulama normal işlemlerde 2 saniye içinde yanıt vermelidir.

NFR-02 Chatbot 5 saniye içinde yanıt vermelidir.

---

## Güvenlik

NFR-03 Sistem güvenli kimlik doğrulama sağlamalıdır.

NFR-04 Sistem rol tabanlı erişim kontrolünü (RBAC) desteklemelidir.

NFR-05 Sistem veri şifreleme sağlamalıdır.

NFR-06 Sistem denetim kayıtları tutmalıdır.

NFR-07 Yöneticiler için iki faktörlü kimlik doğrulama desteklenmelidir.

---

## Kullanılabilirlik

NFR-08 Arayüz basit ve sezgisel olmalıdır.

NFR-09 Tasarım farklı ekran boyutlarına uyumlu olmalıdır.

NFR-10 Sistem erişilebilirlik standartlarına uyumlu olmalıdır.

---

## Güvenilirlik

NFR-11 Sistem %99.9 erişilebilirlik sağlamalıdır.

NFR-12 Sistem otomatik yedekleme desteklemelidir.

NFR-13 Sistem hata kayıtları tutmalıdır.

---

## Ölçeklenebilirlik

NFR-14 Sistem artan kullanıcı sayılarını desteklemelidir.

NFR-15 Sistem modüler mimariye sahip olmalıdır.

---

## Sürdürülebilirlik

NFR-16 Sistem temiz mimari prensiplerine uygun olmalıdır.

NFR-17 Sistem API öncelikli geliştirme yaklaşımını desteklemelidir.

NFR-18 Kod tabanı modüler olmalıdır.

---

# 8. Veri Gereksinimleri

Sistem aşağıdaki verileri saklamalıdır:

- Çalışanlar
- Departmanlar
- Yemek menüleri
- Yemekler
- Servis güzergahları
- Servis durakları
- Araçlar
- Rezervasyonlar
- Anketler
- Anket yanıtları
- Duyurular
- Bildirimler
- Sohbet geçmişi
- Yüklenen dokümanlar
- İK prosedürleri
- İzin politikaları
- Fazla mesai politikaları
- Politika versiyonları ve geçerlilik tarihleri
- Çalışan haftalık çalışma düzeni (`weeklySchedule`)
- Günlük çalışma durumu değerleri (`office`, `remote`, `leave`)
- Roller
- İzinler

---

# 9. Harici Arayüzler

## Mobil Uygulama

- Android
- iOS
- "My Work Schedule" / "Çalışma Düzenim" ekranı
- Takvim ikonlu çalışma düzeni menü öğesi

---

## Web Uygulaması

- React
- TypeScript
- Tailwind CSS
- `/my-schedule` çalışan çalışma düzeni sayfası
- Sol menüde takvim ikonlu "My Schedule" bağlantısı

---

## Web Yönetim Paneli

Masaüstü web uygulaması.

---

## API'ler

- Kimlik Doğrulama API'si
- Yapay Zeka Servisi API'si
- İK Politika/Prosedür API'si
- Bildirim API'si
- Harita API'si
- E-posta API'si

---

## Veritabanı

- PostgreSQL (önerilir)

---

# 10. Kısıtlar

- Şirket kimlik doğrulaması gereklidir.
- Çalışan bilgileri gizli kalmalıdır.
- Yapay zeka yanıtları yalnızca yetkili şirket verilerine dayanmalıdır.
- İK prosedürleri ve izin/fazla mesai cevapları yalnızca onaylı ve güncel dokümanlara dayanmalıdır.
- Şirket bilgilerini yalnızca yöneticiler değiştirebilmelidir.
- Yalnızca yetkili İK veya sistem yöneticileri prosedür ve politika içeriklerini güncelleyebilir.
- Çalışma düzeni ekranı mobil ve web uygulamalarında yalnızca giriş yapan çalışanın kendi `weeklySchedule` verisini göstermeli ve düzenlemelidir.
- Çalışma düzeni verisi çalışan nesnesinde tek kaynak olarak tutulmalı; DataContext, localStorage, admin paneli ve diğer ekranlar aynı veriyi kullanmalıdır.
- Excel içe aktarma işlemi önceden tanımlanmış şablonları desteklemelidir.

---

# 11. Varsayımlar

- Çalışanların internet erişimi vardır.
- Şirket verileri yöneticiler tarafından güncel tutulur.
- Yapay zeka bilgi tabanı düzenli olarak güncellenir.
- İK departmanı izin, fazla mesai, mazeret izni ve işe giriş prosedürlerini güncel tutar.
- Çalışanlar haftalık çalışma düzenlerini mobil veya web uygulaması üzerinden kendileri günceller.
- Servis önerileri için GPS hizmetleri kullanılabilir durumdadır.

---

# 12. Gelecek Geliştirmeler

- İzin yönetimi
- Çevrim içi izin talep iş akışı
- Fazla mesai talep ve onay iş akışı
- Masraf yönetimi
- Toplantı odası rezervasyonu
- Devam takibi
- QR tabanlı çalışan giriş kontrolü
- Şirket takvimi entegrasyonu
- Microsoft Teams entegrasyonu
- Slack entegrasyonu
- Doküman yönetim sistemi
- Sesli asistan
- Yapay zeka destekli analitik
- Tahmine dayalı servis optimizasyonu
- Yapay zeka destekli İK asistanı

---

# 13. Başarı Kriterleri

Proje aşağıdaki koşullar sağlandığında başarılı kabul edilecektir:

- Çalışanlar şirket bilgilerine tek bir uygulamadan erişebilir.
- Yapay zeka chatbotu şirketle ilgili soruları doğru şekilde yanıtlar.
- Çalışanlar kendi haftalık çalışma düzenlerini mobil uygulamadaki "My Work Schedule" / "Çalışma Düzenim" ekranından ve web uygulamasındaki `/my-schedule` sayfasından güncelleyebilir.
- Çalışma düzeni değişiklikleri DataContext ve localStorage üzerinden kalıcı hale gelir.
- Servis önerileri ulaşım verimliliğini artırır.
- Yöneticiler şirket kaynaklarını manuel elektronik tablolar olmadan yönetebilir.
- Anketler ve duyurular aracılığıyla çalışan katılımı artar.
- Sistem performansı fonksiyonel olmayan gereksinimleri karşılar.
- Platform ölçeklenebilir, güvenli ve kolay sürdürülebilir yapıdadır.
