# company-ai-assistant

An AI-powered internal assistant platform with mobile, web, and admin applications that enhances workplace communication and productivity by providing intelligent chatbot support, company knowledge access, employee services, announcements, transportation management, meal menus, surveys, and comprehensive administrative tools.

## Nasıl Çalıştırılır

### Gereksinimler
- Node.js 20+ (öneri: 22 LTS)
- pnpm 9+ (`npm install -g pnpm`)
- JDK 21 (apps/api için, henüz kurulmadı)

### Kurulum
```bash
pnpm install
```

### Geliştirme
```bash
pnpm --filter web dev   # Web uygulaması (localhost:5173)
```

### Servisler (Docker)
```bash
docker compose up -d
```
`db` (Postgres) ve `ollama` servisleri direkt ayağa kalkar. `osrm` servisi (gerçek yol mesafesi/rota geometrisi) farklıdır: verisi git'e commit'lenmez, her geliştiricinin bir kere kendi makinesinde üretmesi gerekir:
```bash
./scripts/prepare-osrm-data.sh   # osrm-data/ klasorunu uretir (birkac dakika surer, docker gerektirir)
docker compose up -d osrm
```
Bu adım atlanırsa `osrm` container'ı veri bulamaz ve backend sessizce haversine (kuş uçuşu düz çizgi) hesabına döner — rota/harita gerçek yolu takip etmeyip yanlış görünür.

### Lint & Format
```bash
pnpm lint     # ESLint ile kod denetimi
pnpm format   # Prettier ile formatlama
```
