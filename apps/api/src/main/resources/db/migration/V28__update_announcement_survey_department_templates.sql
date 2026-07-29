-- A-18 (#127): 'duyurular', 'anket' ve 'rehber_departman' template'leri statik
-- yonlendirmeden canli veriye gecirilir. Degiskenler ilgili resolver'lar tarafindan
-- doldurulur:
--   {{duyurular}}          -> AnnouncementVariableResolver (sabitlenenler ustte)
--   {{aktif_anketler}}     -> SurveyVariableResolver (yalnizca published=true)
--   {{departman_bilgisi}}  -> DepartmentVariableResolver (yonetici iletisimi)
-- Baslik satiri bilerek template'te degil resolver'da (V22/V24/V25/V26 ile ayni karar):
-- veri yokken sabit baslik basmak yaniltici olur.
-- Referans bulunamazsa sessizce gecmek yerine hata verilir (seed kurali).

DO $$
DECLARE
    v_intent_id INTEGER;
    v_pair      RECORD;
BEGIN
    FOR v_pair IN
        SELECT * FROM (VALUES
            ('duyurular',        '{{duyurular}}'),
            ('anket',            '{{aktif_anketler}}'),
            ('rehber_departman', '{{departman_bilgisi}}')
        ) AS t(intent_name, template_text)
    LOOP
        SELECT id INTO v_intent_id FROM intents WHERE name = v_pair.intent_name;
        IF v_intent_id IS NULL THEN
            RAISE EXCEPTION '% intent bulunamadi — template guncellenemedi', v_pair.intent_name;
        END IF;

        UPDATE response_templates
        SET template = v_pair.template_text
        WHERE intent_id = v_intent_id;

        IF NOT FOUND THEN
            RAISE EXCEPTION '% icin response_templates satiri yok — seed tutarsiz', v_pair.intent_name;
        END IF;
    END LOOP;
END $$;
