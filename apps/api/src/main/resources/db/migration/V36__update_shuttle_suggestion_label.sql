-- A-22 (#141): servis vitrin sorusu "Servisim kaçta?" -> "Servis saatleri".
--
-- Gerekce (kullanici geri bildirimi, elle test): chip bir SORU cumlesi gibi degil, bir
-- BASLIK gibi okunmali. "Servisim kaçta?" digerlerinden daha konusma dili kaliyordu.
--
-- Gonderilen metin de degisiyor (label ile question ayni kolondan geliyor), ama bu vaka
-- risksiz: kural katmani "servis" + "saat" kelimelerini gorup dogrudan servis_saatleri'ne
-- yonlendiriyor (RuleBasedIntentMatcher.matchShuttle), embedding'e hic dusmuyor.

UPDATE intents SET suggested_question = 'Servis saatleri' WHERE name = 'servis_saatleri';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM intents
        WHERE name = 'servis_saatleri' AND suggested_question = 'Servis saatleri'
    ) THEN
        RAISE EXCEPTION 'servis_saatleri vitrin sorusu guncellenemedi';
    END IF;
END $$;
