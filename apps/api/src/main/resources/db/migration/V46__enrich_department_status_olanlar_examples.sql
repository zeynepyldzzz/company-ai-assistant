-- A-25 (#169): departman + durum sorularinda "olanlar" kalibi.
--
-- V45 sonrasi kalibrasyon 62/63. Kalan tek vaka:
--   "Muhasebe izinde olanlar" 0.693 -> rehber_departman
--                             (en yakin: "muhasebe departmanında kimler çalışıyor")
--
-- Karsilastirma acik ediyor: "Muhasebede kimler uzaktan çalışıyor" 0.782 ile GECTI, cunku
-- V45'teki "pazarlamada uzaktan çalışanlar kimler" ornegi ona yakin. Ama "X olanlar" kalibi
-- yalnizca DEPARTMANSIZ halde temsil ediliyordu (V32'den "ofiste olanları göster"); departman
-- adiyla birlesince en yakin cumle yine departman listesi ornegi oluyor.
--
-- Not: kullanici bu vakada zaten DOGRU cevap aliyordu — DepartmentVariableResolver'daki
-- savunma katmani durum filtresini uyguluyor. Bu migration siniflandirmayi da dogru yere
-- tasiyor, boylece chat_message_log analizi ve ileriki kalibrasyonlar yaniltici olmuyor.
--
-- YONTEM KURALI: test sorgusu ("Muhasebe izinde olanlar") aynen eklenmez.

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    ('calisma_duzeni', 'pazarlamada izinde olanlar'),
    ('calisma_duzeni', 'satış ekibinde ofiste olanlar')
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
    WHERE i.name = 'calisma_duzeni'
      AND e.phrase IN ('pazarlamada izinde olanlar', 'satış ekibinde ofiste olanlar');
    IF v_examples <> 2 THEN
        RAISE EXCEPTION 'Beklenen 2 "olanlar" kalibi ornegi, bulunan %', v_examples;
    END IF;
END $$;
