# Issue Backlog
## Yapay Zeka Destekli Şirket İçi Asistan — MVP (4 Hafta / 3 Full-Stack Geliştirici)

> Kaynak: `sprintPlan.md`, `businessProcessMapping.md`, `apiEndpoints.md`, `diagrams/ERDiagram.png`, `tech_stack.md`. Bu doküman `sprintPlan.md`'deki modül/hafta planını **atanabilir issue** seviyesine indirger: her madde başlık, açıklama, kabul kriterleri, story point ve önerilen atananla birlikte doğrudan GitHub/Jira'ya kopyalanabilir. Üç geliştirici de full-stack çalışır — her issue kendi modülünde DB migration → API → web/admin UI zincirinin tamamını kapsar.

---

## 0. Story Point Ölçeği

| SP | Anlamı |
|---|---|
| 1 | Birkaç saatlik, tek katmanlı basit iş |
| 2 | Yarım gün |
| 3 | ~1 gün, tek katmanlı ya da çok basit uçtan uca iş |
| 5 | 2-3 gün, standart uçtan uca iş (DB + API + UI) |
| 8 | 3-4 gün, karmaşık/riskli veya çok adımlı iş |
| 13 | Neredeyse bir hafta, yüksek belirsizlik içeren iş |

Story point'ler göreceli efor tahminidir, kesin süre garantisi değildir; sprint içi yeniden dengeleme için kullanılabilir.

## 1. Etiket Lejantı

`sprint-0` · `backend` · `frontend` · `fullstack` · `infra` · `design` · `auth` · `chatbot` · `directory` · `shuttle` · `vehicle` · `menu` · `schedule` · `survey` · `announcement` · `admin` · `test` · `stretch` · `buffer`

---

## 2. Yük Dağılımı Özeti (Story Point)

| Geliştirici | Hafta 1 | Hafta 2 | Hafta 3 | Hafta 4 | **Toplam** |
|---|---|---|---|---|---|
| **A** — Chatbot L1 + KB Admin (BP-02) | 21 | 19 | 15 (12 + 3 stretch) | 8 | **63** |
| **B** — Rehberler + Servis + Araç (BP-05/04/06) | 15 | 18 | 16 | 8 | **57** |
| **C** — Menü + Çalışma Düzeni + Anket + Duyuru + Admin (BP-03/09/07/08/10) | 13 | 21 | 18 | 16 | **68** |

**Hafta 0 (Ortak Temel — Sprint 0):** 29 SP, tüm ekip paralel/pair çalışır (2-3 gün).

Toplamlar birebir eşit değil (A: 63, B: 57, C: 68) — bu, orijinal plandaki "A en riskli/karmaşık tek modül, C çok sayıda ama görece basit modül" dengesini yansıtıyor. Atama sırasında Hafta 4'ü sıkıştırmayın; B ve A için Hafta 4 tamamen entegrasyon/bug-bash bufferı, C için hâlâ öznitelik geliştirmesi (BP-10) içeriyor.

**Test issue'ları (`X-T<hafta>`):** Her geliştiricinin haftalık listesine, o haftanın işini test eden ayrı bir küçük issue eklendi (`Contributing.md` Bölüm 3 — test yazımı Hafta 4'e bırakılmıyor, hafta içinde ilerliyor). Bu, üç geliştiricinin toplamına +8/+8/+11 SP ekledi (eski toplamlar: A 55, B 49, C 57). Hafta 4'ü zaten tamamen entegrasyon/bug-bash'e ayrılan A-10 ve B-11, o haftanın testlerini de kapsar — bu yüzden A ve B için ayrı bir Hafta 4 test issue'su yok. C'nin Hafta 4'ünde böyle bir buffer olmadığından (C-10/C-11 hâlâ öznitelik geliştirmesi), C-T4 ayrı bir issue olarak eklendi.

---

## 3. Sprint 0 — Ortak Temel (Tüm Ekip, Hafta 0, ~2-3 gün)

Bu 6 issue tamamlanmadan hiçbir modül gerçek API'ye/role bağlanamaz; paralel çalışmaya geçmeden önce bitmeli.

### ISSUE-000: Repo / Monorepo İskeleti
**Etiketler:** `sprint-0`, `fullstack`
**Önerilen Atanan:** Geliştirici A
**Story Point:** 3

**Açıklama:** pnpm workspace kurulumu — `apps/web` (çalışan uygulaması), `apps/admin` (veya tek app + role-based routing), `apps/api` (Spring Boot), `packages/shared` (Zod şemaları, ortak tipler). Kök seviyede lint/format (ESLint, Prettier) ve `.editorconfig` kuralları.

**Kabul Kriterleri:**
- [ ] `pnpm install` kök dizinde tüm workspace'leri kurar
- [ ] `apps/web`, `apps/api`, `packages/shared` klasör iskeleti mevcut
- [ ] Lint/format script'leri (`pnpm lint`, `pnpm format`) çalışıyor
- [ ] README'de "nasıl çalıştırılır" bölümü var

**Bağımlılık:** Yok

---

### ISSUE-001: PostgreSQL + Docker Compose + Flyway Migration Altyapısı
**Etiketler:** `sprint-0`, `backend`
**Önerilen Atanan:** Geliştirici C
**Story Point:** 5

**Açıklama:** `diagrams/ERDiagram.png`'deki şemanın (EMPLOYEE, DEPARTMENT, ROLE/PERMISSION, VEHICLE/RESERVATION, SHUTTLE_ROUTE/STOP, MENU/MEAL/MENU_ITEM, SURVEY/SURVEY_QUESTION/SURVEY_RESPONSE/FEEDBACK, ANNOUNCEMENT/NOTIFICATION, CHAT_*, POLICY_*) Flyway migration dosyalarına (`V1__init.sql`, vb.) dönüştürülmesi. Docker Compose ile yerel PostgreSQL 16 + pgvector + pg_trgm extension kurulumu. `weekly_schedule` alanı ER diyagramına göre `EMPLOYEE` tablosunda `jsonb` olarak tutulur; ayrı `WEEKLY_SCHEDULE`/`SCHEDULE_DAY` tablosu yoktur.

**Kabul Kriterleri:**
- [ ] `docker compose up` ile PostgreSQL + pgvector + pg_trgm ayağa kalkıyor
- [ ] Tüm entity'ler için Flyway migration'ları yazıldı ve `mvn flyway:migrate` (veya app başlangıcında otomatik) sorunsuz çalışıyor
- [ ] `EMPLOYEE.weekly_schedule` tek kaynak olarak modellenmiş, her çalışanın haftalık planı tek `EMPLOYEE` kaydında saklanıyor (FR-64 tek kaynak kısıtı)
- [ ] `FEEDBACK` tablosunda `employeeId` kolonu **yok** (FR-43 anonimlik — şema düzeyinde garanti)

**Bağımlılık:** ISSUE-000

---

### ISSUE-002: Spring Boot 3 Modüler Monolit Proje İskeleti
**Etiketler:** `sprint-0`, `backend`
**Önerilen Atanan:** Geliştirici A
**Story Point:** 5

**Açıklama:** Java 21 + Spring Boot 3 projesi (Maven), modül paketleri: `auth`, `menu`, `shuttle`, `directory`, `vehicle`, `survey`, `announcement`, `schedule`, `chatbot`. Her modül kendi controller/service/repository/entity katmanına sahip. Ortak: global exception handler (`{ error: { code, message } }` formatı), `springdoc-openapi` kurulumu, base path `/api/v1`.

**Kabul Kriterleri:**
- [ ] `mvn spring-boot:run` ile uygulama ayağa kalkıyor, `/api/v1` altında boş health-check endpoint dönüyor
- [ ] Modül paket yapısı kuruldu, her modülde en az bir placeholder controller var
- [ ] Global exception handler `apiEndpoints.md`'deki hata formatını (`{ error: { code, message } }`, 400/401/403/404/409/422/500) üretiyor
- [ ] `/swagger-ui.html` erişilebilir

**Bağımlılık:** ISSUE-000, ISSUE-001

---

### ISSUE-003: BP-01 — Auth, Session ve RBAC Middleware
**Etiketler:** `sprint-0`, `backend`, `auth`
**Önerilen Atanan:** Geliştirici A (lead — en kritik/riskli parça, tüm ekip review eder)
**Story Point:** 8

**Açıklama:** `POST /auth/login`, `POST /auth/2fa/verify`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/session` endpoint'leri. JWT tabanlı oturum yönetimi, yönetici girişinde 2FA (NFR-07), RBAC middleware (rol: `employee`, `admin` alt tipleri: `hr_admin`, `fleet_admin`, `shuttle_admin`, `canteen_admin`, `system_admin`). Rol/izin seed verisi.

**Kabul Kriterleri:**
- [ ] `POST /auth/login` geçerli kimlik bilgileriyle access+refresh token döner
- [ ] Yönetici rolü ile girişte 2FA adımı zorunlu (`POST /auth/2fa/verify` olmadan admin endpoint'lerine erişim reddedilir)
- [ ] `GET /auth/session` aktif oturum + rol bilgisini döner
- [ ] RBAC middleware, rol bazlı endpoint erişimini engelliyor (ör. `employee` rolü `/admin/*` uçlarına 403 alır)
- [ ] Rol/izin seed verisi (ROLE, PERMISSION, ROLE_PERMISSION) migration ile yükleniyor
- [ ] `GET /me` giriş yapan kullanıcının profilini döner

**Bağımlılık:** ISSUE-001, ISSUE-002

---

### ISSUE-004: CI Pipeline (GitHub Actions)
**Etiketler:** `sprint-0`, `infra`
**Önerilen Atanan:** Geliştirici B
**Story Point:** 3

**Açıklama:** PR açıldığında otomatik: backend build + test (Maven), frontend build + lint (pnpm), Docker image build doğrulaması (opsiyonel, sadece syntax check yeterli MVP için).

**Kabul Kriterleri:**
- [ ] PR açıldığında GitHub Actions workflow tetikleniyor
- [ ] Backend: `mvn verify` (build + test) çalışıyor, başarısızsa PR kırmızı işaretleniyor
- [ ] Frontend: `pnpm lint` + `pnpm build` çalışıyor
- [ ] Ana branch'e merge korumasında CI zorunlu kontrol olarak ayarlandı

**Bağımlılık:** ISSUE-000, ISSUE-002

---

### ISSUE-005: Web App + Admin Panel Temel Layout ve Auth Guard
**Etiketler:** `sprint-0`, `frontend`
**Önerilen Atanan:** Geliştirici B
**Story Point:** 5

**Açıklama:** React + TS + Tailwind + shadcn/ui ile temel layout (sidebar/nav, header), role-based routing (çalışan vs admin ekranları), auth guard (token yoksa login'e yönlendirme). TanStack Query kurulumu + `openapi-typescript` ile Swagger'dan otomatik TS tipi üretimi. Zod ile shared schema validation.

**Kabul Kriterleri:**
- [ ] Login ekranı, `POST /auth/login` ile çalışıyor, token saklanıyor (ör. httpOnly cookie veya secure storage)
- [ ] Auth guard: token yoksa korumalı route'lara erişim login'e yönlendiriyor
- [ ] Admin rolündeki kullanıcı admin route'larına, employee göremiyor
- [ ] `openapi-typescript` ile backend Swagger şemasından TS tipleri otomatik üretiliyor (`pnpm generate:types` gibi bir script)
- [ ] TanStack Query provider kuruldu, örnek bir GET isteği (`/me`) ile doğrulandı

**Bağımlılık:** ISSUE-002, ISSUE-003

---

## 4. Geliştirici A — BP-02 Chatbot (L1) + Bilgi Tabanı Yönetimi

### Hafta 1

#### A-1: Intent Embedding Servisi Entegrasyonu
**Etiketler:** `backend`, `chatbot`
**Story Point:** 8

**Açıklama:** bge-m3 (veya multilingual-e5-large) embedding modelinin backend'e entegrasyonu. Kullanıcı sorusu → embedding vektörü → önceden tanımlı intent kümesiyle kosinüs benzerliği üzerinden intent sınıflandırma. Bu, ekibin ilk NLP bileşeni olduğu için model servis çağrısı (local/HTTP), benzerlik eşiği kalibrasyonu ve "eşleşme yok" fallback davranışı ayrı ayrı ele alınmalı.

**Kabul Kriterleri:**
- [ ] Embedding servisi (yerel model ya da HTTP servis) backend'den çağrılabiliyor
- [ ] En az 10-15 örnek intent (menü, servis, rehber, izin, fazla mesai vb.) için embedding karşılaştırması doğru sınıflandırma yapıyor
- [ ] Eşik altı benzerlikte "intent bulunamadı" fallback'i tetikleniyor
- [ ] FR-08, FR-10 karşılanıyor

**Bağımlılık:** ISSUE-002

---

#### A-2: Template Yanıt Motoru
**Etiketler:** `backend`, `chatbot`
**Story Point:** 5

**Açıklama:** Intent → önceden tanımlı yanıt şablonu eşleme motoru. Şablonlar yapılandırılabilir (DB veya config dosyası), değişken enjeksiyonu destekler (ör. kullanıcı adı, departman).

**Kabul Kriterleri:**
- [ ] En az 10 intent için template yanıt tanımlı
- [ ] Template motoru, A-1'in döndürdüğü intent'e göre doğru şablonu seçiyor
- [ ] Şablonlar kod değişikliği gerektirmeden (DB/config üzerinden) güncellenebiliyor
- [ ] FR-09 karşılanıyor

**Bağımlılık:** A-1

---

#### A-3: `POST /chatbot/messages` — Yazılı Soru-Cevap Akışı
**Etiketler:** `backend`, `chatbot`
**Story Point:** 5

**Açıklama:** Kullanıcının yazılı soru gönderip yanıt aldığı ana endpoint. A-1 (intent) + A-2 (template) zincirini uçtan uca bağlar. NFR-02 gereği 5 saniye içinde yanıt dönmeli.

**Kabul Kriterleri:**
- [ ] `POST /chatbot/messages` soru alır, intent tespiti + template yanıtla 5 sn içinde yanıt döner (NFR-02)
- [ ] Yanıt formatı `apiEndpoints.md` ile uyumlu
- [ ] Hatalı/boş girişte 400 + standart hata formatı döner
- [ ] FR-08, FR-09, FR-10 karşılanıyor

**Bağımlılık:** A-2

---

#### A-T1: Hafta 1 Testleri (Intent Sınıflandırma + Mesaj Akışı)
**Etiketler:** `backend`, `chatbot`, `test`
**Story Point:** 3

**Açıklama:** A-1/A-2/A-3'ün testleri. Bkz. `Contributing.md` Bölüm 3 — bu, hafta sonuna bırakılmayan, o haftanın işiyle birlikte yazılan test issue'sudur.

**Kabul Kriterleri:**
- [ ] En az 10 örnek soru için intent sınıflandırma doğruluğu bir testle doğrulanıyor (A-1)
- [ ] Eşik altı benzerlikte "intent bulunamadı" fallback'inin tetiklendiği test ediliyor (A-1)
- [ ] `POST /chatbot/messages` happy path + boş/geçersiz girişte 400 dönüşü test ediliyor (A-3)

**Bağımlılık:** A-1, A-2, A-3

---

### Hafta 2

#### A-4: `GET/POST /chatbot/conversations` — Sohbet Geçmişi
**Etiketler:** `backend`, `chatbot`
**Story Point:** 3

**Açıklama:** Sohbet oturumlarının (`CHAT_CONVERSATION`) ve mesajların (`CHAT_MESSAGE`) kalıcı kaydı, kullanıcı bazlı listeleme ve tekil sohbet detayı.

**Kabul Kriterleri:**
- [ ] Her `POST /chatbot/messages` çağrısı ilgili conversation'a mesaj olarak kaydediliyor
- [ ] `GET /chatbot/conversations` kullanıcının sohbet listesini döner
- [ ] `GET /chatbot/conversations/{id}` tekil sohbetin tüm mesajlarını döner
- [ ] FR-09 karşılanıyor

**Bağımlılık:** A-3

---

#### A-5: İK Prosedürü Template Yönlendirmesi
**Etiketler:** `backend`, `chatbot`
**Story Point:** 8

**Açıklama:** Onboarding, izin türleri, fazla mesai, mazeret izni gibi İK prosedür sorularına adım adım yönlendirme (FR-51–57). `GET /hr/procedures?topic=` ve `GET /hr/procedures/{id}` endpoint'leri. Her prosedür yanıtı, güncel doküman versiyonuna ve geçerlilik tarihine dayanmalı (FR-58) — bu nedenle A-6 (PolicyVersion) ile birlikte tasarlanmalı.

**Kabul Kriterleri:**
- [ ] `GET /hr/procedures?topic=onboarding|izin|fazla-mesai|mazeret-izni` ilgili prosedürü döner
- [ ] "Bu prosedüre göre nasıl izin alabilirim?" tarzı sorular doğru prosedüre yönlendiriliyor (FR-54)
- [ ] Her prosedür yanıtı sorumlu departman/kişi bilgisini içeriyor (FR-57)
- [ ] Yanıt, `PolicyVersion.isCurrent=true` olan güncel versiyona dayanıyor (FR-58)
- [ ] FR-11–13, 51–58 karşılanıyor

**Bağımlılık:** A-6 (paralel geliştirilebilir, entegrasyon A-6'ya bağlı)

---

#### A-6: `PolicyDocument` / `PolicyVersion` CRUD (Admin)
**Etiketler:** `backend`, `chatbot`, `admin`
**Story Point:** 5

**Açıklama:** İK prosedür dokümanlarının ve politika versiyonlarının admin CRUD işlemleri. `POST/PUT/DELETE /admin/knowledge-base/documents`, `GET /admin/knowledge-base/documents/{id}/versions`. Versiyon numarası, geçerlilik tarihi (`effectiveDate`), `isCurrent` bayrağı — eski versiyonlar silinmez (denetim izi, NFR-06).

**Kabul Kriterleri:**
- [ ] Yeni doküman/politika versiyonu yüklendiğinde eski versiyon `isCurrent=false` olarak işaretleniyor, silinmiyor
- [ ] `GET /admin/knowledge-base/documents/{id}/versions` versiyon geçmişini kronolojik döner
- [ ] Sadece `hr_admin`/`system_admin` rolleri bu uçlara erişebiliyor (RBAC)
- [ ] FR-58, 77–78 karşılanıyor

**Bağımlılık:** ISSUE-003

---

#### A-T2: Hafta 2 Testleri (KB Admin RBAC + Versiyonlama + Prosedür Yönlendirme)
**Etiketler:** `backend`, `chatbot`, `admin`, `test`
**Story Point:** 3

**Açıklama:** A-4/A-5/A-6'nın testleri, özellikle KB admin uçlarının yetkilendirmesi ve versiyonlama davranışı (denetim izi kritik, NFR-06).

**Kabul Kriterleri:**
- [ ] `hr_admin`/`system_admin` dışındaki rollerin `/admin/knowledge-base/*` uçlarına 403 aldığı test ediliyor (A-6)
- [ ] Yeni doküman versiyonu yüklendiğinde eski versiyonun `isCurrent=false` olduğu ve silinmediği test ediliyor (A-6)
- [ ] `GET /hr/procedures?topic=` en az 3 farklı topic için doğru prosedürü döndüğü test ediliyor (A-5)

**Bağımlılık:** A-4, A-5, A-6

---

### Hafta 3

#### A-7: Admin Bilgi Tabanı Doküman Yönetim Ekranı
**Etiketler:** `frontend`, `admin`
**Story Point:** 5

**Açıklama:** Admin panelde doküman yükleme formu, doküman listesi, versiyon geçmişi görünümü (A-6'nın UI karşılığı).

**Kabul Kriterleri:**
- [ ] Admin, yeni doküman/politika versiyonu yükleyebiliyor (dosya + metadata)
- [ ] Doküman listesi ve versiyon geçmişi tablo halinde gösteriliyor
- [ ] Yalnızca yetkili rol bu ekranı görebiliyor

**Bağımlılık:** A-6, ISSUE-005

---

#### A-8: Chatbot Web UI
**Etiketler:** `frontend`, `chatbot`
**Story Point:** 5

**Açıklama:** Mesaj listesi, mesaj gönderme kutusu, sohbet geçmişi sidebar'ı. NFR-02'ye uygun yükleniyor/typing göstergesi.

**Kabul Kriterleri:**
- [ ] Kullanıcı mesaj yazıp gönderebiliyor, yanıt ekranda görünüyor
- [ ] Sohbet geçmişi sidebar'ından eski konuşmalar açılabiliyor
- [ ] Yanıt bekleme durumunda loading göstergesi var
- [ ] FR-08, 09 karşılanıyor

**Bağımlılık:** A-3, A-4, ISSUE-005

---

#### A-9 (Stretch): Kaynak Gösterme (`CHAT_CITATION`)
**Etiketler:** `backend`, `frontend`, `chatbot`, `stretch`
**Story Point:** 3

**Açıklama:** Zaman kalırsa: chatbot yanıtının hangi `PolicyVersion`'a dayandığını gösteren basit kaynak/citation gösterimi (izlenebilirlik, FR-14).

**Kabul Kriterleri:**
- [ ] İK prosedür yanıtlarının altında "Kaynak: [doküman adı, v.X]" bilgisi gösteriliyor
- [ ] `CHAT_CITATION` kaydı, ilgili `POLICY_VERSION`'a referans veriyor

**Bağımlılık:** A-5, A-6 — **Not:** Bu issue MVP kapsamı için zorunlu değil, Hafta 4'te zaman kalırsa alınmalı.

---

#### A-T3: Hafta 3 Testleri (Chatbot UI + Admin Doküman Ekranı)
**Etiketler:** `frontend`, `chatbot`, `test`
**Story Point:** 2

**Açıklama:** A-7/A-8 için temel smoke/erişim testleri.

**Kabul Kriterleri:**
- [ ] Chatbot UI'da mesaj gönderip yanıt görüntüleme happy path'i bir component/E2E testiyle kapsanıyor (A-8)
- [ ] Yetkisiz rolün admin doküman yönetim ekranını göremediği test ediliyor (A-7)

**Bağımlılık:** A-7, A-8

---

### Hafta 4

#### A-10: Entegrasyon, Cross-Review, Bug Bash, Demo Senaryoları
**Etiketler:** `buffer`, `fullstack`
**Story Point:** 8

**Açıklama:** Chatbot modülünün diğer modüllerle (özellikle Auth/RBAC) entegrasyon testi, B ve C'nin kodlarıyla cross-review, bug bash, demo günü için senaryo hazırlığı (ör. "yeni işe başlayan biri izin prosedürünü sorar" senaryosu uçtan uca).

**Kabul Kriterleri:**
- [ ] En az 3 uçtan uca demo senaryosu hazırlandı ve prova edildi
- [ ] Cross-review'da bulunan kritik buglar kapatıldı
- [ ] Chatbot yanıt süresi NFR-02 (5 sn) altında kalıyor (yük altında da)

**Bağımlılık:** A-1..A-9

---

## 5. Geliştirici B — BP-05 Rehberler, BP-04 Servis, BP-06 Araç Rezervasyonu

### Hafta 1 — Rehberler

#### B-1: Çalışan Arama/Filtre + Detay
**Etiketler:** `backend`, `directory`
**Story Point:** 3

**Açıklama:** `GET /employees?search=&department=&office=`, `GET /employees/{id}`. İsim/departman/ofis bazlı arama ve filtreleme.

**Kabul Kriterleri:**
- [ ] İsme göre arama çalışıyor (kısmi eşleşme, pg_trgm fuzzy search kullanılabilir)
- [ ] Departman ve ofis durumuna göre filtre çalışıyor
- [ ] `GET /employees/{id}` ofis durumu, telefon, e-posta dahil detay döner
- [ ] FR-28–33 karşılanıyor

**Bağımlılık:** ISSUE-001, ISSUE-002

---

#### B-2: Departman Arama + Detay
**Etiketler:** `backend`, `directory`
**Story Point:** 3

**Açıklama:** `GET /departments?search=`, `GET /departments/{id}` — sorumluluklar, yönetici, iletişim bilgisi.

**Kabul Kriterleri:**
- [ ] Departman arama çalışıyor
- [ ] Detay uç noktası sorumluluk, yönetici (managerId → EMPLOYEE), iletişim bilgisini döner
- [ ] FR-34–37 karşılanıyor

**Bağımlılık:** B-1 (aynı entity ilişkisini paylaşır)

---

#### B-3: Dahili Telefon Rehberi
**Etiketler:** `backend`, `directory`
**Story Point:** 2

**Açıklama:** `GET /phonebook?search=`, `POST /phonebook/{extension}/call` (click-to-call tetikleme — MVP'de gerçek çağrı entegrasyonu değil, tetikleme kaydı/log yeterli).

**Kabul Kriterleri:**
- [ ] Dahili numara arama çalışıyor
- [ ] `POST /phonebook/{extension}/call` isteği kabul ediyor (gerçek telefon entegrasyonu olmadan)
- [ ] FR-48–50 karşılanıyor

**Bağımlılık:** B-1

---

#### B-4: Rehber Ekranları (Web)
**Etiketler:** `frontend`, `directory`
**Story Point:** 5

**Açıklama:** Çalışan/departman/telefon rehberi için arama kutusu, filtre, kart/liste görünümü.

**Kabul Kriterleri:**
- [ ] Çalışan arama ekranında arama + filtre + kart görünümü çalışıyor
- [ ] Departman arama ekranı ve detay görünümü var
- [ ] Telefon rehberi listesi + click-to-call butonu var
- [ ] FR-28–37, 48–50 karşılanıyor

**Bağımlılık:** B-1, B-2, B-3, ISSUE-005

---

#### B-T1: Hafta 1 Testleri (Rehber Arama + Telefon)
**Etiketler:** `backend`, `directory`, `test`
**Story Point:** 2

**Açıklama:** B-1/B-3'ün testleri — fuzzy arama ve click-to-call uçlarının davranışı.

**Kabul Kriterleri:**
- [ ] İsme göre kısmi eşleşmenin (pg_trgm fuzzy search) beklenen sonuçları döndüğü test ediliyor (B-1)
- [ ] `POST /phonebook/{extension}/call` geçersiz dahili numarada anlamlı hata döndüğü test ediliyor (B-3)

**Bağımlılık:** B-1, B-2, B-3

---

### Hafta 2 — Servis

#### B-5: `ShuttleRoute`/`ShuttleStop` CRUD (Admin) + Liste
**Etiketler:** `backend`, `shuttle`, `admin`
**Story Point:** 5

**Açıklama:** `GET /shuttle-routes`, `GET /shuttle-routes/{id}/stops`, `GET /shuttle-routes/{id}/plate`, admin: `POST/PUT /admin/shuttle-routes`, `PUT /admin/shuttle-routes/{id}/plate`.

**Kabul Kriterleri:**
- [ ] Güzergah, durak ve saat bilgisi CRUD ile yönetilebiliyor
- [ ] Plaka bilgisi güncellenebiliyor
- [ ] Yalnızca `shuttle_admin` rolü admin uçlarına erişebiliyor
- [ ] FR-22–25, 73 karşılanıyor

**Bağımlılık:** ISSUE-001, ISSUE-003

---

#### B-6: Basit Güzergah Önerisi
**Etiketler:** `backend`, `shuttle`
**Story Point:** 5

**Açıklama:** `GET /shuttle-routes/recommendation?lat=&lng=` — MVP kapsamında Google Maps API olmadan, kullanıcının konumuna en yakın durağı (Öklid/Haversine mesafe hesabı) bulup ilgili güzergahı ve tahmini süreyi (basit sabit hız varsayımıyla) döner. Gerçek rota optimizasyonu Faz 2.

**Kabul Kriterleri:**
- [ ] Verilen lat/lng için en yakın durak ve güzergah doğru hesaplanıyor
- [ ] Tahmini varış süresi (basit yaklaşıklıkla) dönüyor
- [ ] FR-26, 27 (basitleştirilmiş versiyon) karşılanıyor

**Bağımlılık:** B-5

---

#### B-7: Servis Ekranı (Web)
**Etiketler:** `frontend`, `shuttle`
**Story Point:** 5

**Açıklama:** Güzergah listesi, durak/saat tablosu, güncel plaka gösterimi, konum bazlı öneri ekranı.

**Kabul Kriterleri:**
- [ ] Güzergah listesi ve durak/saat detayı görüntüleniyor
- [ ] Plaka bilgisi güncel gösteriliyor
- [ ] Konum izni verildiğinde önerilen güzergah gösteriliyor
- [ ] FR-22–27 karşılanıyor

**Bağımlılık:** B-5, B-6, ISSUE-005

---

#### B-T2: Hafta 2 Testleri (Servis RBAC + Güzergah Önerisi)
**Etiketler:** `backend`, `shuttle`, `admin`, `test`
**Story Point:** 3

**Açıklama:** B-5/B-6'nın testleri — admin yetkilendirmesi ve mesafe/öneri hesabının doğruluğu.

**Kabul Kriterleri:**
- [ ] `shuttle_admin` dışındaki rollerin `/admin/shuttle-routes` uçlarına 403 aldığı test ediliyor (B-5)
- [ ] Bilinen bir lat/lng için en yakın durak ve güzergah hesabının doğru sonuç verdiği test ediliyor (B-6)

**Bağımlılık:** B-5, B-6

---

### Hafta 3 — Araç Rezervasyonu

#### B-8: `Vehicle`/`Reservation` CRUD
**Etiketler:** `backend`, `vehicle`
**Story Point:** 5

**Açıklama:** `GET /vehicles?available=true`, `POST /reservations`, `DELETE /reservations/{id}`, `GET /reservations/me`, admin: `POST/PUT /admin/vehicles`, `PUT /admin/vehicles/{id}/maintenance-status`.

**Kabul Kriterleri:**
- [ ] Bakımda olmayan araçlar `available=true` filtresiyle listeleniyor
- [ ] Rezervasyon oluşturma, çakışan zaman aralığını engelliyor
- [ ] Kullanıcı kendi rezervasyonlarını görebiliyor ve iptal edebiliyor
- [ ] FR-38–41, 74 karşılanıyor

**Bağımlılık:** ISSUE-001, ISSUE-003

---

#### B-9: Araç Yönetimi Admin Ekranı
**Etiketler:** `frontend`, `vehicle`, `admin`
**Story Point:** 3

**Açıklama:** Araç ekleme/güncelleme formu, bakım durumu işaretleme.

**Kabul Kriterleri:**
- [ ] Admin yeni araç ekleyebiliyor, mevcut aracı güncelleyebiliyor
- [ ] Bakım durumu tek tıkla değiştirilebiliyor
- [ ] Yalnızca `fleet_admin` rolü erişebiliyor

**Bağımlılık:** B-8, ISSUE-005

---

#### B-10: Araç Rezervasyon Ekranı (Web)
**Etiketler:** `frontend`, `vehicle`
**Story Point:** 5

**Açıklama:** Uygun araç listesi, rezervasyon formu (tarih/saat seçimi), rezervasyon durum takibi.

**Kabul Kriterleri:**
- [ ] Uygun araçlar listeleniyor, bakımdakiler filtreleniyor
- [ ] Rezervasyon formu ile yeni rezervasyon oluşturulabiliyor
- [ ] Kullanıcı kendi rezervasyon durumunu görüp iptal edebiliyor
- [ ] FR-38–41 karşılanıyor

**Bağımlılık:** B-8, ISSUE-005

---

#### B-T3: Hafta 3 Testleri (Rezervasyon Çakışma Engeli + Araç RBAC)
**Etiketler:** `backend`, `vehicle`, `test`
**Story Point:** 3

**Açıklama:** B-8'in testleri — FR-38-41 kritik iş kuralı (çakışan rezervasyonun reddi) ve admin yetkilendirmesi.

**Kabul Kriterleri:**
- [ ] Çakışan zaman aralığında yapılan rezervasyonun reddedildiği test ediliyor (B-8, FR-38-41 kritik kural)
- [ ] `fleet_admin` dışındaki rollerin `/admin/vehicles` uçlarına 403 aldığı test ediliyor (B-8)

**Bağımlılık:** B-8

---

### Hafta 4

#### B-11: Entegrasyon, Cross-Review, Bug Bash
**Etiketler:** `buffer`, `fullstack`
**Story Point:** 8

**Açıklama:** Rehber/Servis/Araç modüllerinin diğer modüllerle entegrasyon testi, A ve C'nin kodlarıyla cross-review, bug bash.

**Kabul Kriterleri:**
- [ ] Tüm B modülü uçtan uca akışları (arama → detay, rezervasyon oluşturma/iptal, güzergah önerisi) manuel test edildi
- [ ] Cross-review'da bulunan kritik buglar kapatıldı
- [ ] Shared component uyuşmazlıkları (ör. tablo/kart component'leri) A ve C ile hizalandı

**Bağımlılık:** B-1..B-10

---

## 6. Geliştirici C — BP-03 Menü, BP-09 Çalışma Düzeni, BP-07 Anket, BP-08 Duyuru, BP-10 Admin

### Hafta 1 — Yemek Menüsü

#### C-1: `Menu`/`Meal`/`MenuItem` Şeması + Görüntüleme Endpoint'leri
**Etiketler:** `backend`, `menu`
**Story Point:** 3

**Açıklama:** `GET /menus/today`, `GET /menus/weekly`, `GET /meals/{id}` (kalori, alerjen).

**Kabul Kriterleri:**
- [ ] Bugünün menüsü ve haftalık menü doğru dönüyor
- [ ] Yemek detayında kalori ve alerjen bilgisi var
- [ ] FR-17–20 karşılanıyor

**Bağımlılık:** ISSUE-001, ISSUE-002

---

#### C-2: Excel Menü İçe Aktarma
**Etiketler:** `backend`, `menu`, `admin`
**Story Point:** 5

**Açıklama:** `POST /admin/menus/import` — Apache POI ile önceden tanımlı Excel şablonunun parse edilmesi. **Not:** İmplementasyona başlamadan önce yemekhane yönetiminden örnek şablon dosyası istenmeli (sprintPlan.md Bölüm 6 riski).

**Kabul Kriterleri:**
- [ ] Tanımlı şablon formatındaki Excel dosyası yüklendiğinde `MENU`/`MEAL`/`MENU_ITEM` kayıtları doğru oluşuyor
- [ ] Hatalı/eksik şablon formatında anlamlı hata mesajı dönüyor (422)
- [ ] `DELETE /admin/menus/{id}` ile menü kaydı kaldırılabiliyor
- [ ] FR-21, 72 karşılanıyor

**Bağımlılık:** C-1

---

#### C-3: Menü Ekranı (Web)
**Etiketler:** `frontend`, `menu`
**Story Point:** 3

**Açıklama:** Bugün/haftalık sekme geçişi, kalori/alerjen gösterimi.

**Kabul Kriterleri:**
- [ ] Bugünün menüsü ve haftalık menü sekmeler arası geçişle gösteriliyor
- [ ] Her yemek için kalori ve alerjen bilgisi görünür
- [ ] FR-17–20 karşılanıyor

**Bağımlılık:** C-1, ISSUE-005

---

#### C-T1: Hafta 1 Testleri (Excel İçe Aktarma Hata/Başarı Yolları)
**Etiketler:** `backend`, `menu`, `test`
**Story Point:** 2

**Açıklama:** C-2'nin testleri — Excel import'un başarı ve hata yolları.

**Kabul Kriterleri:**
- [ ] Geçerli şablonla içe aktarmanın `MENU`/`MEAL`/`MENU_ITEM` kayıtlarını doğru oluşturduğu test ediliyor (C-2)
- [ ] Hatalı/eksik şablonda 422 + anlamlı hata mesajı döndüğü test ediliyor (C-2)

**Bağımlılık:** C-2

---

### Hafta 2 — Çalışma Düzeni

#### C-4: `WeeklySchedule`/`ScheduleDay` — Tek Kaynak API
**Etiketler:** `backend`, `schedule`
**Story Point:** 5

**Açıklama:** `GET/PUT /schedules/me`, `GET /schedules/me/summary`. **Kritik:** FR-64 tek kaynak (single source of truth) kısıtı — `(employeeId, weekStartDate)` unique constraint zaten ISSUE-001'de kuruldu, bu issue iş mantığını üzerine bina eder.

**Kabul Kriterleri:**
- [ ] `PUT /schedules/me` Pazartesi–Cuma için `office`/`remote`/`leave` durumlarını kaydediyor
- [ ] `GET /schedules/me/summary` gün sayısı özetini doğru hesaplıyor
- [ ] Kullanıcı yalnızca kendi verisini görüp düzenleyebiliyor (FR-63)
- [ ] FR-59–64 karşılanıyor

**Bağımlılık:** ISSUE-001, ISSUE-003

---

#### C-5: `/my-schedule` Ekranı (Web)
**Etiketler:** `frontend`, `schedule`
**Story Point:** 8

**Açıklama:** Pazartesi–Cuma için gün durumu seçimi (Ofiste/Uzaktan/İzinli), haftalık özet gösterimi, kalıcı kayıt.

**⚠️ Tasarım Notu:** Referans Figma dosyasında (Company Assistant Mobile App) bu ekranın tasarımı **mevcut değil** — tek eksik ekran budur. Geliştirmeye başlamadan önce mevcut tasarım dilini (Tailwind + shadcn/ui component'leri, renk paleti, tipografi) referans alarak bu ekranın wireframe/mockup'ı hazırlanmalı (bkz. C-5a). Diğer tüm ekranlar için Figma tasarımı doğrudan kullanılabilir.

**Kabul Kriterleri:**
- [ ] Her iş günü için tek seçim (Ofiste/Uzaktan/İzinli) yapılabiliyor
- [ ] Haftalık özet (gün sayıları) anlık güncelleniyor
- [ ] Kaydet butonu `PUT /schedules/me`'yi çağırıyor, sayfa yenilendiğinde veri kalıcı
- [ ] Tasarım, mevcut uygulama tasarım diliyle tutarlı (C-5a'daki mockup'a uygun)
- [ ] FR-60–63 karşılanıyor

**Bağımlılık:** C-4, C-5a, ISSUE-005

---

#### C-5a: "My Schedule" Ekran Tasarımı (UI/UX Mockup)
**Etiketler:** `design`, `schedule`
**Story Point:** 2

**Açıklama:** Referans Figma dosyasında bulunmayan tek ekran. Mevcut tasarımın component stilini (kart, buton, renk paleti, spacing) baz alarak Figma veya doğrudan kod üzerinde hızlı bir mockup/wireframe hazırlanır. Amaç, C-5'e başlamadan önce gün seçim UI'ı (5 günlük toggle/segment control) ve haftalık özet kartının görsel dilini netleştirmek.

**Kabul Kriterleri:**
- [ ] Gün seçim bileşeninin (Ofiste/Uzaktan/İzinli) görsel tasarımı netleşti
- [ ] Haftalık özet kartının tasarımı netleşti
- [ ] Ekip onayı alındı (kısa review, 15 dk)

**Bağımlılık:** Yok — C-5'ten önce ya da paralel yapılmalı

---

#### C-6: `GET /admin/schedules` — Yönetici Salt-Okunur Tablo
**Etiketler:** `backend`, `frontend`, `schedule`, `admin`
**Story Point:** 3

**Açıklama:** Tüm çalışanların haftalık çalışma düzenini gösteren salt-okunur admin tablosu (API + UI).

**Kabul Kriterleri:**
- [ ] `GET /admin/schedules` tüm çalışanların güncel hafta verisini döner
- [ ] Admin ekranında tablo görünümü, aynı tek-kaynak veriyi (C-4) okuyor — ayrı bir veri kopyası oluşturulmuyor
- [ ] FR-79 karşılanıyor

**Bağımlılık:** C-4, ISSUE-005

---

#### C-T2: Hafta 2 Testleri (Tek Kaynak Kuralı + Sahiplik)
**Etiketler:** `backend`, `schedule`, `test`
**Story Point:** 3

**Açıklama:** C-4'ün testleri — FR-64 tek kaynak kısıtı ve veri sahipliği, ileride Faz 2 mobilin de aynı uca bağlanacağı için burada kırılırsa iki platformu birden etkiler.

**Kabul Kriterleri:**
- [ ] Kullanıcının yalnızca kendi haftalık planını güncelleyebildiği (başka bir çalışanın verisini değiştirme denemesinin reddedildiği) test ediliyor (C-4, FR-63)
- [ ] `(employeeId, weekStartDate)` unique kısıtının ihlal edilemediği test ediliyor (C-4)
- [ ] `GET /admin/schedules`'ın `C-4` ile aynı veriyi okuduğu, ayrı bir kopya oluşturmadığı test ediliyor (C-6)

**Bağımlılık:** C-4, C-6

---

### Hafta 3 — Anket + Duyuru

#### C-7: `Survey`/`SurveyResponse`/`Feedback` Endpoint'leri
**Etiketler:** `backend`, `survey`
**Story Point:** 5

**Açıklama:** `GET /surveys/active`, `POST /surveys/{id}/responses`, `POST /feedback` (anonim).

**Kabul Kriterleri:**
- [ ] Aktif anketler listeleniyor, yanıt gönderilebiliyor
- [ ] `POST /feedback` ile gönderilen kayıtta `employeeId` alanı yok (FR-43 anonimlik — şema zaten garanti ediyor, API da bunu yansıtmalı)
- [ ] FR-42–43 karşılanıyor

**Bağımlılık:** ISSUE-001, ISSUE-003

---

#### C-8: Admin Anket Oluşturma + Sonuç Görüntüleme Ekranı
**Etiketler:** `backend`, `frontend`, `survey`, `admin`
**Story Point:** 5

**Açıklama:** `POST /admin/surveys`, `PUT /admin/surveys/{id}/publish`, `GET /admin/surveys/{id}/results` + admin UI.

**Kabul Kriterleri:**
- [ ] Admin yeni anket oluşturup yayımlayabiliyor
- [ ] Sonuçlar özet/grafik halinde görüntülenebiliyor
- [ ] FR-44, 76 karşılanıyor

**Bağımlılık:** C-7

---

#### C-9: `Announcement`/`Notification` Endpoint'leri + Sabitleme
**Etiketler:** `backend`, `frontend`, `announcement`
**Story Point:** 5

**Açıklama:** `GET /announcements`, `GET /announcements/{id}`, `GET /notifications`, `PUT /notifications/preferences`, admin: `POST /admin/announcements`, `PUT /admin/announcements/{id}/pin` + UI.

**Kabul Kriterleri:**
- [ ] Duyuru listesinde sabitlenenler üstte gösteriliyor
- [ ] Admin duyuru oluşturup yayımlayabiliyor ve sabitleyebiliyor
- [ ] Kullanıcı bildirim tercihlerini güncelleyebiliyor
- [ ] FR-45–47, 65–67 karşılanıyor

**Bağımlılık:** ISSUE-001, ISSUE-003

---

#### C-T3: Hafta 3 Testleri (Anonimlik + Anket Yetkilendirme)
**Etiketler:** `backend`, `survey`, `test`
**Story Point:** 3

**Açıklama:** C-7/C-8'in testleri — FR-43 anonimlik kritik gizlilik kuralı ve admin yetkilendirmesi.

**Kabul Kriterleri:**
- [ ] `POST /feedback` ile oluşturulan kayıtta `employeeId` alanının ne request'te ne DB şemasında bulunmadığı test ediliyor (C-7, FR-43 kritik)
- [ ] Anket oluşturma/yayımlama uçlarının yetkisiz rollerce çağrılamadığı test ediliyor (C-8)

**Bağımlılık:** C-7, C-8

---

### Hafta 4 — Admin CRUD + RBAC + Rapor

#### C-10: Admin Çalışan/Departman CRUD Ekranları
**Etiketler:** `backend`, `frontend`, `admin`
**Story Point:** 5

**Açıklama:** `POST/PUT/DELETE /admin/employees`, `POST/PUT/DELETE /admin/departments` + admin UI formları.

**Kabul Kriterleri:**
- [ ] Admin çalışan oluşturabiliyor, güncelleyebiliyor, silebiliyor
- [ ] Admin departman oluşturabiliyor, güncelleyebiliyor, silebiliyor
- [ ] Yalnızca `hr_admin` rolü erişebiliyor
- [ ] FR-68–71 karşılanıyor

**Bağımlılık:** ISSUE-003

---

#### C-11: Rol/İzin Yönetim Ekranı (RBAC) + Basit Rapor/Export
**Etiketler:** `backend`, `frontend`, `admin`
**Story Point:** 8

**Açıklama:** `GET /admin/roles`, `PUT /admin/users/{id}/roles`, `GET /admin/reports/{type}`, `GET /admin/reports/{id}/export?format=xlsx|pdf`. MVP için tek bir rapor tipiyle başlanmalı (ör. `usage` — modül kullanım özeti).

**Kabul Kriterleri:**
- [ ] Admin kullanıcıya rol atayabiliyor/kaldırabiliyor
- [ ] En az bir rapor tipi (`usage`) oluşturulup xlsx veya pdf olarak dışa aktarılabiliyor
- [ ] Yalnızca `system_admin` rolü rol/izin ekranına erişebiliyor
- [ ] FR-80–82 karşılanıyor

**Bağımlılık:** ISSUE-003, C-10

---

#### C-T4: Hafta 4 Testleri (Admin CRUD RBAC + Rapor Export)
**Etiketler:** `backend`, `admin`, `test`
**Story Point:** 3

**Açıklama:** C-10/C-11'in testleri — BP-10 admin CRUD ve RBAC/rapor uçlarının yetkilendirmesi. C için ayrı bir Hafta 4 entegrasyon/bug-bash buffer'ı yok (bkz. Bölüm 7), bu yüzden bu testler C-10/C-11'in kendi haftasında yazılır.

**Kabul Kriterleri:**
- [ ] `hr_admin` dışındaki rollerin çalışan/departman CRUD uçlarına erişemediği test ediliyor (C-10)
- [ ] Yalnızca `system_admin`'in rol atama ucuna (`PUT /admin/users/{id}/roles`) erişebildiği test ediliyor (C-11)
- [ ] `usage` rapor export'unun (xlsx veya pdf) geçerli bir dosya döndürdüğü test ediliyor (C-11)

**Bağımlılık:** C-10, C-11

---

## 7. Kritik Riskler ve Bağımlılıklar (sprintPlan.md'den taşındı)

- **Auth gate (ISSUE-003):** Hafta 0 bitmeden hiçbir modül gerçek kullanıcı/rol context'iyle test edilemez. Bu adım gecikirse tüm plan bir hafta kayar — bu yüzden en deneyimli kişiye önerilir.
- **A-1 (Intent embedding):** "Basit" görünse de yeni bir NLP bileşenidir; küçümsenmemeli, 8 SP olarak işaretlendi.
- **C-4/C-6 (FR-64 tek kaynak kuralı):** `WeeklySchedule` verisi Faz 2'de mobil geldiğinde de aynı `/schedules/me` ucundan beslenmeli — C bu API sözleşmesini ileride mobilin de kullanacağını göz önünde tasarlamalı.
- **C-5a (My Schedule tasarımı):** Figma'da referans yok — C-5'e başlamadan bu küçük tasarım adımı atlanmamalı, aksi halde geliştirme sırasında tasarım kararları geri döner ve gecikme yaratır.
- **C-2 (Excel şablonu):** Şablon formatı netleşmeden implementasyona başlanmamalı — yemekhane yönetiminden örnek dosya istenmeli.
- **Hafta 4 buffer'ı sıkıştırmayın:** A-10 ve B-11 tamamen entegrasyon/bug-bash; yeni özellik eklenmemeli. C-10/C-11 hâlâ öznitelik geliştirmesi içerdiği için C'nin Hafta 4 yükü diğerlerinden daha yüksek (16 SP, C-T4 dahil) — gerekirse A veya B'den destek planlanmalı.
- **Test issue'larının eklenmesiyle artan yük:** `X-T<hafta>` issue'ları toplam SP'yi artırdı (A: 55→63, B: 49→57, C: 57→68). Hafta 2 sonunda gerçek ilerleme gözden geçirilmeli; sıkışma olursa önce A-9 (stretch, zaten opsiyonel) kesilmeli, test issue'ları kesilmemeli — bunlar RBAC/anonimlik/tek-kaynak gibi kritik kuralları koruyor (bkz. `Contributing.md` Bölüm 3).

---

## 8. İlişkili Dokümanlar

- `sprintPlan.md` — Bu dokümanın kaynağı olan haftalık/modül bazlı özet plan
- `businessProcessMapping.md` — BP-01–BP-10 süreç tanımları
- `apiEndpoints.md` — Bu issue'larda referans verilen tüm REST uçları
- `diagrams/ERDiagram.png` — Veri modeli
- `tech_stack.md` — Teknoloji kararları (Java 21 + Spring Boot 3, PostgreSQL, React + TS)
- `Contributing.md` — PR/review kuralları ve Bölüm 3'teki test yazım kuralı (hangi durumda test zorunlu, hangi durumda değil)
