-- #104 (A-11 takip): 'yemek_menusu' intent'ine kisa "<gun> yemek / <gun> menu"
-- varyantlari eklenir. Bu terse ifadeler eşik (0.68) altinda kalip intent_bulunamadi'ya
-- dusuyordu (or. "carsamba yemek" ~0.67). Embedding genelledigi icin gunlere yayilmis
-- birkac ornek tum aileyi yukari ceker.
-- Embedding NULL birakilir; IntentSeedRunner acilista Ollama (bge-m3) ile doldurur.

DO $$
DECLARE
    v_intent_id INTEGER;
BEGIN
    SELECT id INTO v_intent_id FROM intents WHERE name = 'yemek_menusu';
    IF v_intent_id IS NULL THEN
        RAISE EXCEPTION 'yemek_menusu intent bulunamadi — menu ornekleri eklenemedi';
    END IF;

    INSERT INTO intent_examples (intent_id, phrase) VALUES
        (v_intent_id, 'çarşamba yemek'),
        (v_intent_id, 'salı yemek'),
        (v_intent_id, 'cuma menü'),
        (v_intent_id, 'pazartesi menü'),
        (v_intent_id, 'çarşamba menü'),
        (v_intent_id, 'perşembe menü ne');
END $$;
