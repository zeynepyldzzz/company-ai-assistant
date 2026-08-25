# Sunucu Kurulumu

Uygulamanın production ortamına kurulması. Tüm bileşenler konteynerde çalışır;
sunucuya Java, Node veya PostgreSQL kurulmaz.

## 0. Gereksinimler

| Kaynak | Değer | Neden |
|---|---|---|
| RAM | 8 GB (min 4, rahat 16) | Ollama modeli tek başına ~2 GB; üstüne Postgres, JVM, OSRM |
| Disk | 40 GB | OSRM verisi 413 MB, model ~1.2 GB, imajlar ~2 GB, kalanı DB + log |
| CPU | 4 çekirdek | Embedding üretimi CPU'da koşuyor |
| OS | Ubuntu 22.04 / 24.04 | |
| Ağ | Dışarı internet erişimi | İmajlar ve model ilk kurulumda inecek |

Sunucunun dışarı erişimi yoksa imajlar ve model elle taşınmalıdır; bu adım
atlanırsa kurulum tamamlanamaz.

## 1. Docker kurulumu

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER   # oturumu kapatıp açmak gerekir
```

## 2. Depoyu klonla

```bash
git clone <repo-url>
cd company-ai-assistant
```

## 3. Ortam değişkenleri

```bash
cp .env.example .env
```

`DB_PASSWORD` ve `JWT_SECRET` doldurulmalı. Güçlü değer üretmek için:

```bash
openssl rand -base64 48
```

`.env` dosyası git'e girmez ve sunucu dışına çıkarılmaz.

## 4. OSRM harita verisi

Bu veri git'te tutulmaz (413 MB), her kurulumda bir kez üretilir:

```bash
./scripts/prepare-osrm-data.sh
```

Birkaç dakika sürer. **Bu adım atlanırsa kurulum hata vermez** — backend sessizce
kuş uçuşu mesafe hesabına düşer ve haritadaki rotalar gerçek yolu takip etmez.

## 5. Yığını başlat

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

İlk çalıştırma uzun sürer: imajlar derlenir, ardından embedding modeli (~1.2 GB)
iner. Model inmeden `api` başlamaz — bu bilinçli bir sıralamadır (bkz. adım 6.3).

## 6. Doğrulama

Bu bölüm atlanmamalıdır. Aşağıdaki hataların hiçbiri uygulamayı çökertmez;
sistem sağlıklı görünürken yanlış çalışır.

### 6.1 Servisler

```bash
docker compose -f docker-compose.prod.yml ps
```

Beklenen: `db`, `ollama`, `osrm`, `api`, `web` çalışıyor. Dışarıya açık tek port
`80` olmalı; `db`, `ollama`, `osrm` ve `api` yalnızca konteyner ağında görünür.

```bash
docker inspect assistant-ollama-init --format "{{.State.ExitCode}}"
```

`0` dönmeli — model indirme görevi başarıyla tamamlanmış demektir.

### 6.2 Veritabanı şeması

```bash
docker logs assistant-api 2>&1 | grep -i "migrations"
```

Beklenen: **`Successfully applied 55 migrations`**

`Successfully validated 55 migrations` görüyorsan şema zaten kuruluydu; ilk
kurulumda `applied` yazmalıdır.

### 6.3 Embedding seed — en kritik kontrol

```bash
docker logs assistant-api 2>&1 | grep "embedding seed"
```

Beklenen: **`Intent embedding seed tamamlandi: N/N kayit.`** — iki sayı **eşit**
olmalıdır.

Eşit değilse aradaki fark, sınıflandırmada hiç eşleşmeyecek intent örneği
sayısıdır. Chatbot hata vermez, sadece o soruları tanımaz. Kesin kontrol:

```bash
docker exec assistant-db psql -U app_user -d company_ai_assistant -t \
  -c "SELECT count(*) FROM intent_examples e JOIN intents i ON i.id=e.intent_id
      WHERE e.embedding IS NULL AND i.is_virtual=false;"
```

`0` dönmelidir. Dönmüyorsa Ollama erişilebilir olduğundan emin olup `api`
konteynerini yeniden başlat — `IntentSeedRunner` yalnızca boş kalan satırları
yeniden dener:

```bash
docker compose -f docker-compose.prod.yml restart api
```

### 6.4 Uçtan uca

```bash
curl -o /dev/null -w "%{http_code}\n" http://localhost/                    # 200
curl -o /dev/null -w "%{http_code}\n" http://localhost/duyurular           # 200
curl -o /dev/null -w "%{http_code}\n" http://localhost/api/v1/auth/session # 401
```

Üçüncüsünde `401` beklenir ve doğrudur: istek nginx'i geçip Spring Security'ye
ulaşmış demektir. `404` gelirse nginx proxy yapılandırması hatalıdır.

## 7. Demo hesaplarının kapatılması (ZORUNLU)

**Bu adım tamamlanmadan sunucu dışarıya açılmamalıdır.**

Seed'ler migration olarak yazıldığı için production veritabanında da koşarlar —
demo hesaplar sunucuda kendiliğinden oluşur ve kimse silmezse orada kalır.
Kimlik bilgileri depoda açıkça yazılıdır:

- `admin@company.com` / `Passw0rd!` — `system_admin` rolünde (V3)
- Aynı hesabın TOTP anahtarı da düz metin olarak commit'li (V6)

İkisi bir arada olduğu için 2FA koruma sağlamaz: depoya erişimi olan herkes
geçerli kod üretip tam yetkiyle girebilir.

### 7.1 Gerçek admin hesabını oluştur

Sunucu henüz dışarıya kapalıyken, `http://localhost` üzerinden
`admin@company.com` ile gir (TOTP kodu depodaki anahtardan üretilir), yönetim
panelinden kendi admin hesabını oluştur. Sistem geçici şifre üretir.

Çıkış yap, yeni hesabınla gir: ilk girişte şifre değişimi istenir ve 2FA için
yeni bir QR kod gösterilir — kendi authenticator uygulamana kaydet.

### 7.2 Demo hesapları devre dışı bırak

Yeni admin hesabınla giriş yaptıktan sonra, yönetim panelindeki çalışan
listesinden `admin@company.com` ve `calisan@company.com` kayıtlarını **sil**.

Buton "Sil" yazsa da kayıt gerçekten silinmez: `AdminEmployeeService`
`is_active = false` yapar (soft delete). Bu bilinçlidir — `employee` tablosuna
11 farklı tablodan foreign key referansı vardır (`department.manager_id`,
`weekly_schedule`, `announcement.published_by` ...), gerçek silme bunlara
takılırdı. Pasif hesapla giriş, oturum açma ve token yenileme engellidir.

Panele erişilemiyorsa aynı işlem doğrudan veritabanından yapılabilir:

```bash
docker exec -i assistant-db psql -U app_user -d company_ai_assistant <<'SQL'
UPDATE employee SET is_active = false
WHERE email IN ('admin@company.com', 'calisan@company.com');
SQL
```

### 7.2.1 Doğrulama

```bash
docker exec assistant-db psql -U app_user -d company_ai_assistant -t \
  -c "SELECT email, is_active FROM employee
      WHERE email IN ('admin@company.com','calisan@company.com');"
```

İkisi de `f` dönmelidir.

### 7.3 Kalan demo verisi

Sahte çalışanlar, örnek menü, araçlar ve güzergâhlar da seed'den gelir. Bunlar
güvenlik riski değildir; gerçek veri girildikçe yönetim panelinden düzeltilir.

**Silinmemesi gerekenler:** intent'ler, intent örnekleri ve yanıt şablonları.
Bunlar demo değil sistem verisidir — silinirse chatbot hiçbir soruyu tanımaz.

## 8. İşletim

```bash
docker compose -f docker-compose.prod.yml logs -f api    # log takibi
docker compose -f docker-compose.prod.yml restart api    # yeniden başlatma
docker compose -f docker-compose.prod.yml down           # durdurma (veri korunur)
```

Servisler `restart: unless-stopped` ile tanımlı; sunucu yeniden başladığında
kendiliğinden ayağa kalkarlar.

### Güncelleme

```bash
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

Yeni migration varsa `api` açılışta uygular. Güncelleme sonrası 6.2 ve 6.3
kontrolleri tekrarlanmalıdır.

### Yedekleme

Veriler `pgdata` ve `ollama_data` volume'lerinde tutulur. Veritabanı yedeği:

```bash
docker exec assistant-db pg_dump -U app_user company_ai_assistant \
  | gzip > backup-$(date +%F).sql.gz
```

## 9. Henüz yapılmamış

- **TLS/HTTPS** — sunucu dışarıya açılacaksa `infra/nginx/default.conf` içine 443
  bloğu ve sertifika eklenmelidir. Şu an yalnızca HTTP (80) tanımlı.
- **OSRM imaj sürümü** — `docker-compose.prod.yml` içinde `latest` etiketiyle
  duruyor, sabit sürüme geçilmelidir.
