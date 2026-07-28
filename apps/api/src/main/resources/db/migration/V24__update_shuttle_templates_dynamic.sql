-- A-12 (FR-10/11): 'servis_saatleri' ve 'servis_guzergah' template'leri statik
-- yonlendirmeden canli servis yanitina gecirilir. Degiskenler ShuttleVariableResolver
-- tarafindan doldurulur:
--   {{servis_saatleri}}  -> hat basligi + durak/saat listesi
--   {{servis_guzergahi}} -> hat + plaka + sirali durak listesi
-- Baslik satiri bilerek template'te degil resolver'da: veri yokken "Servis saatleri:"
-- basligi basip altini bos birakmak yaniltici olur (V22/#104 ile ayni karar).
-- Referans bulunamazsa sessizce gecmek yerine hata verilir (seed kurali).

DO $$
DECLARE
    v_intent_id INTEGER;
BEGIN
    SELECT id INTO v_intent_id FROM intents WHERE name = 'servis_saatleri';
    IF v_intent_id IS NULL THEN
        RAISE EXCEPTION 'servis_saatleri intent bulunamadi — servis template guncellenemedi';
    END IF;

    UPDATE response_templates
    SET template = '{{servis_saatleri}}'
    WHERE intent_id = v_intent_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'servis_saatleri icin response_templates satiri yok — seed tutarsiz';
    END IF;

    SELECT id INTO v_intent_id FROM intents WHERE name = 'servis_guzergah';
    IF v_intent_id IS NULL THEN
        RAISE EXCEPTION 'servis_guzergah intent bulunamadi — servis template guncellenemedi';
    END IF;

    UPDATE response_templates
    SET template = '{{servis_guzergahi}}'
    WHERE intent_id = v_intent_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'servis_guzergah icin response_templates satiri yok — seed tutarsiz';
    END IF;
END $$;
