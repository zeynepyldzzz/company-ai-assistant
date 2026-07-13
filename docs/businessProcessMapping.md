# İş Süreçleri Haritalama (Business Process Mapping)

## Yapay Zeka Destekli Şirket İçi Asistan — Mobil ve Web Uygulaması

> Kaynak: `requirementAnalysis2.md`. Bu doküman, gereksinim analizinde tanımlanan fonksiyonel gereksinimleri (FR) anlamlı iş süreçlerine ayrıştırır ve her sürecin aktörlerini, tetikleyicilerini, adımlarını, veri varlıklarını ve ilişkili gereksinimlerini tanımlar. Görsel akış için bkz. `businessProcessDiagram.html`.

---

## 1. Amaç ve Kapsam

Bu haritalama, 82 fonksiyonel gereksinimi (FR-01 – FR-82) **10 uçtan uca iş sürecine** ayırır. Her süreç, konuyla ilgili tüm işlemleri tek çatı altında toplar ve genellikle iki alt-süreçten oluşur:

- **Çalışan Alt-Süreci (Self-Service):** Çalışanın uygulama üzerinden gerçekleştirdiği işlemler.
- **Yönetici Alt-Süreci (Administrative):** Yöneticilerin (admin panel) bu süreci besleyen veri/içerik yönetimi işlemleri.

Bu yapı, önceki 20 süreçlik ayrıştırmadaki tüm adım, gereksinim ve veri varlığı detayını korur; yalnızca çalışan↔yönetici çiftlerini (ör. eski "Yemek Menüsü Görüntüleme" + "Yemek Menüsü Yükleme") ve doğrudan bağımlı süreçleri (ör. Chatbot + Bilgi Tabanı Yönetimi) tek bir sürecin alt-akışları haline getirerek üst düzey sayıyı azaltır. Hiçbir FR veya adım içerikten çıkarılmamıştır.

---

## 2. Süreç Kategorileri

| Kategori                                       | Kod   | Açıklama                                                    |
| ---------------------------------------------- | ----- | ----------------------------------------------------------- |
| A. Kimlik Doğrulama ve Ana Panel               | BP-01 | Girişe erişim + kişiselleştirilmiş giriş noktası            |
| B. Yapay Zeka Chatbot ve Bilgi Tabanı Yönetimi | BP-02 | Genel/İK soruları ile bu soruları besleyen doküman yönetimi |
| C. Yemek Menüsü                                | BP-03 | Görüntüleme + yükleme/yönetim                               |
| D. Ulaşım (Servis)                             | BP-04 | Görüntüleme+öneri + güzergah/plaka yönetimi                 |
| E. Rehberler (Çalışan / Departman / Telefon)   | BP-05 | Arama ve filtreleme                                         |
| F. Şirket Araçları                             | BP-06 | Rezervasyon + araç yönetimi                                 |
| G. Anketler ve Geri Bildirim                   | BP-07 | Katılım + oluşturma/sonuç görüntüleme                       |
| H. Duyurular ve Bildirimler                    | BP-08 | Alma + yayımlama/yönetim                                    |
| I. Haftalık Çalışma Düzeni                     | BP-09 | Çalışan self-servis + yönetici izleme                       |
| J. Çalışan, Departman ve Sistem Yönetimi       | BP-10 | Admin CRUD + kullanıcı izinleri ve raporlama                |

---

## 3. Süreç Envanteri (Özet Tablo)

| ID    | Süreç Adı                                                         | Aktörler                                                     | Tetikleyici                                                   | İlişkili Gereksinimler  |
| ----- | ----------------------------------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------- | ----------------------- |
| BP-01 | Kimlik Doğrulama ve Kişiselleştirilmiş Ana Panel                  | Çalışan, Yönetici, Kimlik Doğrulama API'si                   | Uygulama açılışı → başarılı giriş                             | FR-01–07, NFR-03,04,07  |
| BP-02 | Yapay Zeka Chatbot (Genel + İK/Prosedür) ve Bilgi Tabanı Yönetimi | Çalışan, Yönetici (İK/Sistem Yöneticisi), Yapay Zeka Servisi | Chatbot açma / soru sorma ↔ politika-doküman güncellemesi     | FR-08–16, 51–58, 77–78  |
| BP-03 | Yemek Menüsü Görüntüleme ve Yönetimi                              | Çalışan, Yönetici (Yemekhane Yönetimi)                       | Menü sekmesini açma ↔ yeni menü hazır olduğunda               | FR-17–21, 72            |
| BP-04 | Servis (Ulaşım) Bilgisi, Güzergah Önerisi ve Yönetimi             | Çalışan, Yönetici (Servis Koordinatörü), Harita API'si       | Ulaşım sekmesini açma ↔ güzergah/plaka değişikliği            | FR-22–27, 73            |
| BP-05 | Çalışan / Departman / Telefon Rehberi Arama                       | Çalışan                                                      | Arama yapma                                                   | FR-28–37, 48–50         |
| BP-06 | Şirket Aracı Rezervasyonu ve Araç Yönetimi                        | Çalışan, Yönetici (Araç Sorumlusu)                           | Araç ihtiyacı ↔ filo/bakım güncellemesi                       | FR-38–41, 74            |
| BP-07 | Anket Katılımı, Geri Bildirim ve Anket Yönetimi                   | Çalışan, Yönetici                                            | Anket bildirimi ↔ katılım/sonuç ihtiyacı                      | FR-42–44, 76            |
| BP-08 | Duyuru ve Bildirim Görüntüleme, Yayımlama ve Yönetimi             | Çalışan, Yönetici                                            | Yeni duyuru yayınlanması ↔ duyurulacak bilgi                  | FR-45–47, 65–67, 75, 80 |
| BP-09 | Haftalık Çalışma Düzeni Yönetimi ve İzleme                        | Çalışan, Yönetici                                            | Haftalık planlama ihtiyacı ↔ raporlama/izleme ihtiyacı        | FR-59–64, 79            |
| BP-10 | Çalışan/Departman Yönetimi, Kullanıcı İzinleri ve Raporlama       | Yönetici (İK), Yönetici/Sistem Yöneticisi                    | İK organizasyon değişikliği / yetkilendirme veya rapor talebi | FR-68–71, 80–82         |

---

## 4. Detaylı Süreç Tanımları

### BP-01 — Kimlik Doğrulama ve Kişiselleştirilmiş Ana Panel

- **Aktörler:** Çalışan, Yönetici, Kimlik Doğrulama API'si
- **Veri Varlıkları:** Roller, İzinler

**Çalışan/Yönetici Alt-Süreci — Kimlik Doğrulama ve Oturum Açma**

- **Tetikleyici:** Kullanıcının uygulamayı açması
- **Girdi:** Kurumsal kimlik bilgileri
- **Adımlar:** 1) Kullanıcı giriş bilgilerini girer → 2) Sistem, şirketin kimlik doğrulama sistemine yönlendirir/doğrular → 3) Yönetici ise 2FA istenir (NFR-07) → 4) Oturum başlatılır ve rol bilgisi (RBAC) yüklenir → 5) Hata durumunda tekrar giriş istenir.
- **Çıktı:** Aktif kullanıcı oturumu, rol bazlı erişim
- **İş Kuralı:** Güvenli kimlik doğrulama ve rol tabanlı erişim kontrolü zorunludur (NFR-03, NFR-04).
- **İlişkili FR:** FR-01–03, NFR-03,04,07

**Çalışan Alt-Süreci — Kişiselleştirilmiş Ana Panel Görüntüleme**

- **Tetikleyici:** Başarılı oturum açma
- **Adımlar:** 1) Sistem çalışan profiline göre panel oluşturur → 2) Hızlı erişim kartları gösterilir → 3) Bekleyen bildirimler gösterilir → 4) Profil bilgisi görüntülenir.
- **Çıktı:** Kişiselleştirilmiş dashboard
- **İlişki:** Tüm diğer çalışan süreçlerinin (BP-02 – BP-09) giriş noktasıdır.
- **İlişkili FR:** FR-04–07

---

### BP-02 — Yapay Zeka Chatbot (Genel + İK/Prosedür) ve Bilgi Tabanı Yönetimi

- **Aktörler:** Çalışan, Yönetici (İK/Sistem Yöneticisi), Yapay Zeka Servisi
- **Veri Varlıkları:** Sohbet geçmişi, Yüklenen dokümanlar, İK prosedürleri, İzin politikaları, Fazla mesai politikaları, Politika versiyonları
- **İş Kuralı (kritik):** Yanıtlar yalnızca onaylı ve güncel dokümanlara dayanmalıdır; bu, Yönetici Alt-Süreci'nde yönetilen bilgi kaynağının doğrudan çıktısıdır.

**Çalışan Alt-Süreci A — Yapay Zeka Chatbot ile Genel Soru-Cevap**

- **Tetikleyici:** Chatbot ekranının açılması ve soru girilmesi (metin veya sesli)
- **Adımlar:** 1) Kullanıcı soru sorar (yazılı/sesli - FR-16) → 2) Gerekiyorsa dosya yükler (FR-15) → 3) Chatbot, şirket içi bilgi tabanından ilgili veriyi getirir (FR-10) → 4) Yanıt yalnızca onaylı şirket kaynaklarına dayandırılır (FR-14) → 5) Yanıt kullanıcıya döner (NFR-02: 5 sn içinde).
- **Çıktı:** Soruya yanıt / yönlendirme
- **İlişkili FR:** FR-08–10, 14–16

**Çalışan Alt-Süreci B — İK Prosedürü / Politika Sorgulama**

- **Tetikleyici:** "Bu prosedüre göre nasıl izin alabilirim?" gibi prosedür bazlı soru (özellikle yeni işe başlayanlar)
- **Adımlar:** 1) Kullanıcı prosedür/İK sorusu sorar → 2) Sistem işe giriş prosedürlerini (oryantasyon, çalışma saatleri, departman iletişimi, iç kaynaklar) tanımlar (FR-51–52) → 3) İzin türleri (yıllık, mazeret, hastalık) hakkında bilgi verir (FR-53) → 4) Adım adım yönlendirme sunar (FR-54) → 5) Fazla mesai talep/onay ve kullanım kurallarını açıklar (FR-55) → 6) Mazeret izni koşul/belge/başvuru adımlarını gösterir (FR-56) → 7) Sorumlu departman/kişiyi belirtir (FR-57) → 8) Yanıt, güncel doküman versiyonuna ve geçerlilik tarihine dayanır (FR-58).
- **Çıktı:** Prosedüre dayalı yönlendirme, sorumlu iletişim bilgisi
- **İlişkili FR:** FR-11–13, 51–58

**Yönetici Alt-Süreci — Chatbot Bilgi Tabanı ve İK Doküman Yönetimi**

- **Tetikleyici:** Politika/prosedür güncellemesi
- **Adımlar:** 1) Chatbot bilgi tabanı içeriği güncellenir (FR-77) → 2) İK prosedür dokümanları ve politika versiyonları yüklenir/güncellenir; geçerlilik tarihleri ve ekler saklanır (FR-78, FR-58) → 3) Yalnızca yetkili İK/sistem yöneticileri güncelleyebilir (Kısıt).
- **Çıktı:** Güncel, onaylı bilgi kaynağı
- **Bağımlılık:** Çalışan Alt-Süreçleri A ve B'yi doğrudan besler (chatbot yanıtlarının doğruluğunun kaynağıdır) — sistemin en kritik veri bağımlılığı.
- **İlişkili FR:** FR-77–78

---

### BP-03 — Yemek Menüsü Görüntüleme ve Yönetimi

- **Aktörler:** Çalışan, Yönetici (Yemekhane Yönetimi)
- **Veri Varlıkları:** Yemek menüleri, Yemekler

**Çalışan Alt-Süreci — Yemek Menüsü Görüntüleme**

- **Tetikleyici:** Menü sekmesini açma
- **Adımlar:** 1) Bugünün menüsü gösterilir (FR-17) → 2) Haftalık menüye geçiş yapılabilir (FR-18) → 3) Kalori (FR-19) ve alerjen (FR-20) bilgileri görüntülenir.
- **İlişkili FR:** FR-17–20

**Yönetici Alt-Süreci — Yemek Menüsü Yükleme**

- **Tetikleyici:** Yeni menü hazır olduğunda
- **Adımlar:** 1) Yönetici Excel menü dosyası hazırlar → 2) Önceden tanımlı şablona göre yükler (FR-21, Kısıt) → 3) Sistem doğrular ve Çalışan Alt-Süreci'ne yansıtır.
- **İlişkili FR:** FR-21, 72

---

### BP-04 — Servis (Ulaşım) Bilgisi, Güzergah Önerisi ve Yönetimi

- **Aktörler:** Çalışan, Yönetici (Servis Koordinatörü), Harita API'si
- **Veri Varlıkları:** Servis güzergahları, Servis durakları

**Çalışan Alt-Süreci — Servis Bilgisi Görüntüleme ve Güzergah Önerisi**

- **Tetikleyici:** Ulaşım sekmesini açma
- **Adımlar:** 1) Servis güzergahları listelenir (FR-22) → 2) Duraklar gösterilir (FR-23) → 3) Saatler gösterilir (FR-24) → 4) Güncel plaka bilgisi gösterilir (FR-25) → 5) Çalışanın varış noktasına göre en uygun güzergah önerilir (FR-26, GPS gerektirir) → 6) Tahmini varış süresi hesaplanır (FR-27).
- **İlişkili FR:** FR-22–27

**Yönetici Alt-Süreci — Servis Güzergahı ve Plaka Yönetimi**

- **Tetikleyici:** Güzergah/durak/saat veya plaka değişikliği
- **Adımlar:** 1) Güzergah/durak/saat bilgisi güncellenir → 2) Plaka bilgisi güncellenir (FR-73, FR-25 kaynağı).
- **İlişkili FR:** FR-73

---

### BP-05 — Çalışan / Departman / Telefon Rehberi Arama

- **Aktörler:** Çalışan
- **Tetikleyici:** Arama yapma
- **Adımlar:** 1) Kullanıcı çalışan arar (FR-28) ve filtreler (FR-29) → 2) Çalışan bilgisi, ofis durumu, telefon, e-posta gösterilir (FR-30–33) → 3) Alternatif olarak departman aranır; sorumluluklar, yönetici, iletişim bilgisi gösterilir (FR-34–37) → 4) Telefon rehberinden dahili arama yapılır (FR-48–50).
- **Veri Varlıkları:** Çalışanlar, Departmanlar
- **İlişkili FR:** FR-28–37, 48–50

---

### BP-06 — Şirket Aracı Rezervasyonu ve Araç Yönetimi

- **Aktörler:** Çalışan, Yönetici (Araç Sorumlusu)
- **Veri Varlıkları:** Araçlar, Rezervasyonlar

**Çalışan Alt-Süreci — Şirket Aracı Rezervasyonu**

- **Tetikleyici:** Araç ihtiyacı
- **Adımlar:** 1) Uygun araçlar listelenir (FR-38) → 2) Kullanıcı rezervasyon yapar (FR-39) → 3) Rezervasyon durumu görüntülenir (FR-40) → 4) Bakım durumundaki araçlar filtrelenir (FR-41).
- **İlişkili FR:** FR-38–41

**Yönetici Alt-Süreci — Araç Yönetimi**

- **Tetikleyici:** Filo/bakım güncellemesi
- **Adımlar:** Araç ekleme/güncelleme, bakım durumu işaretleme (FR-74) → Çalışan Alt-Süreci'ne yansır.
- **İlişkili FR:** FR-74

---

### BP-07 — Anket Katılımı, Geri Bildirim ve Anket Yönetimi

- **Aktörler:** Çalışan, Yönetici
- **Veri Varlıkları:** Anketler, Anket yanıtları

**Çalışan Alt-Süreci — Ankete Katılım ve Geri Bildirim Gönderme**

- **Tetikleyici:** Anket bildirimi
- **Adımlar:** 1) Kullanıcı anketi görür ve katılır (FR-42) → 2) Anonim geri bildirim gönderebilir (FR-43).
- **İlişkili FR:** FR-42–43

**Yönetici Alt-Süreci — Anket Oluşturma ve Sonuç Görüntüleme**

- **Tetikleyici:** Katılım ihtiyacı
- **Adımlar:** 1) Anket oluşturulur (FR-76) → 2) Yayımlanır → 3) Yetkili kullanıcılar sonuçları görüntüler (FR-44).
- **İlişkili FR:** FR-44, 76

---

### BP-08 — Duyuru ve Bildirim Görüntüleme, Yayımlama ve Yönetimi

- **Aktörler:** Çalışan, Yönetici
- **Veri Varlıkları:** Duyurular, Bildirimler

**Çalışan Alt-Süreci — Duyuru Görüntüleme ve Bildirim Alma**

- **Tetikleyici:** Yeni duyuru yayınlanması
- **Adımlar:** 1) Duyurular listelenir, önemli olanlar sabitlenir (FR-45, 47) → 2) Anlık bildirim / acil durum bildirimi alınır (FR-46, 65–66) → 3) Kullanıcı bildirim tercihlerini yönetir (FR-67).
- **İlişkili FR:** FR-45–47, 65–67

**Yönetici Alt-Süreci — Duyuru Yayımlama ve Bildirim Yönetimi**

- **Tetikleyici:** Duyurulacak bilgi
- **Adımlar:** 1) Duyuru oluşturulur ve yayımlanır (FR-75) → 2) Bildirimler yönetilir (FR-80 kapsamında bildirim/izin yönetimi) → 3) Çalışan Alt-Süreci'ne yansır.
- **İlişkili FR:** FR-75, 80

---

### BP-09 — Haftalık Çalışma Düzeni Yönetimi ve İzleme

- **Aktörler:** Çalışan, Yönetici
- **Veri Varlıkları:** Çalışan haftalık çalışma düzeni (`weeklySchedule`), Günlük çalışma durumu (`office`, `remote`, `leave`)
- **İş Kuralı (kritik):** Veri tek kaynaktan (single source of truth) sunulmalı; DataContext, localStorage, admin paneli ve diğer ekranlar arasında tutarlı olmalıdır (FR-64, Kısıt).

**Çalışan Alt-Süreci — Haftalık Çalışma Düzeni Yönetimi (My Work Schedule)**

- **Tetikleyici:** Haftalık planlama ihtiyacı / takvim ikonuna tıklama
- **Adımlar:** 1) Çalışan "My Work Schedule" (mobil) veya `/my-schedule` (web) ekranını açar → 2) Pazartesi–Cuma için "Ofiste", "Uzaktan" veya "İzinli" seçilir (FR-60) → 3) Haftalık özet gösterilir (ofis/uzaktan/izin gün sayıları) (FR-61) → 4) Seçim kaydedilir ve kalıcı olarak saklanır (FR-62) → 5) Yalnızca giriş yapan çalışanın kendi verisi gösterilir/düzenlenir (FR-63).
- **Çıktı:** Güncellenmiş `weeklySchedule` verisi
- **İlişkili FR:** FR-59–64

**Yönetici Alt-Süreci — Çalışma Düzeni İzleme**

- **Tetikleyici:** Raporlama/izleme ihtiyacı
- **Adımlar:** 1) Yönetici "Çalışma Düzenleri" tablosunda tüm çalışanların verisini görüntüler (FR-79) → 2) Çalışan Alt-Süreci'ndeki tek kaynak veriye salt-okunur erişir.
- **İlişkili FR:** FR-79

---

### BP-10 — Çalışan/Departman Yönetimi, Kullanıcı İzinleri ve Raporlama

- **Aktörler:** Yönetici (İK), Yönetici/Sistem Yöneticisi
- **Veri Varlıkları:** Çalışanlar, Departmanlar, Roller, İzinler

**Yönetici Alt-Süreci A — Çalışan ve Departman Yönetimi**

- **Tetikleyici:** İK organizasyon değişikliği
- **Adımlar:** 1) Çalışan oluşturma (FR-68) → 2) Güncelleme (FR-69) → 3) Silme (FR-70) → 4) Departman yönetimi (FR-71).
- **İlişkili FR:** FR-68–71

**Yönetici Alt-Süreci B — Kullanıcı İzinleri ve Raporlama**

- **Tetikleyici:** Yetkilendirme veya rapor talebi
- **Adımlar:** 1) Kullanıcı izinleri/rolleri yönetilir (FR-80, RBAC) → 2) Rapor oluşturulur (FR-81) → 3) Rapor dışa aktarılır (FR-82).
- **İlişkili FR:** FR-80–82

---

## 5. Süreçler Arası Bağımlılık Notları

- **BP-02 (Yönetici Alt-Süreci) → BP-02 (Çalışan Alt-Süreçleri A/B):** Chatbot'un doğru ve onaylı yanıt verebilmesi, bilgi tabanı ve İK doküman yönetiminin güncel olmasına bağlıdır. Bu, sistemin en kritik veri bağımlılığıdır.
- **BP-09 (Çalışan ↔ Yönetici Alt-Süreçleri):** Aynı `weeklySchedule` verisi üzerinde çalışan yazma ve yönetici okuma süreçleridir; tek kaynak ilkesi ihlal edilmemelidir.
- **BP-03/BP-04/BP-06/BP-07/BP-08 (Yönetici Alt-Süreçleri) → (Çalışan Alt-Süreçleri):** Her sürecin admin içerik yönetimi alt-akışı, aynı sürecin çalışan görüntüleme alt-akışının veri kaynağıdır.
- **BP-01**, tüm diğer süreçlerin önkoşuludur (guard/gate süreç).

---

## 6. İlişkili Doküman

Bu haritalamaya karşılık gelen görsel akış şemaları için bkz. [businessProcessDiagram.html](businessProcessDiagram.html) — genel uçtan uca akış, chatbot/İK sorgulama alt süreci, haftalık çalışma düzeni alt süreci ve yönetim (admin) içerik yönetimi alt süreci diyagramlarını içerir.
