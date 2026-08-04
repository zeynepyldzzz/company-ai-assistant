-- B-22: sirket Izmir'de calisiyor, ornek servis listesinde artik sadece
-- Izmir guzergahlari (V42) gorunsun diye V18'deki Istanbul guzergahlari
-- kaldirildi. shuttle_stop.route_id ON DELETE CASCADE oldugu icin (V1)
-- durak kayitlari ayrica silinmeden otomatik temizlenir.

DELETE FROM shuttle_route WHERE plate_number IN ('34 SR 101', '34 SR 202');
