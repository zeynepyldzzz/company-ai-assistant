-- A-24 (#155): tek kelimelik / kisa form sorgular.
--
-- Olculdu (chat_message_log, 2026-08-03, esik 0.68). IKI FARKLI durum cikti:
--
-- 1) Siralama DOGRU, yalnizca esik yetmiyor:
--      "Menüler"              0.656  en yakin: "menüdeki yemeklerin kalorisi ne kadar"
--      "Departmanları listele" 0.650 en yakin: "departman sorumlusuna nasıl ulaşırım"
--      "Yemekler"             0.669  |  "Departmanlar" 0.602  |  "Servisler" 0.585
--
-- 2) Siralama BOZUK — kisa girdiler SELAMLAMAYA kaciyor:
--      "Rehber"      0.646  en yakin: "merhaba"
--      "Hatlar"      0.498  en yakin: "merhaba"
--      "Duraklar"    0.494  en yakin: "iyi günler"
--
-- Ikinci grup A-19'daki "carsamba 0.611 ile selamlamaya yakin" bulgusunun aynisi: tek
-- kelimelik girdi kendi kategorisinde KISA ornek bulamayinca, anlamdan bagimsiz olarak
-- selamlama cumlelerine dusuyor — cunku selamlama ornekleri de kisa. UZUNLUK BENZERLIGI
-- ANLAMI BASTIRIYOR. Cozum ikisi icin de ayni: kendi kategorisine kisa ornek eklemek.
--
-- EMIR KIPI beklendigi kadar eksik degilmis: "Anketleri listele" 0.873, "Duyuruları göster"
-- 0.812, "Çalışanları listele" 0.827 — A-20'de eklenen ornekler bunlari dolayli olarak
-- kurtarmis. Eksik olan tek yer rehber_departman'di, o da asagida kapatiliyor.
--
-- "Prosedürler" BILEREK KAPSAM DISI: sistemde dort ayri prosedur kategorisi var (izin,
-- fazla mesai, oryantasyon, mazeret) ve bu sorgu hangisini kastettigini soylemiyor.
-- intent_bulunamadi donmesi burada DOGRU davranis — A-22'de eklenen oneri chip'leri
-- devreye giriyor ve kullanici hangi proseduru sorabilecegini goruyor. Belirsiz soruyu
-- rastgele bir kategoriye zorlamaktansa secenek sunmak daha iyi.
--
-- YONTEM KURALI (V30'dan): test sorgusunun KENDISI ornek olarak eklenmez, yoksa test 1.000
-- ile gecer ve hicbir sey olcmus olmayiz. Ayni kalip, farkli cumle.
--
-- CAPRAZ KIRLENME: kisa formlar kisa olduklari icin BIRBIRLERINE de yakin duruyor
-- ("Servisler" ~ "Departmanlar"). Ayirt edici olan tek sey alan kelimesi, o yuzden her
-- ornekte alan kelimesi acikca geciyor. IntentCalibrationIT'ye hem yeni vakalar hem
-- mevcut davranisi koruyan nobetciler eklendi.

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    -- servis_guzergah: "Servisler" 0.585, "Hatlar" 0.498, "Duraklar" 0.494
    ('servis_guzergah', 'servis listesi'),
    ('servis_guzergah', 'hangi hatlar var'),
    ('servis_guzergah', 'duraklar neler'),

    -- yemek_menusu: "Menüler" 0.656, "Yemekler" 0.669
    ('yemek_menusu', 'menü listesi'),
    ('yemek_menusu', 'bugünkü yemekler'),

    -- rehber_departman: "Departmanlar" 0.602, "Departmanları listele" 0.650
    ('rehber_departman', 'departman listesi'),
    ('rehber_departman', 'departmanları göster'),

    -- rehber_kisi: "Rehber" 0.646 (en yakini "merhaba" idi)
    ('rehber_kisi', 'çalışan rehberi'),
    ('rehber_kisi', 'kişi listesi')
) AS p(intent_name, phrase) ON i.name = p.intent_name
WHERE NOT EXISTS (
    SELECT 1 FROM intent_examples e WHERE e.intent_id = i.id AND e.phrase = p.phrase
);

-- Seed sessizce eksik uygulanmaz.
DO $$
DECLARE
    v_missing TEXT;
BEGIN
    SELECT string_agg(p.intent_name, ', ') INTO v_missing
    FROM (VALUES
        ('servis_guzergah'), ('yemek_menusu'), ('rehber_departman'), ('rehber_kisi')
    ) AS p(intent_name)
    WHERE NOT EXISTS (SELECT 1 FROM intents i WHERE i.name = p.intent_name);

    IF v_missing IS NOT NULL THEN
        RAISE EXCEPTION 'Intent bulunamadi: % — kisa form ornekleri eksik uygulandi', v_missing;
    END IF;
END $$;
