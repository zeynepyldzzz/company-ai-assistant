# A-5 — İK Prosedürü Yönlendirmesi: Tasarım Kararları

Bu not, A-5 (İK Prosedürü Template Yönlendirmesi) kapsamında alınan ve A-6 ile Faz 2'yi
doğrudan etkileyen kararları kayıt altına alır. Amaç, A-6'da migration'ın yeniden yazılmasını
gerektirecek bir tasarım borcunun oluşmamasıdır.

## 1. Veri modeli: üç katman

`hr_procedure` → `policy_document` → `policy_version`

| Tablo | Sorumluluk |
| --- | --- |
| `hr_procedure` | Prosedürün kimliği: başlık, topic (`category`), sorumlu birim, chatbot intent eşlemesi |
| `policy_document` | Dokümanın kimliği. Versiyonlar arasında sabit kalan id — `/documents/{id}/versions` bunun üzerine kurulur |
| `policy_version` | Versiyonlanan içerik: `content`, `steps`, `file_path`, `effective_date`, `is_current` |

### ER diyagramından sapma

`docs/diagrams/ERDiagram.png` tek bir `POLICY_DOCUMENT` tablosu gösteriyordu (version,
effective_date, is_current aynı satırda). Üç katmana geçildi. Gerekçeler:

- A-6 kabul kriteri `GET /admin/knowledge-base/documents/{id}/versions` versiyon geçmişi
  döndürüyor — tek tabloda `{id}` zaten bir versiyonu işaret eder, uç anlamsızlaşır.
- A-5 kabul kriteri `PolicyVersion.isCurrent=true` diyor.
- Faz 2 A2-2 `PolicyVersion.content` alanının chunklanmasını istiyor — içerik versiyonda.
- Faz 2 A2-4 `CHAT_CITATION.policyVersionId` bekliyor.

**Yapılacak:** ER diyagramı bu modele göre güncellenmeli.

### `steps` — JSONB, versiyon üzerinde

`policy_version.steps jsonb NOT NULL DEFAULT '[]'`, şekil:

```json
[{ "order": 1, "title": "...", "detail": "..." }]
```

Adımlar versiyonlanır. ER'de `hr_procedure` üzerindeydi; orada kalsaydı prosedür güncellendiğinde
eski adımlar kaybolur, FR-58 ("yanıt güncel versiyona dayanır") ve NFR-06 denetim izi fiilen
sağlanamazdı. `jsonb_typeof(steps) = 'array'` CHECK ile şekil garanti altında.

### Sorumlu birim — `hr_procedure` üzerinde

`responsible_department_id` (FK → `department`) + `responsible_contact` (serbest metin).
Sahiplik prosedürün kimliğine aittir, içeriği gibi versiyonlanmaz. FK olduğu için departman
adı değiştiğinde yanıtlar otomatik güncel gelir; `ON DELETE SET NULL`.

### DB seviyesinde zorlanan kurallar

- `ux_policy_version_current` — `CREATE UNIQUE INDEX ... ON policy_version (document_id) WHERE is_current`.
  Bir dokümanın en fazla tek `is_current` versiyonu olabilir (FR-58). A-6 bu kuralı kodda
  ayrıca kontrol etmek zorunda değil.
- `uq_policy_version_no` — `(document_id, version_no)` tekil.
- FK'lar `RESTRICT`: `hr_procedure` ← `policy_document` ← `policy_version`.
  CASCADE olsaydı tek bir `DELETE` tüm versiyon geçmişini sessizce silerdi (NFR-06 ihlali).
- `policy_document.deleted_at` — A-6'nın `DELETE /documents/{id}` ucu **soft delete** yapacak.

### Faz 2 notu

`policy_document.embedding vector(1024)` kolonu V14'te düşürüldü. A2-2 embedding'i
`policy_version` chunk'larından üretecek; kolon yanlış katmandaydı ve boştu.

## 2. FR-54 — chatbot zinciriyle entegrasyon

**Karar: prosedür soruları mevcut intent akışından geçer.** Ayrı bir dal açılmadı.

Böylece A-4 kalibrasyon logu, eşik/fallback davranışı ve `matchedPhrase` mantığı olduğu gibi
korunur; `ChatMessageService` zinciri ikiye bölünmez.

### Eşleme

`hr_procedure.intent_id` (FK → `intents`, UNIQUE, `ON DELETE SET NULL`).

Topic slug'ları ve intent adları birbirinden türetilemiyor
(`onboarding` ↔ `ise_giris_oryantasyon`, `fazla-mesai` ↔ `fazla_mesai`), bu yüzden eşleme
şemada kolon olarak duruyor. `intents` tablosuna kolon eklenmedi — chatbot modülü ortak alan.

`IntentResult` intent **adını** taşıdığı için (id değil) okuma şöyle:

```sql
SELECT p.* FROM hr_procedure p
JOIN intents i ON i.id = p.intent_id
WHERE i.name = ?
```

`IntentResult` imzası değiştirilmedi; değiştirilseydi `IntentClassificationServiceTest` ve
`ChatMessageServiceTest` etkilenirdi. A-5, merge'lü chatbot koduna dokunmadan okuyabiliyor.

### Değişken üretimi

Yeni sınıf: `HrProcedureVariableResolver`. `ChatVariableResolver` değiştirilmedi
(modül sınırı korunuyor). `ChatMessageService` iki map'i merge eder.

Üretilen değişkenler:

| Değişken | Kaynak |
| --- | --- |
| `{{prosedur_basligi}}` | `hr_procedure.title` |
| `{{prosedur_adimlari}}` | `policy_version.steps` (numaralı metne dönüştürülür) |
| `{{sorumlu_departman}}` | `department.name` (JOIN) |
| `{{sorumlu_iletisim}}` | `hr_procedure.responsible_contact` |
| `{{gecerlilik_tarihi}}` | `policy_version.effective_date` |
| `{{versiyon}}` | `policy_version.version_no` |

`{{departman}}` (kullanıcının kendi departmanı, `ChatVariableResolver`'dan) ile
`{{sorumlu_departman}}` (prosedürün sorumlusu) farklı şeylerdir, karıştırılmamalı.

**Dikkat:** `TemplateRenderer` bilinmeyen placeholder'ı olduğu gibi bırakır. Prosedür
bulunamazsa kullanıcı ekranda düz `{{prosedur_adimlari}}` görür — bu durum servis
seviyesinde ele alınıp fallback template'ine düşülmeli.

## 3. Endpoint sözleşmesi

| Uç | Yanıt |
| --- | --- |
| `GET /hr/procedures` | 4 prosedürün listesi, `{ data, page, pageSize, total }` zarfı (apiEndpoints §0) |
| `GET /hr/procedures?topic=<slug>` | Tekil prosedür nesnesi |
| Geçersiz topic | 404 |
| `GET /hr/procedures/{id}` | Prosedür detayı + güncel versiyon/geçerlilik tarihi |

Topic slug'ları: `onboarding`, `izin`, `fazla-mesai`, `mazeret-izni`
(`hr_procedure.category`, UNIQUE).

Her yanıt `is_current = true` olan versiyona dayanır (FR-58) ve sorumlu departman/iletişim
bilgisi içerir (FR-57). Rol: `employee`.

## 4. Seed (V15) notları

- `mazeret_izni` intent'i yeni eklendi; diğer üçü (`ise_giris_oryantasyon`, `izin_prosedur`,
  `fazla_mesai`) V8'de zaten seed'lenmişti, template'leri statikten dinamiğe çevrildi.
- **`izin_prosedur`'un "mazeret izni için ne yapmam gerekiyor" örnek cümlesi `mazeret_izni`'ne
  taşındı.** Kalsaydı mazeret sorularının bir kısmını kendine çeker, FR-54 kabul kriteri
  sahada ihlal edilirdi. Metin değişmediği için embedding geçerli kaldı, yeniden hesaplanmadı.
- `izin_prosedur`, kaybettiği cümleyi telafi etmek için iki yıllık-izin cümlesiyle güçlendirildi.
- Yeni `intent_examples` satırları `embedding = NULL` yazıldı; `IntentSeedRunner` açılışta
  doldurur. Seed sonrası eksik embedding kontrolü yapılmalı — embedding üretimi patlarsa intent
  sessizce hiç eşleşmez.
- Seed, `Insan Kaynaklari` departmanı veya intent eşlemesi bulunamazsa `RAISE EXCEPTION` ile
  durur; sessiz `NULL` yazmaz.
- **Prosedür adımları örnek içeriktir.** Gerçek İK metinleri A-6 admin CRUD'u ile İK tarafından
  yüklenecektir.

## 5. Kapsam dışı

- A-6: `PolicyDocument`/`PolicyVersion` admin CRUD — ayrı issue.
- Faz 2: chunk + embedding pipeline (A2-2), citation (A2-4).
