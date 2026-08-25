# company-ai-assistant

Şirket içi yapay zekâ destekli asistan platformu. Çalışanlara chatbot üzerinden
kurum bilgisi, yemek menüsü, servis güzergâhları, araç rezervasyonu, duyurular,
anketler ve çalışma düzeni bilgisi sunar; yöneticiler için ayrı bir yönetim
paneli içerir.

Dil modeli **sunucunun kendisinde** çalışır (Ollama); sohbet metinleri ve çalışan
verisi dışarıya gönderilmez.

## Modüller

Modüler monolit mimarisi: `Auth`, `Directory` (rehber), `Menu` (yemekhane),
`Shuttle` (servis), `Vehicle` (araç/rezervasyon), `Survey` (anket),
`Announcement` (duyuru), `WorkSchedule` (çalışma düzeni), `Chatbot`.

**Chatbot yaklaşımı:** Intent sınıflandırma embedding benzerliğiyle yapılır,
cevaplar önceden tanımlı şablonlardan üretilir — LLM cevap **üretmez**. Bu
bilinçli bir karardır: bir İK asistanında en tehlikeli hata "doğru formatta
yanlış bilgi"dir ve kullanıcının bunu fark etme şansı yoktur.

## Teknoloji yığını

### Backend

| Teknoloji | Sürüm |
|---|---|
| Java (Eclipse Temurin) | 21 |
| Spring Boot | 4.1.0 |
| Spring Security | 7.1.0 |
| Maven (wrapper) | 3.9.16 |
| Tomcat (embedded) | 11.0.22 |
| Flyway | 12.4.0 |
| PostgreSQL JDBC | 42.7.11 |
| Jackson (`tools.jackson`) | 3.1.4 |
| springdoc-openapi (Swagger UI) | 3.0.1 |
| Apache POI (`poi-ooxml`) | 5.3.0 |
| jjwt (JWT) | 0.13.0 |
| `dev.samstevens.totp` (2FA) | 1.7.1 |

### Frontend

| Teknoloji | Sürüm |
|---|---|
| React / React DOM | 19.2.7 |
| TypeScript | 6.0.3 |
| Vite | 8.1.4 |
| Tailwind CSS | 4.3.3 |
| React Router | 8.2.0 |
| TanStack Query | 5.101.2 |
| Base UI React | 1.6.0 |
| shadcn | 4.13.0 |
| Leaflet / react-leaflet | 1.9.4 / 5.0.0 |
| lucide-react | 1.24.0 |
| sonner | 2.0.7 |
| next-themes | 0.4.6 |
| clsx / tailwind-merge / CVA | 2.1.1 / 3.6.0 / 0.7.1 |
| Node.js | 22 |
| pnpm | 9 |

Ortak paket (`packages/shared`): **zod 4.4.3** — doğrulama şemaları web ve
backend sözleşmesi arasında paylaşılır.

### Veritabanı ve altyapı

| Bileşen | Sürüm | İmaj |
|---|---|---|
| PostgreSQL | 16.14 | `pgvector/pgvector:pg16` |
| pgvector (vektör arama) | 0.8.5 | — |
| pg_trgm (bulanık metin arama) | 1.6 | — |
| Ollama | 0.32.1 | `ollama/ollama:0.32.1` |
| Embedding modeli | bge-m3 (1.2 GB) | — |
| OSRM (rota motoru) | — | `ghcr.io/project-osrm/osrm-backend` |
| nginx (production) | 1.27 | `nginx:1.27-alpine` |

### Geliştirme araçları

ESLint 10.7.0, typescript-eslint 8.63.0, Prettier 3.9.5,
openapi-typescript 7.13.0 (Swagger'dan TS tipi üretimi), GitHub Actions (CI).

### Harici servis

**Nominatim** (OpenStreetMap) — adres/koordinat çözümleme. Projedeki tek dış
servis çağrısıdır; rota hesabı (OSRM) ve dil modeli (Ollama) kendi sunucumuzda
çalışır.

## Mimari notlar

- **Veri erişimi `JdbcTemplate` ile yapılır, JPA/Hibernate kullanılmaz.**
  `spring-boot-starter-data-jpa` bağımlılık ağacında görünür ve Hibernate 7.4.1
  gelir, ancak repository'ler JDBC desenini izler.
- **API context path:** `/api/v1`
- **Kimlik doğrulama:** JWT (access + rotasyonlu refresh token). Admin rolleri
  için ek olarak TOTP tabanlı 2FA.
- **Roller:** `employee`, `hr_admin`, `fleet_admin`, `shuttle_admin`,
  `canteen_admin`, `system_admin`
- **Kullanıcı tablosu `employee`** (`users` değil).
- **Hata formatı:** `{ error: { code, message } }`
- **Liste yanıtları:** `{ data, page, pageSize, total }` zarfı

## Geliştirme ortamı

### Gereksinimler

- JDK 21
- Node.js 22 + pnpm 9
- Docker

### Kurulum

```bash
pnpm install
docker compose up -d
```

`db` (PostgreSQL + pgvector) ve `ollama` doğrudan ayağa kalkar.

**OSRM verisi** git'te tutulmaz (413 MB), her geliştirici bir kez üretir:

```bash
./scripts/prepare-osrm-data.sh
docker compose up -d osrm
```

Bu adım atlanırsa hata alınmaz — backend sessizce kuş uçuşu mesafeye düşer ve
haritadaki rotalar gerçek yolu takip etmez.

### Çalıştırma

```bash
cd apps/api && ./mvnw spring-boot:run    # http://localhost:8080/api/v1
pnpm --filter web dev                    # http://localhost:5173
```

Windows'ta Maven komutları `apps\api` içinden `.\mvnw` ile, git komutları repo
kökünden çalıştırılır.

### Lint ve format

```bash
pnpm lint
pnpm format
```

## Production kurulumu

Tüm bileşenler konteynerde çalışır; sunucuya Java, Node veya PostgreSQL kurmaya
gerek yoktur:

```bash
cp .env.example .env      # DB_PASSWORD ve JWT_SECRET doldurulur
./scripts/prepare-osrm-data.sh
docker compose -f docker-compose.prod.yml up -d --build
```

Doğrulama adımları ve kurulum sonrası **zorunlu güvenlik adımı** için
[docs/deployment.md](docs/deployment.md) okunmalıdır.

## Proje yapısı

```
apps/api          Spring Boot backend (com.company.assistant)
apps/web          React + Vite + TypeScript arayüz
packages/shared   Zod şemaları (web/backend sözleşmesi)
infra/nginx       Production reverse proxy yapılandırması
infra/osrm        OSRM kaynak harita verisi (.osm.pbf)
scripts           Kurulum yardımcıları
docs              Gereksinim analizi, API sözleşmesi, backlog, katkı kuralları
```

## Test

```bash
cd apps/api && ./mvnw verify    # CI'ın koştuğu komut
```

`*IT` testleri surefire tarafından toplanmaz, ortam değişkeniyle elle koşulur:

```bash
INTENT_IT=true ./mvnw test -Dtest=<TestAdi>IT
```

## Katkı

Her değişiklik issue → branch → PR akışından geçer; `docs/` altındaki
değişiklikler doğrudan `main`'e push edilebilir. Merge için takımdan bir onay
gerekir, yazar kendi PR'ını onaylayamaz.

Ayrıntılar: [docs/Contributing.md](docs/Contributing.md)

## Dokümantasyon

| Dosya | İçerik |
|---|---|
| [docs/Contributing.md](docs/Contributing.md) | Katkı kuralları, branch/PR akışı |
| [docs/apiEndpoints.md](docs/apiEndpoints.md) | API sözleşmesi |
| [docs/issue.md](docs/issue.md) | Backlog (faz 1) |
| [docs/issuePhase2.md](docs/issuePhase2.md) | Backlog (faz 2) |
| [docs/sprintPlan.md](docs/sprintPlan.md) | Sprint planı |
| [docs/requirementAnalysis2.md](docs/requirementAnalysis2.md) | Gereksinim analizi |
| [docs/businessProcessMapping.md](docs/businessProcessMapping.md) | İş süreçleri |
| [docs/deployment.md](docs/deployment.md) | Sunucu kurulumu |
