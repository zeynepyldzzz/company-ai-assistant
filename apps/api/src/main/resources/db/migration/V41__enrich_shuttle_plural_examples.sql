-- A-24 (#155): servis tarafina COGUL kisa form ornekleri.
--
-- V39 sonrasi kalibrasyon 55/57. Kalan iki vakada siralama ARTIK DOGRU (V39 oncesi
-- "merhaba"ya kaciyorlardi), yalnizca skor esigin altinda:
--   "Servisler" 0.640  en yakin: "servis listesi"
--   "Hatlar"    0.628  en yakin: "hangi hatlar var"
--
-- Ayni turdaki digerleri gecti ve aradaki fark ornegin BICIMINDE:
--   "Duraklar"    ~ "duraklar neler"   -> 0.686 GECTI   (cogul + iki kelime)
--   "Menüler"     ~ "menü listesi"     -> 0.830 GECTI
--   "Departmanlar"~ "departman listesi"-> 0.758 GECTI
--   "Servisler"   ~ "servis listesi"   -> 0.640 KACIRDI (tekil ornek)
--
-- "X listesi" kalibi menu ve departmanda yeterken serviste yetmiyor; muhtemelen "servis"
-- kelimesi cok daha fazla farkli baglamda gectigi icin vektoru daha dagimik. Duraklar'da
-- ise geriye donuk olarak COGUL ornek ("duraklar neler") ise yaradi. Bu migration ayni
-- kalibi servis ve hat icin de ekler.
--
-- YONTEM KURALI: test sorgulari ("Servisler", "Hatlar") aynen eklenmez.
--
-- NUMARA NOTU: bu dosya once V40 idi; ekip arkadasi acik bir PR'da V40'i rezerve ettigi
-- icin V41'e tasindi (V29'da yasanan "birden fazla migration" hatasini tekrarlamamak icin).

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    ('servis_guzergah', 'servisler neler'),
    ('servis_guzergah', 'hatlar neler'),
    ('servis_guzergah', 'hangi servisler var')
) AS p(intent_name, phrase) ON i.name = p.intent_name
WHERE NOT EXISTS (
    SELECT 1 FROM intent_examples e WHERE e.intent_id = i.id AND e.phrase = p.phrase
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM intents WHERE name = 'servis_guzergah') THEN
        RAISE EXCEPTION 'servis_guzergah intent bulunamadi — cogul form ornekleri eklenemedi';
    END IF;
END $$;
