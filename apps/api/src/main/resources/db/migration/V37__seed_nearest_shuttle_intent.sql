-- A-21 (#146): 'servis_en_yakin' intent'i — en yakin servis YONLENDIRMESI.
--
-- Chatbot bu soruyu CEVAPLAMIYOR, cevabin iyi verilebildigi ekrana goturuyor. Gerekce:
--   1. Cevap KONUM gerektiriyor; ChatMessageRequest yalnizca metin tasiyor, tarayicinin
--      aktif konumu chatbot'a hic gelmiyor.
--   2. Kullanici semtini yazsa bile chatbot'un yapabilecegi tek sey GeocodingService
--      uzerinden dis servise (Nominatim) gitmek olurdu: saniyede 1 istek siniri, yanit
--      suresine eklenen gecikme, yeni hata yuzeyi.
--   3. Servisler ekraninda AKTIF KONUM destegi var — yani chatbot'un veremeyecegi bir sey
--      orada mevcut.
--
-- Bu, "chatbot statik yonlendirme yapmasin" ilkesine aykiri degil: orada mesele ELIMIZDEKI
-- veriyi vermek yerine ekran adresi soylemekti. Burada veri bizde degil, kullanicinin
-- konumunda.
--
-- Ornekler bilerek KISA formlar iceriyor ("en yakın durak", "yakınımdaki servis"):
-- A-19/A-22'de olculdu, kisa sorguyu esik ustune cikaran sey kisa ornektir.
--
-- CAPRAZ KIRLENME UYARISI: bu ornekler "servis" kelimesini servis_saatleri ve
-- servis_guzergah ile paylasiyor. IntentCalibrationIT'ye her ucu icin nobetci eklendi.

INSERT INTO intents (name, description, action_target, action_label)
VALUES (
    'servis_en_yakin',
    'En yakin servis duragi onerisi — konum gerektirdigi icin Servisler ekranina yonlendirir (template)',
    'shuttle_recommendation',
    'En yakın servisi bul'
);

INSERT INTO response_templates (intent_id, template)
SELECT i.id,
       'Sana en yakın servisi konumuna göre bulabiliyoruz. Servisler ekranından semtini ya da '
       || 'adresini yazıp "En Yakını Bul" diyebilirsin; en yakın durağı, hangi hatta bağlı olduğunu '
       || 've tahmini mesafeyi gösterir. Konum izni verirsen adres yazmana da gerek kalmaz.'
FROM intents i
WHERE i.name = 'servis_en_yakin';

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    ('servis_en_yakin', 'bana en yakın servis hangisi'),
    ('servis_en_yakin', 'en yakın durak nerede'),
    ('servis_en_yakin', 'evime en yakın servis'),
    ('servis_en_yakin', 'yakınımdaki servis durağı'),
    ('servis_en_yakin', 'en yakın servisi bul'),
    ('servis_en_yakin', 'hangi durak bana daha yakın')
) AS p(intent_name, phrase) ON i.name = p.intent_name
WHERE NOT EXISTS (
    SELECT 1 FROM intent_examples e WHERE e.intent_id = i.id AND e.phrase = p.phrase
);

-- Seed sessizce yarim uygulanmaz.
DO $$
DECLARE
    v_examples INTEGER;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM intents WHERE name = 'servis_en_yakin') THEN
        RAISE EXCEPTION 'servis_en_yakin intent olusturulamadi';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM response_templates rt
        JOIN intents i ON i.id = rt.intent_id
        WHERE i.name = 'servis_en_yakin'
    ) THEN
        RAISE EXCEPTION 'servis_en_yakin icin template eklenemedi';
    END IF;

    SELECT count(*) INTO v_examples
    FROM intent_examples e JOIN intents i ON i.id = e.intent_id
    WHERE i.name = 'servis_en_yakin';

    IF v_examples < 6 THEN
        RAISE EXCEPTION 'servis_en_yakin icin beklenen 6 ornek, bulunan %', v_examples;
    END IF;
END $$;
