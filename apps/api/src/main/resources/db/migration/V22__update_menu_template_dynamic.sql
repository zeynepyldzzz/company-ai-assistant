-- A-11 (FR-08/09): 'yemek_menusu' template'i statik yonlendirmeden canli menu yanitina
-- gecirilir. Degiskenler MenuVariableResolver tarafindan doldurulur:
--   {{menu_gunu}}    -> baslik satiri ("Carsamba (29.07.2026) menusu:" / "Bu haftanin menusu:")
--   {{gunun_menusu}} -> madde madde yemek listesi
-- Referans bulunamazsa sessizce gecmek yerine hata verilir (seed kurali).

DO $$
DECLARE
    v_intent_id INTEGER;
BEGIN
    SELECT id INTO v_intent_id FROM intents WHERE name = 'yemek_menusu';
    IF v_intent_id IS NULL THEN
        RAISE EXCEPTION 'yemek_menusu intent bulunamadi — menu template guncellenemedi';
    END IF;

    UPDATE response_templates
    SET template = E'{{menu_gunu}}\n{{gunun_menusu}}'
    WHERE intent_id = v_intent_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'yemek_menusu icin response_templates satiri yok — seed tutarsiz';
    END IF;
END $$;
