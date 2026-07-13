# Sprint Planı ve Issue Kırılımı (4 Hafta / 3 Full-Stack Geliştirici)
## Yapay Zeka Destekli Şirket İçi Asistan — MVP

> Kaynak: `requirementAnalysis2.md`, `businessProcessMapping.md`, `apiEndpoints.md`, `erDiagram.md`, `tech_stack.md`. Bu doküman, 4 haftalık MVP teslimatı için modül bazlı iş bölümünü ve issue kırılımını tanımlar. Her madde doğrudan bir GitHub issue'suna çevrilebilir.

---

## 1. Kapsam Kararı

**MVP'ye dahil (4 hafta):**
- Web uygulaması (çalışan) + Web yönetim paneli (React + TS + Tailwind + shadcn/ui)
- Backend API (Java 21 + Spring Boot 3, modüler monolit, Spring Data JPA, PostgreSQL)
- Chatbot **L1 yalnızca**: Intent detection (embedding bazlı) + template yanıtlar + İK prosedürü template yönlendirmesi (FR-11–13, 51–58) — **LLM/RAG yok**
- Auth: Şirket SSO/kimlik doğrulama + RBAC (2FA dahil)
- Tüm diğer modüller (Menu, Shuttle, Directory, Vehicle, Survey, Announcement, Schedule, Admin CRUD)

**Faz 2'ye ertelendi (kapsam dışı):**
- Mobil uygulama (Expo/React Native) — tamamı
- Chatbot **L2**: RAG pipeline, local LLM (Phi-4 Mini/Qwen), pgvector embedding retrieval, dosya yükleme ile soru sorma (FR-15)
- Sesli giriş (FR-16)
- Bölüm 12'deki "Gelecek Geliştirmeler" listesi

**Neden:** AI katmanı (embedding modeli + local LLM servisi + RAG pipeline + citation doğrulama) tek başına 3 kişi/4 hafta için ayrı bir proje büyüklüğünde. Mobil app'i aynı anda geliştirmek ekip odağını dörde bölerdi. Web + admin + template-tabanlı chatbot ile önce çalışan, demo edilebilir bir MVP çıkarılıyor; RAG+LLM+mobil Faz 2'de üstüne eklenir.

---

## 2. Ekip ve Sorumluluk Alanları (Modül Bazlı, Uçtan Uca)

| Kişi | Modüller (BP) | Kapsam |
|---|---|---|
| **Geliştirici A** | BP-01 (Auth çekirdeği — ortak), BP-02 (Chatbot L1 + KB admin) | En karmaşık/riskli modül; en deneyimli kişiye önerilir |
| **Geliştirici B** | BP-05 (Rehberler), BP-04 (Servis), BP-06 (Araç Rezervasyonu) | Orta karmaşıklıkta, birbirine benzeyen CRUD+arama akışları |
| **Geliştirici C** | BP-03 (Yemek Menüsü), BP-09 (Çalışma Düzeni), BP-07 (Anket), BP-08 (Duyuru), BP-10 (Admin CRUD + RBAC + Rapor) | Çok sayıda ama görece basit modül |

Her kişi kendi modülünde DB migration → API endpoint → web/admin UI zincirinin tamamından sorumludur.

---

## 3. Hafta 0 — Ortak Temel (Tüm Ekip, 2-3 gün, paralel başlamadan önce)

Bu adımlar paylaşılmadan modüller birbirinden bağımsız ilerleyemez; tüm ekip birlikte tamamlamalı.

- [ ] **ISSUE-000:** Repo/monorepo iskeleti (pnpm workspace: `apps/web`, `apps/admin` veya tek app + role-based routing, `apps/api`, `packages/shared`)
- [ ] **ISSUE-001:** PostgreSQL + Docker Compose kurulumu, `erDiagram.md`'deki şemanın Flyway migration'larına dönüştürülmesi
- [ ] **ISSUE-002:** Spring Boot 3 (Java 21) modüler monolit proje iskeleti (modül paketleri: Auth, Menu, Shuttle, Directory, Vehicle, Survey, Announcement, Schedule, Chatbot)
- [ ] **ISSUE-003:** BP-01 — Auth: `/auth/login`, `/auth/session`, `/auth/refresh`, `/auth/logout`, RBAC middleware, rol/izin seed verisi
- [ ] **ISSUE-004:** CI pipeline (GitHub Actions): build + lint + test on PR
- [ ] **ISSUE-005:** Web app + Admin panel temel layout, routing, auth guard, TanStack Query + openapi-typescript kurulumu

**Çıktı:** Auth çalışır durumda, boş ama gezilebilir web+admin kabuk, DB migration'ları hazır. Bu olmadan hiçbir modül gerçek API'ye bağlanamaz.

---

## 4. Haftalık Plan (Hafta 1-4)

| Hafta | Geliştirici A | Geliştirici B | Geliştirici C |
|---|---|---|---|
| 1 | BP-02 Chatbot L1 temeli (intent embedding servisi, template motor) | BP-05 Rehberler (Employee/Department/Phonebook arama) | BP-03 Yemek Menüsü (görüntüleme + admin Excel import) |
| 2 | BP-02 İK prosedür template yönlendirme (FR-51–58) + KB admin CRUD | BP-04 Servis (güzergah/durak/plaka + basit öneri) | BP-09 Çalışma Düzeni (tek kaynak, FR-64 kritik) |
| 3 | BP-02 test, sohbet geçmişi, admin doküman versiyonlama | BP-06 Araç Rezervasyonu (liste, rezervasyon, bakım durumu) | BP-07 Anket/Feedback + BP-08 Duyuru/Bildirim |
| 4 | Entegrasyon, cross-review, bug bash, chatbot demo senaryoları | Entegrasyon, cross-review, bug bash | BP-10 Admin CRUD + RBAC ekranı + basit rapor/export, entegrasyon |

**Hafta 4 tamamı buffer/entegrasyon haftası olarak düşünülmeli** — 3 kişilik ekiplerde modüller birleşirken kaçınılmaz sürtünme (ör. shared component'ler, API sözleşme uyuşmazlıkları) çıkar.

---

## 5. Detaylı Issue Listesi

### Geliştirici A — BP-02 Chatbot (L1) 

- [ ] **A-1:** Intent embedding servisi entegrasyonu (bge-m3 veya multilingual-e5-large) — soru → intent sınıflandırma
- [ ] **A-2:** Template yanıt motoru (intent → önceden tanımlı yanıt şablonu eşleme)
- [ ] **A-3:** `POST /chatbot/messages` — yazılı soru-cevap akışı (NFR-02: 5 sn içinde yanıt)
- [ ] **A-4:** `GET/POST /chatbot/conversations` — sohbet geçmişi kayıt ve listeleme
- [ ] **A-5:** İK prosedür template yönlendirmesi — onboarding, izin, fazla mesai, mazeret izni akışları (FR-51–57), `GET /hr/procedures`
- [ ] **A-6:** `PolicyDocument`/`PolicyVersion` CRUD (admin) — versiyon + geçerlilik tarihi (FR-58, 78)
- [ ] **A-7:** Admin bilgi tabanı doküman yönetim ekranı (yükleme, versiyon geçmişi listesi)
- [ ] **A-8:** Chatbot web UI (mesaj listesi, gönderme, sohbet geçmişi sidebar)
- [ ] **A-9 (stretch):** Basit "kaynak gösterme" (`CHAT_CITATION`) — hangi prosedür versiyonundan yanıt üretildiği

### Geliştirici B — BP-05 Rehberler, BP-04 Servis, BP-06 Araç Rezervasyonu

- [ ] **B-1:** `GET /employees`, `GET /employees/{id}` — arama/filtre + detay
- [ ] **B-2:** `GET /departments`, `GET /departments/{id}` — arama + detay
- [ ] **B-3:** `GET /phonebook` — dahili rehber
- [ ] **B-4:** Rehber ekranları (web) — arama, filtre, kart/liste görünümü
- [ ] **B-5:** `ShuttleRoute`/`ShuttleStop` CRUD (admin) + `GET /shuttle-routes`
- [ ] **B-6:** Basit güzergah önerisi (`GET /shuttle-routes/recommendation`) — GPS entegrasyonu olmadan en yakın durak hesaplama (Google Maps API entegrasyonu Faz 2'ye bırakılabilir)
- [ ] **B-7:** Servis ekranı (web) — güzergah listesi, durak/saat, plaka
- [ ] **B-8:** `Vehicle`/`Reservation` CRUD — `GET /vehicles`, `POST/DELETE /reservations`, `GET /reservations/me`
- [ ] **B-9:** Araç yönetimi admin ekranı (ekleme, bakım durumu işaretleme)
- [ ] **B-10:** Araç rezervasyon ekranı (web) — uygun araç listesi, rezervasyon formu, durum takibi

### Geliştirici C — BP-03 Menü, BP-09 Çalışma Düzeni, BP-07 Anket, BP-08 Duyuru, BP-10 Admin

- [ ] **C-1:** `Menu`/`Meal`/`MenuItem` şeması + `GET /menus/today`, `GET /menus/weekly`
- [ ] **C-2:** Excel import (`POST /admin/menus/import`) — Apache POI ile şablon parse
- [ ] **C-3:** Menü ekranı (web) — bugün/haftalık sekmesi, kalori/alerjen gösterimi
- [ ] **C-4:** `WeeklySchedule`/`ScheduleDay` — `GET/PUT /schedules/me`, `GET /schedules/me/summary` (**FR-64 tek kaynak kısıtına dikkat**)
- [ ] **C-5:** `/my-schedule` ekranı (web) — Pzt-Cuma durum seçimi, haftalık özet
- [ ] **C-6:** `GET /admin/schedules` — yönetici salt-okunur tablo görünümü
- [ ] **C-7:** `Survey`/`SurveyResponse`/`Feedback` — `GET /surveys/active`, `POST /surveys/{id}/responses`, `POST /feedback`
- [ ] **C-8:** Admin anket oluşturma + sonuç görüntüleme ekranı
- [ ] **C-9:** `Announcement`/`Notification` — `GET /announcements`, `POST /admin/announcements`, sabitleme
- [ ] **C-10:** Admin çalışan/departman CRUD ekranları (FR-68–71)
- [ ] **C-11:** Rol/izin yönetim ekranı (RBAC, FR-80) + basit rapor/export (FR-81–82, tek bir rapor tipiyle başla)

---

## 6. Kritik Riskler ve Bağımlılıklar

- **Auth gate (BP-01):** Hafta 0 bitmeden hiçbir modül gerçek kullanıcı/rol context'iyle test edilemez. Bu adım gecikirse tüm plan bir hafta kayar.
- **Chatbot L1 kapsamı hâlâ küçümsenmemeli:** Intent embedding entegrasyonu + template motor, "basit" görünse de yeni bir NLP bileşenidir; A'nın 3 haftası bu yüzden yalnızca tek modüle ayrıldı.
- **FR-64 tek kaynak kuralı:** `WeeklySchedule` verisi mobil (Faz 2) geldiğinde de aynı `/schedules/me` ucundan beslenmeli — C bu API sözleşmesini ileride mobilin de kullanacağını göz önünde tasarlamalı.
- **Hafta 4 buffer'ı sıkıştırmayın:** 3 kişilik ekiplerde entegrasyon haftası genellikle planlanandan uzun sürer; yeni modül eklemek yerine bu haftayı bug-fix/polish'e ayırın.
- **Excel import şablonu (C-2):** Şablon formatı netleşmeden implementasyona başlamayın — yönetimden örnek dosya isteyin.

---

## 7. Faz 2 Backlog (4 Hafta Sonrası)

- Mobil uygulama (Expo) — tüm BP'lerin mobil karşılığı
- Chatbot L2: RAG pipeline, pgvector embedding retrieval, local LLM (Foundry Local + Phi-4 Mini/Qwen)
- Dosya yükleyerek soru sorma (FR-15), sesli giriş (FR-16)
- Google Maps API ile gerçek rota optimizasyonu ve ETA (FR-27)
- Push notification (Firebase FCM)
- `requirementAnalysis2.md` bölüm 12'deki diğer maddeler
