# Issue Backlog — Faz 2
## Mobil Uygulama + Chatbot L2 (4 Hafta / 3 Full-Stack Geliştirici)

> Kaynak: `sprintPlanPhase2.md`, `sprintPlan.md`/`issue.md` (Faz 1), `businessProcessMapping.md`, `apiEndpoints.md`, `erDiagram.md`, `tech_stack.md`. Bu doküman `sprintPlanPhase2.md`'deki haftalık planı atanabilir issue seviyesine indirger. Faz 1'de kurulan backend API'lerin büyük kısmı burada **yeniden kullanılır** — Faz 2'nin asıl işi mobil istemci katmanı ve Chatbot'un RAG/LLM altyapısıdır.

---

## 0. Story Point Ölçeği

| SP | Anlamı |
|---|---|
| 1 | Birkaç saatlik, tek katmanlı basit iş |
| 2 | Yarım gün |
| 3 | ~1 gün, tek katmanlı ya da çok basit uçtan uca iş |
| 5 | 2-3 gün, standart uçtan uca iş |
| 8 | 3-4 gün, karmaşık/riskli veya çok adımlı iş |
| 13 | Neredeyse bir hafta, yüksek belirsizlik içeren iş |

## 1. Etiket Lejantı

`sprint-0` · `mobile` · `backend` · `fullstack` · `rag` · `llm` · `chatbot` · `push` · `maps` · `voice` · `storage` · `directory` · `shuttle` · `vehicle` · `menu` · `schedule` · `survey` · `announcement` · `admin` · `buffer`

---

## 2. Yük Dağılımı Özeti (Story Point)

| Geliştirici | Hafta 1 | Hafta 2 | Hafta 3 | Hafta 4 | **Toplam (Hafta 1-4)** | Hafta 0 | **Genel Toplam** |
|---|---|---|---|---|---|---|---|
| **A** — Chatbot L2 (RAG + LLM) | 16 | 13 | 21 | 8 | **58** | 8 | **66** |
| **B** — Mobil: Auth/Dashboard/Rehber/Servis+Maps/Araç (çalışan + admin) | 10 | 16 | 13 | 16 | **55** | 15 | **70** |
| **C** — Mobil: Menü/My Schedule/Anket/Duyuru+Push (çalışan + admin) | 11 | 15 | 18 | 19 | **63** | 8 | **71** |

**Hafta 0 (Ortak Temel):** 31 SP, tüm ekip (2-3 gün).

**Admin mobil ekranlarının eklenmesiyle** (bkz. `sprintPlanPhase2.md` Bölüm 1) B ve C'nin toplamı önemli ölçüde arttı (B: 39→55, C: 42→63, Hafta 1-4). Genel toplamda üç geliştirici artık birbirine yakın (66/70/71) — A hâlâ en riskli tek modülü taşıyor, B ve C ise hem çalışan hem admin mobil ekranlarının toplam hacmiyle dengeleniyor. **Hafta 3 ve 4, B ve C için en yoğun haftalar** (13-19 SP/hafta) çünkü admin ekranları çoğunlukla ilgili çalışan ekranından sonra, aynı domain'in devamı olarak bu haftalara yerleştirildi. Bu haftalarda buffer neredeyse yok — `sprintPlanPhase2.md` Bölüm 5'teki fallback sırası (admin mobil ekranları önce kesilir) bu riski karşılamak için var.

---

## 3. Sprint 0 — Ortak Temel (Tüm Ekip, Hafta 0, ~2-3 gün)

### F2-ISSUE-000: Expo Mobil App İskeleti
**Etiketler:** `sprint-0`, `mobile`
**Önerilen Atanan:** Geliştirici B
**Story Point:** 5

**Açıklama:** `apps/mobile` klasörü pnpm workspace'e eklenir (Expo/React Native). NativeWind kurulumu, React Navigation (tab + stack navigator: Ana Panel, Chatbot, Menü, Servis, Rehber, Araç, Anket, Duyuru, My Schedule sekmeleri). Ortak component kütüphanesi (`packages/shared`'daki Zod şemaları web ile paylaşılır).

**Kabul Kriterleri:**
- [ ] `pnpm --filter mobile start` ile Expo geliştirme sunucusu ayağa kalkıyor
- [ ] Tab navigasyonu (boş placeholder ekranlarla) çalışıyor
- [ ] NativeWind ile temel stil (renk paleti, tipografi) web ile tutarlı
- [ ] `packages/shared`'daki tipler/şemalar mobil projeden import edilebiliyor

**Bağımlılık:** Yok (Faz 1 ISSUE-000 monorepo yapısının üzerine eklenir)

---

### F2-ISSUE-001: Mobil Auth Entegrasyonu
**Etiketler:** `sprint-0`, `mobile`, `auth`
**Önerilen Atanan:** Geliştirici B
**Story Point:** 5

**Açıklama:** Login ekranı, `expo-secure-store` ile token saklama, auth guard (token yoksa login'e yönlendirme), session refresh. Mevcut `/auth/login`, `/auth/refresh`, `/auth/session`, `/auth/logout` endpoint'lerini yeniden kullanır — **yeni backend işi gerekmiyor**. Ana Panel (Dashboard) ekranının iskeleti de bu issue kapsamında (hızlı erişim kartları, bekleyen bildirimler). **Admin ekranları da mobile taşındığı için** (bkz. `sprintPlanPhase2.md`), rol bazlı navigasyon (employee/`hr_admin`/`fleet_admin`/`shuttle_admin`/`canteen_admin`/`system_admin`) ve admin girişinde 2FA (`/auth/2fa/verify`, NFR-07) bu issue'nun kapsamına girer.

**Kabul Kriterleri:**
- [ ] Kullanıcı mobilde giriş yapabiliyor, token güvenli şekilde saklanıyor
- [ ] Token süresi dolduğunda otomatik refresh çalışıyor
- [ ] Auth guard, token yoksa login ekranına yönlendiriyor
- [ ] Dashboard ekranı `GET /dashboard` ile hızlı erişim kartlarını gösteriyor
- [ ] Admin rolündeki kullanıcı girişte 2FA adımını tamamlamadan admin mobil ekranlarına erişemiyor (NFR-07)
- [ ] Mobil navigasyon rol bazlı — her admin alt tipi yalnızca kendi yönetim ekranlarını görüyor, employee hiçbirini görmüyor
- [ ] FR-01–07 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-000

---

### F2-ISSUE-002: Push Notification Altyapısı
**Etiketler:** `sprint-0`, `mobile`, `backend`, `push`
**Önerilen Atanan:** Geliştirici C
**Story Point:** 5

**Açıklama:** Firebase FCM projesi kurulumu, `expo-notifications` ile cihaz push token'ı alma. Backend'e **yeni endpoint**: `POST /me/device-tokens` (kayıt), `DELETE /me/device-tokens/{token}` (kaldırma). Bu endpoint `apiEndpoints.md`'deki 71 uca ek olarak Faz 2'de eklenir.

**Kabul Kriterleri:**
- [ ] Mobil app açıldığında push izni istiyor, izin verilirse FCM token backend'e kaydediliyor
- [ ] `POST /me/device-tokens` token'ı `EMPLOYEE` ile ilişkilendirip saklıyor
- [ ] Test push bildirimi (Firebase console'dan) cihaza ulaşıyor
- [ ] Uygulama silindiğinde/çıkış yapıldığında token temizleniyor

**Bağımlılık:** F2-ISSUE-001 (kullanıcı kimliğiyle ilişkilendirme için)

---

### F2-ISSUE-003: Location / Maps Altyapısı
**Etiketler:** `sprint-0`, `mobile`, `maps`
**Önerilen Atanan:** Geliştirici B
**Story Point:** 5

**Açıklama:** `expo-location` + `expo-task-manager` izin akışı (ön planda ve arka planda konum izni), Google Maps API proje/key kurulumu (Directions API, Geocoding API aktivasyonu).

**Kabul Kriterleri:**
- [ ] Konum izni istemi doğru şekilde gösteriliyor (ön plan + arka plan)
- [ ] `expo-location` ile anlık konum alınabiliyor
- [ ] Google Maps API key kısıtlamalarıyla (paket adı/bundle ID) birlikte kuruldu
- [ ] Test isteğiyle Directions API'den örnek bir rota yanıtı alındı

**Bağımlılık:** F2-ISSUE-000

---

### F2-ISSUE-004: RAG / AI Altyapısı
**Etiketler:** `sprint-0`, `backend`, `rag`, `llm`, `storage`
**Önerilen Atanan:** Geliştirici A
**Story Point:** 8

**Açıklama:** Ollama'nın Docker Compose'a container olarak eklenmesi, Phi-4 Mini / Qwen 2.5 modelinin çekilmesi ve servis edilmesi. `POLICY_EMBEDDING` tablosuna yazacak embedding job iskeleti (bge-m3/multilingual-e5-large ile chunk + embed). Dosya yükleme (FR-15) ve KB doküman ekleri için MinIO container'ının aktive edilmesi (`tech_stack.md`'de "Future" işaretliydi).

**Kabul Kriterleri:**
- [ ] `docker compose up` ile Ollama container'ı ayağa kalkıyor, seçilen model yüklü
- [ ] Backend'den Ollama'ya örnek bir prompt gönderilip yanıt alınabiliyor
- [ ] MinIO container'ı ayakta, backend'den dosya yükleme/indirme testi başarılı
- [ ] Embedding job iskeleti kuruldu (henüz toplu çalıştırılmadı, Hafta 1'de A2-2'de tamamlanacak)

**Bağımlılık:** Faz 1 ISSUE-001 (PostgreSQL + pgvector extension)

---

### F2-ISSUE-005: CI Güncellemesi (Mobil Build Pipeline)
**Etiketler:** `sprint-0`, `mobile`, `infra`
**Önerilen Atanan:** Geliştirici C
**Story Point:** 3

**Açıklama:** GitHub Actions'a mobil için EAS (Expo Application Services) build/test job'ı eklenir. PR'da en azından lint + TypeScript check; ana branch'e merge'de opsiyonel EAS preview build.

**Kabul Kriterleri:**
- [ ] PR açıldığında mobil app için lint + tsc çalışıyor
- [ ] Ana branch'e merge'de EAS build tetiklenebiliyor (manuel veya otomatik)

**Bağımlılık:** F2-ISSUE-000, Faz 1 ISSUE-004

---

## 4. Geliştirici A — Chatbot L2 (RAG + LLM)

### Hafta 1

#### A2-1: Ollama LLM Entegrasyonu (Backend)
**Etiketler:** `backend`, `llm`, `chatbot`
**Story Point:** 8

**Açıklama:** Backend'den Ollama'ya prompt gönderme servisi: sistem prompt tasarımı ("yalnızca verilen bağlama dayan, uydurma" — FR-14 kısıtı), context enjeksiyonu için placeholder, timeout ve hata durumunda fallback (L1 template yanıtına düşme).

**Kabul Kriterleri:**
- [ ] Backend, Ollama'dan senkron/stream yanıt alabiliyor
- [ ] Sistem prompt, yanıtların yalnızca verilen bağlama dayanmasını zorluyor (FR-14)
- [ ] Ollama zaman aşımına uğrarsa L1 template fallback'i devreye giriyor
- [ ] FR-08–10 karşılanıyor (L2 seviyesinde)

**Bağımlılık:** F2-ISSUE-004

---

#### A2-2: PolicyVersion Embedding Pipeline
**Etiketler:** `backend`, `rag`
**Story Point:** 8

**Açıklama:** Mevcut `PolicyVersion.content` alanının chunklanması (ör. 500-800 token'lık parçalar) ve her chunk için embedding üretilip `POLICY_EMBEDDING` tablosuna yazılması. Yeni bir `PolicyVersion` yayımlandığında (Faz 1'deki A-6 admin CRUD akışı) otomatik embedding tetiklenmeli.

**Kabul Kriterleri:**
- [ ] Mevcut tüm `PolicyVersion` kayıtları için toplu embedding job'ı çalıştırılabiliyor
- [ ] Yeni versiyon yayımlandığında embedding otomatik oluşuyor
- [ ] Chunk boyutu ve overlap parametreleri yapılandırılabilir
- [ ] FR-58, 77–78 ile uyumlu (versiyon bazlı embedding)

**Bağımlılık:** F2-ISSUE-004, Faz 1 A-6

---

### Hafta 2

#### A2-3: RAG Retrieval Servisi
**Etiketler:** `backend`, `rag`
**Story Point:** 8

**Açıklama:** Kullanıcı sorusunun embedding'i ile `POLICY_EMBEDDING` üzerinde pgvector benzerlik araması (cosine similarity, top-k chunk). Bulunan chunk'ların LLM prompt'una context olarak enjekte edilmesi. A2-1 (LLM) ve A2-2 (embedding pipeline) burada birleşiyor.

**Kabul Kriterleri:**
- [ ] Bir soru için en alakalı top-5 chunk pgvector ile doğru şekilde getiriliyor
- [ ] Getirilen chunk'lar LLM prompt'una context olarak ekleniyor, yanıt üretiliyor
- [ ] Alakalı chunk bulunamazsa (düşük benzerlik skoru) "bu konuda bilgim yok" tarzı güvenli yanıt dönüyor (halüsinasyon engelleme, FR-14)
- [ ] FR-11–13, 51–58 karşılanıyor (L2 seviyesinde, gerçek RAG ile)

**Bağımlılık:** A2-1, A2-2

---

#### A2-4: Citation / Kaynak Gösterme
**Etiketler:** `backend`, `frontend`, `rag`, `chatbot`
**Story Point:** 5

**Açıklama:** Her RAG yanıtı için `CHAT_CITATION` kaydı oluşturulması — hangi `PolicyVersion`/chunk'tan yanıt üretildiği. Faz 1'deki A-9 (stretch, yapılmadıysa) kapsamının tam/zorunlu versiyonu.

**Kabul Kriterleri:**
- [ ] Her İK prosedür yanıtının altında "Kaynak: [doküman adı, versiyon, geçerlilik tarihi]" gösteriliyor
- [ ] `CHAT_CITATION.policyVersionId`, gerçekten kullanılan chunk'ın versiyonuna işaret ediyor
- [ ] Kaynağa tıklandığında (mobil/web) ilgili doküman detayına gidiliyor
- [ ] FR-14, 58 izlenebilirlik kısıtı karşılanıyor

**Bağımlılık:** A2-3

---

### Hafta 3

#### A2-5: Dosya Yükleyerek Soru Sorma (FR-15)
**Etiketler:** `backend`, `mobile`, `rag`, `storage`
**Story Point:** 8

**Açıklama:** `POST /chatbot/conversations/{id}/attachments` — kullanıcı bir dosya yükler (MinIO'ya kaydedilir), dosya içeriği geçici olarak chunklanıp embed edilir, o sohbet oturumu için RAG context'ine dahil edilir (diğer kullanıcıların bilgi tabanına karışmaz — oturum bazlı, izole).

**Kabul Kriterleri:**
- [ ] Kullanıcı mobil/web'den dosya (PDF/DOCX) yükleyebiliyor, MinIO'ya kaydediliyor
- [ ] Dosya içeriği chunklanıp embed ediliyor, yalnızca o sohbet oturumunda kullanılıyor
- [ ] Yüklenen dosyaya dair soru sorulduğunda yanıt dosya içeriğine dayanıyor
- [ ] Desteklenmeyen dosya formatında anlamlı hata (422) dönüyor
- [ ] FR-15 karşılanıyor

**Bağımlılık:** A2-3, F2-ISSUE-004 (MinIO)

---

#### A2-6: Sesli Giriş (FR-16)
**Etiketler:** `mobile`, `voice`, `chatbot`
**Story Point:** 5

**Açıklama:** `expo-speech-recognition` ile mobilde konuşmadan metne çevirme (cihaz üzerinde/on-device). Transkript edilen metin, mevcut `POST /chatbot/messages` akışına `inputType=voice` ile gönderilir — **yeni backend endpoint'i gerekmiyor**, `CHAT_MESSAGE.inputType` alanı zaten Faz 1 ER diagram'ında tanımlı.

**Kabul Kriterleri:**
- [ ] Mikrofon butonuna basılı tutarak konuşma metne çevriliyor
- [ ] Transkript, mesaj kutusuna otomatik doluyor, kullanıcı onaylayıp gönderiyor
- [ ] Gönderilen mesajın `inputType` alanı `voice` olarak kaydediliyor
- [ ] FR-16 karşılanıyor

**Bağımlılık:** F2-ISSUE-000, A2-1

---

#### A2-7: Chatbot Mobil UI
**Etiketler:** `mobile`, `chatbot`
**Story Point:** 8

**Açıklama:** Mesaj listesi, mesaj gönderme kutusu, sesli giriş butonu (A2-6), dosya ekleme butonu (A2-5), kaynak gösterimi (A2-4), sohbet geçmişi listesi.

**Kabul Kriterleri:**
- [ ] Mobilde yazılı/sesli soru sorulabiliyor, yanıt + kaynak gösteriliyor
- [ ] Dosya eklenip o dosyaya dair soru sorulabiliyor
- [ ] Sohbet geçmişi listelenip eski konuşmalar açılabiliyor
- [ ] Yanıt bekleme durumunda (LLM gecikmesi göz önüne alınarak) net bir loading/typing göstergesi var
- [ ] FR-08–10, 14–16 mobilde karşılanıyor

**Bağımlılık:** A2-3, A2-4, A2-5, A2-6, F2-ISSUE-001

---

### Hafta 4

#### A2-8: RAG Performans/Doğruluk Tuning, Entegrasyon, Demo
**Etiketler:** `buffer`, `rag`, `llm`
**Story Point:** 8

**Açıklama:** NFR-02 (5 sn yanıt) riski gerçek RAG+LLM ile yeniden değerlendirilir — gerekirse streaming yanıt (kullanıcı üretilen metni parça parça görür) ile algılanan hız iyileştirilir. Retrieval doğruluğu (yanlış/alakasız chunk getirme oranı) manuel test setiyle değerlendirilir. Demo senaryoları (yazılı soru, sesli soru, dosya yükleyerek soru, kaynak gösterme) hazırlanır.

**Kabul Kriterleri:**
- [ ] En az 20 soruluk test setinde RAG yanıtlarının doğruluğu manuel değerlendirildi
- [ ] Ortalama/95p yanıt süresi ölçüldü; NFR-02 karşılanamıyorsa streaming ile algılanan gecikme azaltıldı
- [ ] En az 4 uçtan uca demo senaryosu (yazılı/sesli/dosyalı soru + kaynak gösterme) prova edildi
- [ ] Kritik buglar kapatıldı

**Bağımlılık:** A2-1..A2-7

---

## 5. Geliştirici B — Mobil: Auth/Dashboard, Rehberler, Servis (+ Gerçek Maps), Araç Rezervasyonu (+ Admin: Servis/Araç Yönetimi, BP-10 Çalışan/Departman CRUD)

### Hafta 1

#### B2-1: Auth + Dashboard Mobil Ekranları
**Etiketler:** `mobile`
**Story Point:** 5

**Açıklama:** F2-ISSUE-001'in UI tamamlama işi: login ekranı görsel son hali, Dashboard'da hızlı erişim kartları (menü/servis/rehber/araç/anket/duyuru/schedule kısayolları), bekleyen bildirimler özeti, profil bilgisi.

**Kabul Kriterleri:**
- [ ] Dashboard'da tüm modüllere hızlı erişim kartları var
- [ ] Bekleyen bildirim sayısı/özeti gösteriliyor
- [ ] Profil ekranı `GET /me` verisini gösteriyor
- [ ] FR-04–07 karşılanıyor

**Bağımlılık:** F2-ISSUE-001

---

#### B2-2: Rehber Mobil Ekranları
**Etiketler:** `mobile`, `directory`
**Story Point:** 5

**Açıklama:** Çalışan/departman arama, telefon rehberi, click-to-call — Faz 1'deki B-1/B-2/B-3 API'lerinin mobil karşılığı.

**Kabul Kriterleri:**
- [ ] Çalışan arama + filtre + detay ekranı mobilde çalışıyor
- [ ] Departman arama + detay ekranı mobilde çalışıyor
- [ ] Telefon rehberinden tek dokunuşla arama (native `Linking.openURL('tel:...')`) tetikleniyor
- [ ] FR-28–37, 48–50 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-000, F2-ISSUE-001

---

### Hafta 2

#### B2-3: Servis Mobil Ekranı
**Etiketler:** `mobile`, `shuttle`
**Story Point:** 3

**Açıklama:** Güzergah listesi, durak/saat, güncel plaka — Faz 1'deki B-5 API'sinin mobil karşılığı.

**Kabul Kriterleri:**
- [ ] Güzergah listesi ve durak/saat detayı mobilde görüntüleniyor
- [ ] Güncel plaka bilgisi gösteriliyor
- [ ] FR-22–25 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-000

---

#### B2-4: Gerçek Google Maps Entegrasyonu (FR-27 Tam Versiyonu)
**Etiketler:** `backend`, `mobile`, `maps`, `shuttle`
**Story Point:** 8

**Açıklama:** Faz 1'deki B-6'nın basit Haversine mesafe hesabının yerine Google Maps Directions API ile gerçek rota ve trafik bazlı ETA hesaplaması. Backend'deki `GET /shuttle-routes/recommendation` servisinin Maps API çağrısı yapacak şekilde güncellenmesi.

**Kabul Kriterleri:**
- [ ] `GET /shuttle-routes/recommendation` artık Directions API'den gerçek süre/mesafe döndürüyor
- [ ] Mobil ekranda önerilen güzergah + gerçekçi ETA gösteriliyor
- [ ] Maps API çağrısı başarısız olursa Faz 1'deki basit hesaplamaya fallback yapılıyor (kesinti toleransı)
- [ ] FR-26, 27 tam olarak karşılanıyor

**Bağımlılık:** F2-ISSUE-003, B2-3

---

#### B2-5: Arka Plan Konum Takibi Entegrasyonu
**Etiketler:** `mobile`, `maps`
**Story Point:** 5

**Açıklama:** `expo-location` + `expo-task-manager` ile kullanıcının konumunu alıp B2-4'teki öneri servisine gönderme; arka planda çalışırken pil/performans etkisini minimize eden bir polling stratejisi (ör. sadece Servis ekranı açıkken aktif, sürekli arka plan takibi değil — FR'lerde sürekli canlı takip talebi yok).

**Kabul Kriterleri:**
- [ ] Konum izni verildiğinde güncel konum alınıp öneri servisine gönderiliyor
- [ ] Servis ekranı kapatıldığında konum takibi duruyor (gereksiz pil tüketimi yok)
- [ ] İzin reddedilirse manuel varış noktası girişi fallback'i var

**Bağımlılık:** F2-ISSUE-003, B2-4

---

### Hafta 3

#### B2-6: Araç Rezervasyon Mobil Ekranı
**Etiketler:** `mobile`, `vehicle`
**Story Point:** 5

**Açıklama:** Uygun araç listesi, rezervasyon formu, durum takibi, iptal — Faz 1'deki B-8 API'sinin mobil karşılığı.

**Kabul Kriterleri:**
- [ ] Uygun araçlar listeleniyor, bakımdakiler filtreleniyor
- [ ] Rezervasyon oluşturma/iptal mobilde çalışıyor
- [ ] Kullanıcı kendi rezervasyon durumunu görebiliyor
- [ ] FR-38–41 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-000, F2-ISSUE-001

---

#### B2-7: Servis Yönetimi Admin Mobil Ekranı
**Etiketler:** `mobile`, `shuttle`, `admin`
**Story Point:** 8

**Açıklama:** Güzergah/durak/saat oluşturma-güncelleme formu, plaka güncelleme — `POST/PUT /admin/shuttle-routes`, `PUT /admin/shuttle-routes/{id}/plate` endpoint'lerinin mobil karşılığı. Faz 1'deki B-5 API'sinin web admin ekranıyla aynı akış, bu kez mobilde. Yalnızca `shuttle_admin` rolüne görünür.

**Kabul Kriterleri:**
- [ ] `shuttle_admin` rolündeki kullanıcı mobil navigasyonda "Servis Yönetimi" bölümünü görüyor, `employee` görmüyor
- [ ] Güzergah/durak/saat oluşturma ve güncelleme formu mobilde çalışıyor
- [ ] Plaka bilgisi mobilden güncellenebiliyor
- [ ] FR-73, 25 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-001 (rol bazlı navigasyon), Faz 1 B-5

---

### Hafta 4

#### B2-8: Araç Yönetimi Admin Mobil Ekranı
**Etiketler:** `mobile`, `vehicle`, `admin`
**Story Point:** 5

**Açıklama:** Araç ekleme/güncelleme, bakım durumu işaretleme — `POST/PUT /admin/vehicles`, `PUT /admin/vehicles/{id}/maintenance-status` mobil karşılığı. Faz 1'deki B-9 (web) ile aynı akış. Yalnızca `fleet_admin` rolüne görünür.

**Kabul Kriterleri:**
- [ ] `fleet_admin` rolündeki kullanıcı araç ekleyip güncelleyebiliyor
- [ ] Bakım durumu mobilde tek dokunuşla değiştirilebiliyor
- [ ] `employee` rolü bu ekranı göremiyor
- [ ] FR-74 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-001, Faz 1 B-9

---

#### B2-9: Çalışan/Departman Yönetimi Admin Mobil Ekranı (BP-10)
**Etiketler:** `mobile`, `admin`
**Story Point:** 8

**Açıklama:** `POST/PUT/DELETE /admin/employees`, `POST/PUT/DELETE /admin/departments` mobil karşılığı — Faz 1'deki C-10 (web) ile aynı akış, mobilde. Yalnızca `hr_admin` rolüne görünür.

**Kabul Kriterleri:**
- [ ] `hr_admin` rolündeki kullanıcı çalışan oluşturabiliyor, güncelleyebiliyor, silebiliyor
- [ ] Departman oluşturma/güncelleme/silme mobilde çalışıyor
- [ ] `employee` rolü bu ekranı göremiyor
- [ ] FR-68–71 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-001, Faz 1 C-10

---

#### B2-10: Entegrasyon, Cross-Review, Bug Bash (iOS + Android)
**Etiketler:** `buffer`, `mobile`
**Story Point:** 3

**Açıklama:** Gerçek cihaz/simülatör testleri (hem iOS hem Android), A ve C'nin ekranlarıyla navigasyon/entegrasyon testi, TestFlight/Play Store internal testing kanallarının hazırlanması. **Not:** Admin mobil ekranlarının eklenmesiyle bu haftaya ayrılan buffer süresi Faz 1'e göre daralmış durumda (3 SP) — B2-7/B2-8/B2-9'da gecikme olursa bu issue ilk sıkışacak olan, gerekirse `sprintPlanPhase2.md` Bölüm 5'teki fallback sırası (admin mobil ekranları önce ertelenir) devreye alınmalı.

**Kabul Kriterleri:**
- [ ] Tüm B modülü ekranları (çalışan + admin) hem iOS hem Android'de manuel test edildi
- [ ] TestFlight (iOS) ve Play Store internal testing (Android) kanalları hazır, build yüklendi
- [ ] Cross-review'da bulunan kritik buglar kapatıldı

**Bağımlılık:** B2-1..B2-9

---

## 6. Geliştirici C — Mobil: Menü, My Schedule, Anket, Duyuru (+ Push, + Admin: Menü/Anket/Duyuru Yönetimi, BP-10 RBAC/Rapor)

### Hafta 1

#### C2-1: Menü Mobil Ekranı
**Etiketler:** `mobile`, `menu`
**Story Point:** 3

**Açıklama:** Bugün/haftalık sekme, kalori/alerjen gösterimi — Faz 1'deki C-1 API'sinin mobil karşılığı.

**Kabul Kriterleri:**
- [ ] Bugünün ve haftalık menü mobilde sekmeler arası geçişle gösteriliyor
- [ ] Kalori/alerjen bilgisi görünür
- [ ] FR-17–20 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-000

---

#### C2-2: My Schedule Mobil Ekranı
**Etiketler:** `mobile`, `schedule`
**Story Point:** 8

**Açıklama:** `requirementAnalysis2.md` bölüm 9'da özellikle belirtilen "My Work Schedule" ekranı ve takvim ikonlu menü öğesi. Pazartesi–Cuma gün durumu seçimi (Ofiste/Uzaktan/İzinli), haftalık özet — Faz 1'deki C-4 API'si (`/schedules/me`) ve C-5a'daki tasarım kararları temel alınır (Figma'da bu ekran yoktu, Faz 1'de mockup'ı hazırlanmıştı).

**Kabul Kriterleri:**
- [ ] Her iş günü için tek seçim yapılabiliyor, haftalık özet anlık güncelleniyor
- [ ] Kaydet `PUT /schedules/me`'yi çağırıyor, veri kalıcı
- [ ] Aynı veri web'deki `/my-schedule` ile birebir tutarlı (FR-64 tek kaynak — aynı endpoint'i okuyup yazıyor)
- [ ] Takvim ikonlu menü öğesi ana navigasyonda mevcut
- [ ] FR-59–64 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-000, F2-ISSUE-001, Faz 1 C-5a (tasarım referansı)

---

### Hafta 2

#### C2-3: Anket / Feedback Mobil Ekranı
**Etiketler:** `mobile`, `survey`
**Story Point:** 5

**Açıklama:** Aktif anketlere katılım, anonim feedback gönderme — Faz 1'deki C-7 API'sinin mobil karşılığı.

**Kabul Kriterleri:**
- [ ] Aktif anketler listeleniyor, yanıt gönderilebiliyor
- [ ] Anonim feedback formu (kullanıcı kimliği hiçbir şekilde gönderilmiyor) çalışıyor
- [ ] FR-42–43 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-000, F2-ISSUE-001

---

#### C2-4: Duyuru Mobil Ekranı
**Etiketler:** `mobile`, `announcement`
**Story Point:** 5

**Açıklama:** Duyuru listesi (sabitlenenler üstte) + detay — Faz 1'deki C-9 API'sinin mobil karşılığı.

**Kabul Kriterleri:**
- [ ] Duyuru listesinde sabitlenenler üstte gösteriliyor
- [ ] Duyuru detay ekranı çalışıyor
- [ ] FR-45, 47 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-000

---

#### C2-5: Yemek Menüsü Admin Mobil Ekranı (Excel İçe Aktarma)
**Etiketler:** `mobile`, `menu`, `admin`
**Story Point:** 5

**Açıklama:** `expo-document-picker` ile Excel dosyası seçip `POST /admin/menus/import`'a yükleme, menü kaydı kaldırma (`DELETE /admin/menus/{id}`) — Faz 1'deki C-2 backend'inin mobil karşılığı. Yalnızca `canteen_admin` rolüne görünür.

**Kabul Kriterleri:**
- [ ] `canteen_admin` rolündeki kullanıcı cihazdan Excel dosyası seçip yükleyebiliyor
- [ ] Hatalı/eksik şablon formatında mobilde anlamlı hata mesajı gösteriliyor (422)
- [ ] Menü kaydı mobilden kaldırılabiliyor
- [ ] `employee` rolü bu ekranı göremiyor
- [ ] FR-21, 72 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-001, Faz 1 C-2

---

### Hafta 3

#### C2-6: Push Bildirim Dispatch Entegrasyonu
**Etiketler:** `backend`, `push`, `announcement`
**Story Point:** 8

**Açıklama:** Bir duyuru yayımlandığında veya acil durum bildirimi tetiklendiğinde, `NOTIFICATION` kaydı oluşturmanın yanı sıra F2-ISSUE-002'de kurulan altyapı üzerinden gerçek FCM push bildirimi gönderilmesi. Bildirim türüne göre (`info`/`urgent`) farklı push önceliği.

**Kabul Kriterleri:**
- [ ] `POST /admin/announcements` çağrıldığında ilgili kullanıcılara gerçek push bildirimi gidiyor
- [ ] `urgent` tipi bildirimler yüksek öncelikli push olarak gönderiliyor (FR-66)
- [ ] Kullanıcı bildirim tercihlerinde kapattığı kategoriler için push gönderilmiyor
- [ ] FR-46, 65–66 karşılanıyor

**Bağımlılık:** F2-ISSUE-002, Faz 1 C-9

---

#### C2-7: Bildirim Listesi + Tercihleri Mobil Ekranı
**Etiketler:** `mobile`, `announcement`
**Story Point:** 5

**Açıklama:** `GET /notifications`, `PUT /notifications/preferences` mobil UI karşılığı.

**Kabul Kriterleri:**
- [ ] Bildirim listesi (okundu/okunmadı durumuyla) mobilde gösteriliyor
- [ ] Kullanıcı bildirim tercihlerini (kategori bazlı aç/kapa) mobilde güncelleyebiliyor
- [ ] FR-65–67 mobilde karşılanıyor

**Bağımlılık:** C2-6

---

#### C2-8: Duyuru Yönetimi Admin Mobil Ekranı
**Etiketler:** `mobile`, `announcement`, `admin`
**Story Point:** 5

**Açıklama:** `POST /admin/announcements`, `PUT /admin/announcements/{id}/pin` mobil karşılığı — Faz 1'deki C-9 (web) ile aynı akış. Yönetici rolüne görünür.

**Kabul Kriterleri:**
- [ ] Yönetici rolündeki kullanıcı duyuru oluşturup yayımlayabiliyor
- [ ] Duyuru sabitleme mobilde çalışıyor
- [ ] `employee` rolü bu ekranı göremiyor
- [ ] FR-75 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-001, Faz 1 C-9

---

### Hafta 4

#### C2-9: Anket Yönetimi Admin Mobil Ekranı
**Etiketler:** `mobile`, `survey`, `admin`
**Story Point:** 8

**Açıklama:** `POST /admin/surveys`, `PUT /admin/surveys/{id}/publish`, `GET /admin/surveys/{id}/results` mobil karşılığı — Faz 1'deki C-8 (web) ile aynı akış.

**Kabul Kriterleri:**
- [ ] Yönetici rolündeki kullanıcı anket oluşturup yayımlayabiliyor
- [ ] Anket sonuçları mobilde özet/grafik halinde görüntülenebiliyor
- [ ] `employee` rolü bu ekranı göremiyor
- [ ] FR-44, 76 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-001, Faz 1 C-8

---

#### C2-10: Rol/İzin Yönetimi (RBAC) + Rapor Mobil Ekranı (BP-10)
**Etiketler:** `mobile`, `admin`
**Story Point:** 8

**Açıklama:** `GET /admin/roles`, `PUT /admin/users/{id}/roles`, `GET /admin/reports/{type}`, `GET /admin/reports/{id}/export?format=xlsx|pdf` mobil karşılığı — Faz 1'deki C-11 (web) ile aynı akış. Mobilde rapor export'u native paylaşım sheet'i (`Share` API) üzerinden indirilir/paylaşılır. Yalnızca `system_admin` rolüne görünür.

**Kabul Kriterleri:**
- [ ] `system_admin` rolündeki kullanıcı mobilde rol atayabiliyor/kaldırabiliyor
- [ ] En az bir rapor tipi (`usage`) mobilde oluşturulup native share ile dışa aktarılabiliyor
- [ ] `employee` rolü bu ekranı göremiyor
- [ ] FR-80–82 mobilde karşılanıyor

**Bağımlılık:** F2-ISSUE-001, Faz 1 C-11

---

#### C2-11: Entegrasyon, Cross-Review, Bug Bash (iOS + Android)
**Etiketler:** `buffer`, `mobile`
**Story Point:** 3

**Açıklama:** Gerçek cihaz/simülatör testleri, A ve B'nin ekranlarıyla entegrasyon, push bildirimlerinin gerçek cihazda uçtan uca doğrulanması. **Not:** Admin mobil ekranlarının eklenmesiyle bu haftaya ayrılan buffer süresi Faz 1'e göre daralmış durumda (3 SP) — C2-8/C2-9/C2-10'da gecikme olursa bu issue ilk sıkışacak olan, gerekirse `sprintPlanPhase2.md` Bölüm 5'teki fallback sırası devreye alınmalı.

**Kabul Kriterleri:**
- [ ] Tüm C modülü ekranları (çalışan + admin) hem iOS hem Android'de manuel test edildi
- [ ] Push bildirimleri gerçek cihazda (uygulama arka planda/kapalıyken dahil) doğrulandı
- [ ] Cross-review'da bulunan kritik buglar kapatıldı

**Bağımlılık:** C2-1..C2-10

---

## 7. Kritik Riskler ve Bağımlılıklar

- **Kapsam büyüklüğü (en kritik risk):** Admin mobil ekranlarının eklenmesiyle B ve C'nin toplam yükü ~%40-50 arttı (bkz. Bölüm 2). Mobil parity (çalışan + admin) + RAG/LLM aynı 4 haftada bitirilmesi artık daha da sıkı bir hedef. `sprintPlanPhase2.md` Bölüm 5'teki fallback descope sırası **admin mobil ekranlarını en önce keser** — Hafta 2 sonunda gerçek ilerleme bu sıraya göre değerlendirilmeli.
- **B ve C için Hafta 3-4 aşırı yüklü (13-19 SP/hafta), buffer neredeyse yok:** B2-10 ve C2-11 (entegrasyon/bug-bash) 8 SP'den 3 SP'ye düşürüldü — bu, admin ekranlarında en ufak bir gecikmenin doğrudan test/entegrasyon süresinden çalınacağı anlamına gelir. Gerekirse A'dan (Hafta 4'te 8 SP ile daha hafif) destek planlanmalı.
- **FR-64 tek kaynak kuralı (C2-2):** Mobil `/schedules/me` çağrısı web ile birebir aynı endpoint'i kullanmalı; ayrı bir mobil-özel state/cache katmanı FR-64'ü ihlal eder.
- **NFR-02 riski:** RAG+LLM latency'si L1'den çok daha yüksek; A2-8'de gerçek ölçüm yapılmadan "5 sn karşılanıyor" varsayılmamalı.
- **Admin mobil 2FA:** F2-ISSUE-001'de admin rolleri için mobilde de 2FA zorunlu kılınmazsa NFR-07 ihlal edilir — web'de var olan bu güvenlik kontrolü mobile taşınırken atlanmamalı.
- **Cihaz mağazası süreçleri:** Apple/Google review süreçleri günler alabilir; Hafta 4'e bırakılırsa demo/teslim tarihini riske atar — TestFlight/Play internal testing Hafta 3 sonunda hazır olmalı (public store review MVP demo'su için gerekli değil, internal testing yeterli).

---

## 8. İlişkili Dokümanlar

- `sprintPlanPhase2.md` — Bu dokümanın kaynağı olan haftalık/modül bazlı özet plan
- `sprintPlan.md` / `issue.md` — Faz 1 (MVP) planı ve issue kırılımı
- `apiEndpoints.md` — Faz 2'de yeniden kullanılan (ve `POST /me/device-tokens` ile genişleyen) REST uçları
- `erDiagram.md` — `POLICY_EMBEDDING`, `CHAT_ATTACHMENT`, `CHAT_CITATION` (Faz 1'de zaten modellenmişti, bu fazda kullanıma alınıyor)
- `tech_stack.md` — Ollama, MinIO aktivasyonu, Google Maps API, Firebase FCM
