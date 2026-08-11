-- A-40 (#209) ikinci tur: "acim" icin dogrudan ornek.
--
-- V52 "aciktim" ve "karnim ac" ekledi. Olculdu (IntentCalibrationIT, 2026-08-11):
--     "acim"  0.635 -> 0.653   en yakin: "karnim ac"   (esik 0.68)
--
-- Seed DOGRU yone calisti — en yakin komsu artik dogru intent'ten bir cumle — ama genelleme
-- esigi gecmeye yetmedi. V39'un birinci grubu bu: siralama dogru, yalnizca esik yetmiyor.
--
-- YONTEM KURALINDAN BILINCLI SAPMA. V30/V39 kurali "test sorgusunun KENDISI ornek olarak
-- eklenmez" diyor ve gerekcesi olcum butunlugu: seed edilen sorgu 1.000 ile gecer, hicbir sey
-- olculmus olmaz. Burada kurali cignemenin sebebi urun tarafi: "acim" bir yazim hatasi ya da
-- argo degil, duz Turkce ve log'da iki kez gecmis. Normal bir cumleyi cevapsiz birakmak icin
-- sebep yok.
--
-- Olcum butunlugu SU SEKILDE korundu: IntentCalibrationIT'de "acim" artik bir REGRESYON vakasi
-- (1.000 beklenir, hicbir sey olcmez) ve genellemeyi olcen yeni bir vaka eklendi —
-- "karnim acikti", seed EDILMEMIS bir varyant. Yani olcum devam ediyor, sadece baska cumleyle.
--
-- "Hatlar" ile farki: orada sorun kelimenin cok anlamliligiydi ("hat" = telefon hatti / cizgi /
-- hat sanati) ve eklenecek her ornek ayni duvara carpiyordu. Burada anlam net, komsu dogru,
-- eksik olan yalnizca 0.03.

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    ('yemek_menusu', 'açım')
) AS p(intent_name, phrase) ON i.name = p.intent_name
WHERE NOT EXISTS (
    SELECT 1 FROM intent_examples e WHERE e.intent_id = i.id AND e.phrase = p.phrase
);

-- Seed sessizce eksik uygulanmaz.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM intents WHERE name = 'yemek_menusu') THEN
        RAISE EXCEPTION 'Intent bulunamadi: yemek_menusu — aclik ornegi eksik uygulandi';
    END IF;
END $$;
