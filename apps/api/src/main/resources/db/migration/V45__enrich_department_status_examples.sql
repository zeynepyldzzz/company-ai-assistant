-- A-25 (#169) duzeltmesi: departman + DURUM sorulari calisma_duzeni'nde kalmali.
--
-- V44'te rehber_departman'a eklenen "departman calisan listesi" ornekleri, durum filtreli
-- sorulari da kendine cekti. Olculdu (V44 sonrasi):
--   "Muhasebede kimler uzaktan çalışıyor" 0.742 -> rehber_departman  (en yakin:
--                                                   "muhasebe departmanında kimler çalışıyor")
--   "Muhasebe izinde olanlar"             0.693 -> rehber_departman  (ayni cumle)
--
-- Ikisi de calisma_duzeni'ne ait: departman kapsamli DURUM sorusu A-14'ten beri orada
-- (OfficeStatusVariableResolver departman + durum filtresini destekliyor). Kural katmaninda
-- bu ayrim yapiliyor — durum kelimesi varsa departman listesi kurali devreye girmiyor — ama
-- embedding tarafinda ayni koruma yoktu.
--
-- Ayrica DepartmentVariableResolver'a savunma katmani eklendi: soru yanlis kategoriye
-- gelse bile durum filtresi uygulanip DOGRU cevap donuyor. Iki katman birlikte, cunku tek
-- basina ornek eklemek embedding yarisina bagli ve kirilgan.
--
-- YONTEM KURALI: test sorgulari aynen eklenmez.

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    ('calisma_duzeni', 'muhasebede kimler izinde'),
    ('calisma_duzeni', 'satış ekibinde kimler uzaktan'),
    ('calisma_duzeni', 'bilgi teknolojilerinde kimler ofiste'),
    ('calisma_duzeni', 'pazarlamada uzaktan çalışanlar kimler')
) AS p(intent_name, phrase) ON i.name = p.intent_name
WHERE NOT EXISTS (
    SELECT 1 FROM intent_examples e WHERE e.intent_id = i.id AND e.phrase = p.phrase
);

DO $$
DECLARE
    v_examples INTEGER;
BEGIN
    SELECT count(*) INTO v_examples
    FROM intent_examples e JOIN intents i ON i.id = e.intent_id
    WHERE i.name = 'calisma_duzeni' AND e.phrase IN (
        'muhasebede kimler izinde',
        'satış ekibinde kimler uzaktan',
        'bilgi teknolojilerinde kimler ofiste',
        'pazarlamada uzaktan çalışanlar kimler');
    IF v_examples <> 4 THEN
        RAISE EXCEPTION 'Beklenen 4 departman+durum ornegi, bulunan %', v_examples;
    END IF;
END $$;
