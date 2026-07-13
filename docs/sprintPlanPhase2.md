# Sprint Planı — Faz 2 (4 Hafta / 3 Full-Stack Geliştirici)
## Yapay Zeka Destekli Şirket İçi Asistan — Mobil Uygulama + Chatbot L2

> Kaynak: `requirementAnalysis2.md`, `businessProcessMapping.md`, `apiEndpoints.md`, `diagrams/ERDiagram.png`, `tech_stack.md`, `sprintPlan.md` (Faz 1/MVP), `issue.md` (Faz 1 issue kırılımı). Bu doküman, Faz 1'de web + Chatbot L1 ile teslim edilen MVP'nin üzerine **mobil uygulama** ve **Chatbot L2 (RAG/LLM)**'yi ekleyen ikinci 4 haftalık sprintin modül bazlı iş bölümünü tanımlar. Ekip aynı: 3 full-stack geliştirici.

---

## 1. Kapsam Kararı

**Faz 2'ye dahil (4 hafta):**
- **Mobil uygulama (Expo/React Native) — çalışan self-servis ekranları:** Auth/Dashboard (BP-01), Chatbot (BP-02), Menü (BP-03 çalışan alt-süreci), Servis (BP-04 çalışan alt-süreci + gerçek Maps entegrasyonu), Rehberler (BP-05), Araç Rezervasyonu (BP-06 çalışan alt-süreci), Anket/Feedback (BP-07 çalışan alt-süreci), Duyuru/Bildirim (BP-08 çalışan alt-süreci + gerçek push), Çalışma Düzeni / My Schedule (BP-09 çalışan alt-süreci)
- **Mobil uygulama — admin/yönetici ekranları:** `requirementAnalysis2.md` bölüm 9'da admin paneli "masaüstü web uygulaması" olarak tanımlansa da, bu fazda **web'deki admin akışlarının mobil karşılığı da sağlanır**: BP-03/04/06/07/08'in yönetici alt-süreçleri (Excel menü yükleme, güzergah/araç yönetimi, anket oluşturma, duyuru yayımlama+sabitleme) ve BP-10'un tamamı (Çalışan/Departman CRUD, RBAC rol atama, rapor oluşturma/export) mobilde de kullanılabilir olacak. Rol bazlı navigasyon ile yalnızca ilgili admin rolündeki kullanıcılara gösterilir. Yeni bir gereksinim analizi gerekmez — bu ekranların backend'i Faz 1'de zaten tamamlanmıştı (FR-21, 68–82), tek eksik olan mobil istemci katmanıdır.
- **Chatbot L2** — RAG pipeline (pgvector embedding retrieval), local LLM entegrasyonu (Ollama + Phi-4 Mini/Qwen), dosya yükleyerek soru sorma (FR-15), sesli giriş (FR-16), kaynak gösterme/citation (FR-14 izlenebilirliği)
- Bu üçünü destekleyen altyapı: Push notification (Firebase FCM), gerçek Google Maps API entegrasyonu (FR-27), dosya depolama için MinIO aktivasyonu (`tech_stack.md`'de "Future" işaretliydi, bu fazda devreye giriyor)

**Faz 2'ye dahil DEĞİL:**
- **`requirementAnalysis2.md` Bölüm 12 — "Gelecek Geliştirmeler" listesi** (izin talep iş akışı, fazla mesai onay iş akışı, masraf yönetimi, toplantı odası rezervasyonu, devam takibi, QR giriş, takvim/Teams/Slack entegrasyonu, doküman yönetim sistemi, AI destekli analitik vb.) — bu 14 madde için henüz FR/BP tanımlanmadı; planlanabilmesi için önce ayrı bir gereksinim analizi turu gerekir. Bkz. Bölüm 6 (Faz 3+ Backlog).

**Kapsam tamamlandığında:** Faz 1 (web + admin + Chatbot L1) ile Faz 2 (mobil — hem çalışan hem admin ekranları + Chatbot L2) birlikte `requirementAnalysis2.md`'deki **FR-01–FR-82'nin tamamını hem web hem mobil üzerinden** karşılar.

**Neden bu kapsam ve önemli bir uyarı:** Mobil parity ve RAG/LLM chatbot, Faz 1 planlamasında "tek başına ayrı proje büyüklüğünde" olarak değerlendirilmişti (bkz. `sprintPlan.md` Bölüm 1). Admin mobil ekranlarının da eklenmesiyle kapsam bir kez daha büyüdü — Bölüm 2 ve `issuePhase2.md`'deki güncel story point toplamları (B ve C için Hafta 0 dahil ~70 SP) bunu net gösteriyor. Bu, aynı 3 kişi/4 haftalık ekip için sıkı bir hedef. Bölüm 5'teki fallback descope sırası bu yüzden **admin mobil ekranlarını en önce kesilecek** katman olarak işaretliyor — bunların web'de zaten tam çalışan bir karşılığı var, ertelenmeleri hiçbir işlevi tamamen ortadan kaldırmaz.

---

## 2. Ekip ve Sorumluluk Alanları (Faz 1'deki Modül Sahipliğinin Devamı)

| Kişi | Faz 2 Modülleri | Kapsam |
|---|---|---|
| **Geliştirici A** | Chatbot L2 (RAG + LLM + dosya yükleme + sesli giriş + citation) | Faz 1'de BP-02'yi kurduğu için devam ediyor; en riskli/karmaşık tek parça |
| **Geliştirici B** | Mobil altyapı (Expo iskelet, Auth, Location/Maps) + Rehberler + Servis (çalışan + admin, gerçek Maps) + Araç Rezervasyonu (çalışan + admin) mobil ekranları + BP-10'dan Çalışan/Departman CRUD mobil | Faz 1'de BP-05/04/06 sahibiydi; şimdi aynı domainlerin admin mobil ekranlarını da kurar |
| **Geliştirici C** | Push notification altyapısı + Menü (çalışan + admin) + Çalışma Düzeni (My Schedule) + Anket (çalışan + admin) + Duyuru (çalışan + admin) mobil ekranları + BP-10'dan RBAC/Rapor mobil | Faz 1'de BP-03/09/07/08 sahibiydi; şimdi aynı domainlerin admin mobil ekranlarını da kurar |

Her kişi kendi modülünde mobil UI + gerekli backend eklentilerinin (yeni endpoint, push dispatch, embedding job vb.) tamamından sorumludur. Backend'in büyük kısmı Faz 1'de zaten kuruldu — bu fazda asıl iş mobil istemci katmanı ve Chatbot'un AI altyapısı.

---

## 3. Hafta 0 — Ortak Temel (Tüm Ekip, 2-3 gün, paralel başlamadan önce)

- [ ] **F2-ISSUE-000:** Expo mobil app iskeleti (`apps/mobile` pnpm workspace'e eklenir, NativeWind, React Navigation kurulumu)
- [ ] **F2-ISSUE-001:** Mobil Auth entegrasyonu (login ekranı, `expo-secure-store` ile token saklama, auth guard, session refresh) — mevcut `/auth/*` endpoint'lerini yeniden kullanır, yeni backend işi yok
- [ ] **F2-ISSUE-002:** Push notification altyapısı (Firebase FCM projesi kurulumu, `expo-notifications`, backend'e yeni `POST /me/device-tokens` endpoint'i)
- [ ] **F2-ISSUE-003:** Location/Maps altyapısı (`expo-location` + `expo-task-manager` izin akışı, Google Maps API proje/key kurulumu)
- [ ] **F2-ISSUE-004:** RAG/AI altyapısı (Ollama Docker container kurulumu, pgvector embedding job iskeleti, dosya depolama için MinIO container aktivasyonu)
- [ ] **F2-ISSUE-005:** CI güncellemesi (mobil build/test job'ı — EAS build pipeline'a eklenir)

**Çıktı:** Boş ama gezilebilir, giriş yapılabilir mobil app kabuğu; push/location izinleri çalışır durumda; Ollama+pgvector+MinIO ayakta. Bu olmadan hiçbir modül gerçek cihazda test edilemez.

---

## 4. Haftalık Plan (Hafta 1-4)

| Hafta | Geliştirici A | Geliştirici B | Geliştirici C |
|---|---|---|---|
| 1 | Ollama LLM entegrasyonu + PolicyVersion embedding pipeline (pgvector) | Auth/Dashboard + Rehberler mobil ekranları | Menü mobil ekranı + My Schedule mobil ekranı |
| 2 | RAG retrieval servisi + citation (`CHAT_CITATION`) entegrasyonu | Servis mobil ekranı + gerçek Google Maps entegrasyonu (FR-27) + arkaplan konum takibi | Anket/Feedback mobil ekranı + Duyuru mobil ekranı + Menü admin mobil (Excel yükleme) |
| 3 | Dosya yükleyerek soru sorma (FR-15) + sesli giriş (FR-16) + Chatbot mobil UI | Araç Rezervasyonu mobil ekranı + Servis Yönetimi admin mobil | Push bildirim dispatch entegrasyonu (FCM) + Bildirim tercihleri ekranı + Duyuru Yönetimi admin mobil |
| 4 | RAG doğruluk/performans tuning (NFR-02 riski), entegrasyon, demo senaryoları | Araç Yönetimi admin mobil + Çalışan/Departman CRUD admin mobil (BP-10) + entegrasyon (azaltılmış buffer) | Anket Yönetimi admin mobil + RBAC/Rapor admin mobil (BP-10) + entegrasyon (azaltılmış buffer) |

**Hafta 4 artık salt buffer değil** — admin mobil ekranlarının çoğu Hafta 3-4'e yığıldı, bu yüzden entegrasyon/bug-bash süresi Faz 1'e göre daha kısıtlı. B ve C için Hafta 3-4 en yoğun haftalar (bkz. `issuePhase2.md` Bölüm 2, güncel SP dağılımı); Hafta 2 sonunda ilerleme netleşmezse Bölüm 5'teki fallback sırası devreye girmeli.

---

## 5. Kritik Riskler, Bağımlılıklar ve Fallback Descope Sırası

- **En büyük risk — kapsam büyüklüğü:** Mobil parity (çalışan + admin) + RAG/LLM chatbot'u aynı 4 haftada bitirmek, Faz 1'in kendi gerekçesinde "ayrı ayrı proje büyüklüğü" olarak tanımlanan işleri birleştiriyor; admin mobil ekranlarının eklenmesiyle yük bir kez daha arttı. Hafta 2 sonunda ilerleme gözden geçirilmeli; gecikme varsa aşağıdaki sırayla kapsam daraltılmalı:
  1. **Admin mobil ekranları (Servis/Araç/Menü/Anket/Duyuru Yönetimi + BP-10 CRUD/RBAC/Rapor mobil)** — en önce kesilecekler, çünkü hepsinin web'de zaten tam çalışan bir karşılığı var; ertelenmeleri hiçbir işlevi tamamen ortadan kaldırmaz, yöneticiler geçici olarak web admin paneli kullanmaya devam eder.
  2. **A2-6 Sesli giriş (FR-16)** — izole bir mobil-client özelliği, kesilirse RAG/dosya yükleme etkilenmez.
  3. **A2-5 Dosya yükleyerek soru sorma (FR-15)** — RAG temel akışı (metinle soru-cevap) çalışır durumda kalır, sadece dosya eki ertelenir.
  4. **B2-4 Gerçek Google Maps entegrasyonu** — Faz 1'deki basit Haversine hesaplamasına geri dönülebilir, FR-27 "tam" değil ama fonksiyonel kalır.
  5. **A2-4 Citation/kaynak gösterme** — RAG yanıt üretimi çalışır, sadece "hangi dokümandan" bilgisi ertelenir (FR-14 temel kısıtı hâlâ karşılanır, sadece UI'da görünürlük ertelenir).
- **NFR-02 riski (chatbot 5 sn yanıt):** RAG retrieval + LLM generation, template tabanlı L1'den çok daha yavaş. A-1/A-3 tamamlanır tamamlanmaz gerçekçi yük testi yapılmalı; gerekirse streaming yanıt (kullanıcı LLM üretirken kısmi metni görür) NFR-02'yi "algılanan" hızla karşılamak için değerlendirilmeli.
- **Mobil admin girişinde 2FA:** Admin ekranları artık mobile de taşındığı için NFR-07 (yöneticiler için iki faktörlü kimlik doğrulama) mobil admin girişinde de uygulanmalı — F2-ISSUE-001 kapsamında admin rolündeki kullanıcılar için mobilde de `/auth/2fa/verify` adımı zorunlu kılınmalı. Çalışan (employee) girişinde 2FA yok.
- **MinIO aktivasyonu:** `tech_stack.md`'de "Future" işaretliydi; dosya yükleme (FR-15) ve KB doküman ekleri artık gerçek obje depolamaya ihtiyaç duyuyor. Hafta 0'da aktive edilmezse A-5 bloklanır.
- **Cihaz testi:** Hafta 4'te hem iOS hem Android'de gerçek cihaz/simülatör testi zaman alır; TestFlight/Play Store internal testing kanalları Hafta 3 sonunda hazır olmalı, Hafta 4'e bırakılmamalı.

---

## 6. Faz 3+ Backlog (Henüz Planlanmadı — Ayrı Gereksinim Analizi Gerekli)

`requirementAnalysis2.md` Bölüm 12'deki 14 madde bu fazın kapsamı dışında bırakıldı çünkü hiçbirinin FR/BP karşılığı yok — sprint planına dökülebilmeleri için önce `requirementAnalysis2.md` tarzı bir gereksinim analizi (yeni FR'ler, BP-11+ süreç haritalama, yeni endpoint'ler, ER diagram güncellemesi) yapılmalı:

- Çevrim içi izin talep iş akışı, fazla mesai talep/onay iş akışı (BP-02'nin mevcut "bilgi verme" işlevinin ötesinde gerçek bir workflow)
- Masraf yönetimi
- Toplantı odası rezervasyonu (BP-06 Araç Rezervasyonu'na benzer bir model kullanılabilir)
- Devam takibi, QR tabanlı çalışan giriş kontrolü
- Şirket takvimi, Microsoft Teams, Slack entegrasyonları
- Doküman yönetim sistemi (KB'nin ötesinde genel amaçlı)
- Yapay zeka destekli analitik, tahmine dayalı servis optimizasyonu, yapay zeka destekli İK asistanı

**Öneri:** Faz 2 tamamlandıktan sonra, bu listeden 2-3 maddeyi iş değeri/karmaşıklık matrisiyle önceliklendirip ayrı bir gereksinim analizi turu başlatılmalı (Faz 3 planı, bu analiz olmadan anlamlı şekilde issue'lara bölünemez).

---

## 7. İlişkili Dokümanlar

- `sprintPlan.md` / `issue.md` — Faz 1 (MVP) planı ve issue kırılımı
- `issuePhase2.md` — Bu planın atanabilir issue seviyesindeki detayı (story point dahil)
- `apiEndpoints.md`, `diagrams/ERDiagram.png`, `tech_stack.md` — Faz 1'de kurulan ve bu fazda yeniden kullanılan altyapı
