-- A-20 (#139): servis intent'leri icin EMIR KIPI ornekleri.
--
-- V32 emir kipini yalnizca calisma_duzeni'ne ekledi. IntentCalibrationIT'ye konan nobetci
-- ("servisleri listele") bunun bir yan etkisi OLMADIGINI gosterdi — sorgu calisma_duzeni'ne
-- kaymadi, dogru intent'te kaldi:
--
--   "servisleri listele" -> 0.679, en yakin cumle 'servis saatleri neler' (esik 0.68)
--
-- Yani kayma degil, servis intent'lerinde emir kipinin HIC temsil edilmemesi. Binde birlik
-- fark: tek bir emir kipli ornek yeterli. Esige dokunulmuyor (A-19 karari).
--
-- Ornekler servis_guzergah'a yazildi; "listele" cogul/genel bir istektir ve hat listesini
-- ima eder. Test her iki servis intent'ini de kabul ediyor (ikisi de mesru cevap).
--
-- YONTEM KURALI: test sorgusu ("servisleri listele") aynen eklenmez.

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    ('servis_guzergah', 'tüm servisleri göster'),
    ('servis_guzergah', 'servis hatlarını listele')
) AS p(intent_name, phrase) ON i.name = p.intent_name
WHERE NOT EXISTS (
    SELECT 1 FROM intent_examples e WHERE e.intent_id = i.id AND e.phrase = p.phrase
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM intents WHERE name = 'servis_guzergah') THEN
        RAISE EXCEPTION 'servis_guzergah intent bulunamadi — ornek eklenemedi';
    END IF;
END $$;
