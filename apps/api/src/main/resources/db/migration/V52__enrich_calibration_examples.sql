-- A-40 (#209): olculmus kalibrasyon boslugu — gayriresmi selamlama ve aclik ifadeleri.
--
-- Olculdu (chat_message_log, 2026-08-11, esik 0.68) — hepsi intent_bulunamadi:
--     "Napion"      0.596     "Napiyorsun"  0.571     "napiosun"  0.492
--     "hellan sana" 0.552     "Acim"        0.635
--
-- Ayni olcumde "Nasilsin" 0.865 ve "Naber" 1.000 ile ZATEN eslesiyor. Yani eksik olan hatir
-- sorma kavrami degil, KISALTILMIS/argo yazim. Aclik tarafinda da menu kavrami var ama
-- "acim/aciktim" gibi ihtiyac ifadeleri hic ornek olarak yazilmamis; mevcut ornekler
-- ("menu listesi", "bugunku yemekler") sorunun kendisini degil konusunu tarif ediyor.
--
-- YONTEM KURALI (V30/V39'dan): test sorgusunun KENDISI ornek olarak eklenmez, yoksa test
-- 1.000 ile gecer ve hicbir sey olculmus olmaz. Asagidaki cumleler olculen sorgularin
-- kendisi DEGIL, ayni kalibin farkli yazimlari. "acim" ve "napion" bilerek yok.
--
-- BEKLENEN SINIR: "napion" gibi agir kisaltmalarin "ne yapiyorsun"a yakinsamasi garanti
-- degil — bge-m3 cok dilli bir model ve bu bicimler egitim verisinde seyrek olabilir. Merge
-- sonrasi yeniden olculmeli; duzelmezse cozum ornek eklemek degil trigram/LLM (Faz 2).
--
-- "sa", "slm", "mrb" BURAYA EKLENMEDI: iki-uc harflik dizgede embedding'in tutunacagi
-- anlamsal sinyal yok (V39'un "uzunluk benzerligi anlami bastiriyor" bulgusu). Onlar kural
-- katmaninda TAM ESLESME ile cozuldu.

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    -- selamlama: "Napion" 0.596, "Napiyorsun" 0.571, "napiosun" 0.492, "hellan sana" 0.552
    ('selamlama', 'ne yapıyorsun'),
    ('selamlama', 'naptın'),
    ('selamlama', 'nasıl gidiyor'),
    ('selamlama', 'selam sana'),

    -- yemek_menusu: "Acim" 0.635. Ihtiyac ifadesi, konu tarifi degil.
    ('yemek_menusu', 'acıktım'),
    ('yemek_menusu', 'karnım aç'),
    ('yemek_menusu', 'çok acıktım ne yiyebilirim')
) AS p(intent_name, phrase) ON i.name = p.intent_name
WHERE NOT EXISTS (
    SELECT 1 FROM intent_examples e WHERE e.intent_id = i.id AND e.phrase = p.phrase
);

-- Seed sessizce eksik uygulanmaz.
DO $$
DECLARE
    v_missing TEXT;
BEGIN
    SELECT string_agg(p.intent_name, ', ') INTO v_missing
    FROM (VALUES
        ('selamlama'), ('yemek_menusu')
    ) AS p(intent_name)
    WHERE NOT EXISTS (SELECT 1 FROM intents i WHERE i.name = p.intent_name);

    IF v_missing IS NOT NULL THEN
        RAISE EXCEPTION 'Intent bulunamadi: % — kalibrasyon ornekleri eksik uygulandi', v_missing;
    END IF;
END $$;
