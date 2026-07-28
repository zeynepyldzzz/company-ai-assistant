-- A-13 (FR-59..64): 'calisma_duzeni' template'i statik yonlendirmeden canli plan yanitina
-- gecirilir. Degisken ScheduleVariableResolver tarafindan doldurulur:
--   {{calisma_duzenim}} -> tek gun cumlesi veya haftanin gun gun listesi
-- Baslik satiri bilerek template'te degil resolver'da (V22/V24 ile ayni karar): kayit yokken
-- veya gelecek hafta sorulmusken basilan sabit baslik yaniltici olur.
-- Referans bulunamazsa sessizce gecmek yerine hata verilir (seed kurali).

DO $$
DECLARE
    v_intent_id INTEGER;
BEGIN
    SELECT id INTO v_intent_id FROM intents WHERE name = 'calisma_duzeni';
    IF v_intent_id IS NULL THEN
        RAISE EXCEPTION 'calisma_duzeni intent bulunamadi — template guncellenemedi';
    END IF;

    UPDATE response_templates
    SET template = '{{calisma_duzenim}}'
    WHERE intent_id = v_intent_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'calisma_duzeni icin response_templates satiri yok — seed tutarsiz';
    END IF;
END $$;
