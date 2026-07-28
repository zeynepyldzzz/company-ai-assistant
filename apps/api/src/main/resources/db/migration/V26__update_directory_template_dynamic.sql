-- A-15 (#117): 'rehber_kisi' template'i statik yonlendirmeden canli rehber yanitina gecirilir.
-- Degisken DirectoryVariableResolver tarafindan doldurulur:
--   {{kisi_bilgisi}} -> kisi karti, netlestirme sorusu veya bulunamadi metni
-- Baslik satiri bilerek template'te degil resolver'da (V22/V24/V25 ile ayni karar): kisi
-- bulunamadiginda sabit bir baslik basmak yaniltici olur.
-- Referans bulunamazsa sessizce gecmek yerine hata verilir (seed kurali).

DO $$
DECLARE
    v_intent_id INTEGER;
BEGIN
    SELECT id INTO v_intent_id FROM intents WHERE name = 'rehber_kisi';
    IF v_intent_id IS NULL THEN
        RAISE EXCEPTION 'rehber_kisi intent bulunamadi — template guncellenemedi';
    END IF;

    UPDATE response_templates
    SET template = '{{kisi_bilgisi}}'
    WHERE intent_id = v_intent_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'rehber_kisi icin response_templates satiri yok — seed tutarsiz';
    END IF;
END $$;
