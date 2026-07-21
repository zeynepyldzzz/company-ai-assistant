-- V9__enrich_intent_examples.sql
-- IT bulgularina gore ornek iyilestirme:
-- 1) "yarın ne yiyeceğiz" kalip-benzerligi yuzunden alakasiz "yarın ... olacak"
--    sorularini cekiyordu; yemek-spesifik ifadeyle degistirildi.
-- 2) Kisa gundelik selamlasmalar esik altinda kaliyordu; varyant eklendi.

DELETE FROM intent_examples WHERE phrase = 'yarın ne yiyeceğiz';

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    ('yemek_menusu', 'yarın menüde ne olacak'),
    ('selamlama', 'naber'),
    ('selamlama', 'selam naber iyi misin'),
    ('selamlama', 'iyi günler'),
    ('selamlama', 'görüşürüz iyi çalışmalar')
) AS p(intent_name, phrase) ON p.intent_name = i.name;