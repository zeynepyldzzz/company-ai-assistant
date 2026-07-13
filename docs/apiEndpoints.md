# API Endpoint Tanımları (REST API Specification)
## Yapay Zeka Destekli Şirket İçi Asistan — Mobil ve Web Uygulaması

> Kaynak: `requirementAnalysis2.md` (FR-01–FR-82, bölüm 8 Veri Gereksinimleri, bölüm 9 API'ler) ve `businessProcessMapping.md` (BP-01–BP-10). Her endpoint, ilgili iş sürecinin FR'lerine bağlanır. Taban yol: `/api/v1`. `/auth/login`, `/auth/2fa/verify` ve `/auth/refresh` dışındaki tüm endpoint'ler `Authorization: Bearer <token>` bekler. **Faz 2 (Mobil + Chatbot L2)** ile eklenen uçlar `(Faz 2)` etiketiyle işaretlenmiştir — kaynağı `issuePhase2.md`'dir; bunlar dışında Faz 2, Faz 1'de tanımlanan tüm uçları olduğu gibi yeniden kullanır.

---

## 0. Genel Kurallar

- **Base URL:** `/api/v1`
- **Kimlik doğrulama:** Bearer token (şirket SSO üzerinden alınan JWT); `/auth/login`, `/auth/2fa/verify` ve `/auth/refresh` hariç tüm endpoint'ler bu token'ı zorunlu kılar. Yönetici işlemlerinde ek olarak RBAC rol kontrolü uygulanır.
- **Roller:** `employee`, `admin` (alt tipler: `hr_admin`, `fleet_admin`, `shuttle_admin`, `canteen_admin`, `system_admin` — yetkilendirme rol bazlı daraltılabilir).
- **Sayfalama:** Liste dönen endpoint'ler `?page=&pageSize=` destekler, yanıt `{ data: [...], page, pageSize, total }` zarfı içinde döner.
- **Hata formatı:** `{ error: { code, message } }`, standart HTTP durum kodları (400/401/403/404/409/422/500).

---

## 1. BP-01 — Kimlik Doğrulama ve Kişiselleştirilmiş Ana Panel

| Method | Endpoint | Açıklama | Rol | FR |
|---|---|---|---|---|
| POST | `/auth/login` | Kurumsal kimlik bilgileriyle giriş | Herkes | FR-01, FR-02 |
| POST | `/auth/2fa/verify` | Yönetici girişinde iki faktörlü doğrulama | Yönetici | NFR-07 |
| POST | `/auth/refresh` | Oturum token'ını yeniler | Herkes | FR-03 |
| POST | `/auth/logout` | Oturumu sonlandırır | Herkes | FR-03 |
| GET | `/auth/session` | Aktif oturum + rol bilgisini döner | Herkes | FR-03, NFR-04 |
| GET | `/me` | Giriş yapan kullanıcının profil bilgisi | Herkes | FR-07 |
| GET | `/dashboard` | Kişiselleştirilmiş ana panel (hızlı erişim kartları, bekleyen bildirimler) | Çalışan | FR-04–06 |
| POST | `/me/device-tokens` | Mobil push bildirim cihaz token'ı kaydetme (Faz 2) | Herkes | FR-46, 65–66 |
| DELETE | `/me/device-tokens/{token}` | Cihaz token'ını kaldırma (Faz 2) | Herkes | FR-46, 65–66 |

---

## 2. BP-02 — Yapay Zeka Chatbot ve Bilgi Tabanı Yönetimi

| Method | Endpoint | Açıklama | Rol | FR |
|---|---|---|---|---|
| POST | `/chatbot/messages` | Soru gönderir (yazılı/sesli), yanıt döner | Çalışan | FR-08–10, 14, 16 |
| POST | `/chatbot/conversations/{id}/attachments` | Sohbete dosya yükler | Çalışan | FR-15 |
| GET | `/chatbot/conversations` | Sohbet geçmişi listesi | Çalışan | FR-09 |
| GET | `/chatbot/conversations/{id}` | Tek sohbetin detayı | Çalışan | FR-09 |
| GET | `/hr/procedures?topic=` | Prosedür/politika bazlı yönlendirme (onboarding, izin, fazla mesai, mazeret izni) | Çalışan | FR-11–13, 51–57 |
| GET | `/hr/procedures/{id}` | Prosedür detayı + güncel versiyon/geçerlilik tarihi | Çalışan | FR-58 |
| GET | `/admin/knowledge-base/documents` | Chatbot bilgi tabanı doküman listesi | HR/Sistem Yöneticisi | FR-77 |
| POST | `/admin/knowledge-base/documents` | Yeni bilgi tabanı / İK doküman yükleme | HR/Sistem Yöneticisi | FR-77–78 |
| PUT | `/admin/knowledge-base/documents/{id}` | Doküman/politika güncelleme (yeni versiyon) | HR/Sistem Yöneticisi | FR-78, 58 |
| DELETE | `/admin/knowledge-base/documents/{id}` | Doküman kaldırma | HR/Sistem Yöneticisi | FR-78 |
| GET | `/admin/knowledge-base/documents/{id}/versions` | Doküman versiyon geçmişi | HR/Sistem Yöneticisi | FR-58, 78 |

---

## 3. BP-03 — Yemek Menüsü Görüntüleme ve Yönetimi

| Method | Endpoint | Açıklama | Rol | FR |
|---|---|---|---|---|
| GET | `/menus/today` | Bugünün menüsü | Çalışan | FR-17 |
| GET | `/menus/weekly` | Haftalık menü | Çalışan | FR-18 |
| GET | `/meals/{id}` | Yemek detayı (kalori, alerjen) | Çalışan | FR-19, 20 |
| GET | `/admin/menus` | Yönetici menü listesi | Yemekhane Yönetimi | FR-21 |
| POST | `/admin/menus/import` | Excel şablonuyla menü yükleme | Yemekhane Yönetimi | FR-21, 72 |
| DELETE | `/admin/menus/{id}` | Menü kaydını kaldırma | Yemekhane Yönetimi | FR-21 |

---

## 4. BP-04 — Servis (Ulaşım) Bilgisi, Güzergah Önerisi ve Yönetimi

| Method | Endpoint | Açıklama | Rol | FR |
|---|---|---|---|---|
| GET | `/shuttle-routes` | Servis güzergahları listesi | Çalışan | FR-22 |
| GET | `/shuttle-routes/{id}/stops` | Güzergaha ait duraklar ve saatler | Çalışan | FR-23, 24 |
| GET | `/shuttle-routes/{id}/plate` | Güncel plaka bilgisi | Çalışan | FR-25 |
| GET | `/shuttle-routes/recommendation?lat=&lng=` | Varış noktasına göre önerilen güzergah + tahmini süre (GPS) | Çalışan | FR-26, 27 |
| POST | `/admin/shuttle-routes` | Yeni güzergah oluşturma | Servis Koordinatörü | FR-73 |
| PUT | `/admin/shuttle-routes/{id}` | Güzergah/durak/saat güncelleme | Servis Koordinatörü | FR-73 |
| PUT | `/admin/shuttle-routes/{id}/plate` | Plaka bilgisi güncelleme | Servis Koordinatörü | FR-73, 25 |

---

## 5. BP-05 — Çalışan / Departman / Telefon Rehberi Arama

| Method | Endpoint | Açıklama | Rol | FR |
|---|---|---|---|---|
| GET | `/employees?search=&department=&office=` | Çalışan arama/filtreleme | Çalışan | FR-28, 29 |
| GET | `/employees/{id}` | Çalışan detayı (ofis durumu, telefon, e-posta) | Çalışan | FR-30–33 |
| GET | `/departments?search=` | Departman arama | Çalışan | FR-34 |
| GET | `/departments/{id}` | Departman detayı (sorumluluk, yönetici, iletişim) | Çalışan | FR-35–37 |
| GET | `/phonebook?search=` | Dahili telefon rehberi | Çalışan | FR-48, 49 |
| POST | `/phonebook/{extension}/call` | Dahili numarayı arama tetikleme (click-to-call) | Çalışan | FR-50 |

---

## 6. BP-06 — Şirket Aracı Rezervasyonu ve Araç Yönetimi

| Method | Endpoint | Açıklama | Rol | FR |
|---|---|---|---|---|
| GET | `/vehicles?available=true` | Uygun (bakımda olmayan) araç listesi | Çalışan | FR-38, 41 |
| POST | `/reservations` | Araç rezervasyonu oluşturma | Çalışan | FR-39 |
| GET | `/reservations/me` | Kendi rezervasyonlarının durumu | Çalışan | FR-40 |
| DELETE | `/reservations/{id}` | Rezervasyon iptali | Çalışan | FR-39 |
| POST | `/admin/vehicles` | Araç ekleme | Araç Sorumlusu | FR-74 |
| PUT | `/admin/vehicles/{id}` | Araç güncelleme | Araç Sorumlusu | FR-74 |
| PUT | `/admin/vehicles/{id}/maintenance-status` | Bakım durumu işaretleme | Araç Sorumlusu | FR-74, 41 |

---

## 7. BP-07 — Anket Katılımı, Geri Bildirim ve Anket Yönetimi

| Method | Endpoint | Açıklama | Rol | FR |
|---|---|---|---|---|
| GET | `/surveys/active` | Katılıma açık anketler | Çalışan | FR-42 |
| POST | `/surveys/{id}/responses` | Anket yanıtı gönderme | Çalışan | FR-42 |
| POST | `/feedback` | Anonim geri bildirim gönderme | Çalışan | FR-43 |
| POST | `/admin/surveys` | Anket oluşturma | Yönetici | FR-76 |
| PUT | `/admin/surveys/{id}/publish` | Anketi yayımlama | Yönetici | FR-76 |
| GET | `/admin/surveys/{id}/results` | Anket sonuçlarını görüntüleme | Yönetici | FR-44 |

---

## 8. BP-08 — Duyuru ve Bildirim Görüntüleme, Yayımlama ve Yönetimi

| Method | Endpoint | Açıklama | Rol | FR |
|---|---|---|---|---|
| GET | `/announcements` | Duyuru listesi (sabitlenenler üstte) | Çalışan | FR-45, 47 |
| GET | `/announcements/{id}` | Duyuru detayı | Çalışan | FR-45 |
| GET | `/notifications` | Anlık/acil durum bildirimleri | Çalışan | FR-46, 65, 66 |
| PUT | `/notifications/preferences` | Bildirim tercihlerini güncelleme | Çalışan | FR-67 |
| POST | `/admin/announcements` | Duyuru oluşturma ve yayımlama | Yönetici | FR-75 |
| PUT | `/admin/announcements/{id}/pin` | Duyuruyu sabitleme | Yönetici | FR-47, 75 |
| PUT | `/admin/notifications/settings` | Bildirim/izin yönetimi (admin taraf) | Yönetici | FR-80 |

---

## 9. BP-09 — Haftalık Çalışma Düzeni Yönetimi ve İzleme

| Method | Endpoint | Açıklama | Rol | FR |
|---|---|---|---|---|
| GET | `/schedules/me` | Giriş yapan çalışanın haftalık `weeklySchedule` verisi | Çalışan | FR-63 |
| PUT | `/schedules/me` | Pazartesi–Cuma gün durumlarını (`office`/`remote`/`leave`) kaydetme | Çalışan | FR-60, 62 |
| GET | `/schedules/me/summary` | Haftalık özet (gün sayıları) | Çalışan | FR-61 |
| GET | `/admin/schedules` | Tüm çalışanların çalışma düzeni tablosu (salt okunur) | Yönetici | FR-79 |

> **Not:** `/schedules/me` tek kaynak (single source of truth) olarak tasarlanmalı; hem mobil, hem web `/my-schedule`, hem admin `/admin/schedules` bu uçtan beslenmelidir (FR-64, Kısıt).

---

## 10. BP-10 — Çalışan/Departman Yönetimi, Kullanıcı İzinleri ve Raporlama

| Method | Endpoint | Açıklama | Rol | FR |
|---|---|---|---|---|
| POST | `/admin/employees` | Çalışan oluşturma | İK Yöneticisi | FR-68 |
| PUT | `/admin/employees/{id}` | Çalışan güncelleme | İK Yöneticisi | FR-69 |
| DELETE | `/admin/employees/{id}` | Çalışan silme | İK Yöneticisi | FR-70 |
| POST | `/admin/departments` | Departman oluşturma | İK Yöneticisi | FR-71 |
| PUT | `/admin/departments/{id}` | Departman güncelleme | İK Yöneticisi | FR-71 |
| DELETE | `/admin/departments/{id}` | Departman silme | İK Yöneticisi | FR-71 |
| GET | `/admin/roles` | Rol/izin listesi | Sistem Yöneticisi | FR-80 |
| PUT | `/admin/users/{id}/roles` | Kullanıcıya rol/izin atama (RBAC) | Sistem Yöneticisi | FR-80 |
| GET | `/admin/reports/{type}` | Rapor oluşturma (ör. `usage`, `schedule`, `survey`) | Yönetici | FR-81 |
| GET | `/admin/reports/{id}/export?format=xlsx\|pdf` | Raporu dışa aktarma | Yönetici | FR-82 |

---

## 11. Sayı Özeti

| Business Process | Endpoint Sayısı |
|---|---|
| BP-01 (Faz 2: +2 `device-tokens`) | 9 |
| BP-02 | 11 |
| BP-03 | 6 |
| BP-04 | 7 |
| BP-05 | 6 |
| BP-06 | 7 |
| BP-07 | 6 |
| BP-08 | 7 |
| BP-09 | 4 |
| BP-10 | 10 |
| **Toplam (Faz 1: 71 + Faz 2: 2)** | **73** |

