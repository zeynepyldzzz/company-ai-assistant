-- A-19 (#129): 'calisma_duzeni' icin KISA form ornekleri.
--
-- V29 sonrasi kalibrasyon 25/26. Kalan tek vaka "kimler ofiste" (0.504) — en yakin cumle
-- dogru intent'te ("kimler ofisten calisiyor") ama skor cok dusuk. Ayni kelimeleri paylasan
-- iki kisa ifade arasinda 0.504 cikmasi, kisa sorgu / uzun ornek asimetrisinin sertligini
-- gosteriyor.
--
-- Olculen desen: kisa sorguyu esik ustune cikaran sey KISA ornektir.
--   "anketler"  -> ornek "anket listesi"  (2 kelime) -> 0.724
--   "duyurular" -> ornek "duyuru var mi"  (3 kelime) -> 0.833
-- V29'da calisma_duzeni'ne eklenen ornekler uzundu; bu migration kisa formlari ekler.
--
-- Not: "kimler ofiste" test sorgusunun KENDISI eklenmez (yontem kurali) — farkli kelime
-- sirasi/ifade ile ayni kalip temsil edilir.

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    ('calisma_duzeni', 'ofiste kimler var'),
    ('calisma_duzeni', 'bugün ofiste olanlar kimler'),
    ('calisma_duzeni', 'ofisteki ekip kimler')
) AS p(intent_name, phrase) ON i.name = p.intent_name
WHERE NOT EXISTS (
    SELECT 1 FROM intent_examples e WHERE e.intent_id = i.id AND e.phrase = p.phrase
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM intents WHERE name = 'calisma_duzeni') THEN
        RAISE EXCEPTION 'calisma_duzeni intent bulunamadi — ornek eklenemedi';
    END IF;
END $$;
