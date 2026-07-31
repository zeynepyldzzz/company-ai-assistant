-- A-20 (#139): 'calisma_duzeni' icin EMIR KIPI ve "sirket" kapsamli ornekler.
--
-- Olculdu (elle test, 2026-07-31 — esik 0.68):
--   "öfiste kimler var"          -> 0.743 GECTI   (soru kalibi, ornekte var)
--   "öfiste olanları listele"    -> 0.673 KACIRDI (en yakin cumle DOGRU)
--   "öfiste olanlar söyle"       -> 0.616 KACIRDI
--   "şirkette olanlari listele"  -> 0.643 KACIRDI ve en yakin cumle YANLIS intent'te
--                                   ("bir çalışanın telefon numarasını bulmak istiyorum")
--
-- Iki ayri bosluk gorunuyor:
--   1. EMIR KIPI hic temsil edilmiyor. Mevcut orneklerin tamami soru kalibinda
--      ("kimler ofiste", "ofiste kimler var"); "listele"/"göster" fiilleri farkli bir
--      bolgeye dusuyor ve rehber cumlelerindeki "bulmak istiyorum" ile yarisiyor.
--   2. "sirket" kelimesi HICBIR calisma_duzeni orneginde gecmiyor. Sirket geneli kapsam
--      A-20'de koda eklendi ama sinifllandirma tarafinda karsiligi yoktu.
--
-- Yazim hatasi notu: "öfiste kimler var" 0.743 ile GECIYOR. Yani hatanin kendisi oldurucu
-- degil; kalip iyi temsil edildiginde model tolere ediyor. Ornek zenginlestirmesi yazim
-- hatasi toleransini da dolayli olarak artirir.
--
-- YONTEM KURALI (V30'dan): test setindeki sorgular buraya AYNEN eklenmez, yoksa test
-- 1.000 benzerlikle gecer ve hicbir sey olcmus oluruz. Ayni KALIP temsil edilir:
--   test "şirkette olanları listele" -> ornek "ofistekileri listele" / "şirkette kimler var"
--
-- CAPRAZ KIRLENME UYARISI: emir kipi TUM intent'lerde ortaktir ("menüyü listele",
-- "servisleri listele"). Bu yuzden orneklerde emir fiili TEK BASINA birakilmadi, her zaman
-- ofis/uzaktan/sirket alan kelimesiyle birlikte yazildi. IntentCalibrationIT'ye ayrica
-- "bu haftanın menüsünü listele" ve "servisleri listele" nobetcileri eklendi.

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    -- emir kipi + alan kelimesi
    ('calisma_duzeni', 'ofistekileri listele'),
    ('calisma_duzeni', 'uzaktan çalışanları listele'),
    ('calisma_duzeni', 'ofiste olanları göster'),
    ('calisma_duzeni', 'kimler ofiste söyler misin'),
    -- "sirket" kapsami
    ('calisma_duzeni', 'şirkette kimler var'),
    ('calisma_duzeni', 'şirket genelinde kimler ofiste')
) AS p(intent_name, phrase) ON i.name = p.intent_name
WHERE NOT EXISTS (
    SELECT 1 FROM intent_examples e WHERE e.intent_id = i.id AND e.phrase = p.phrase
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM intents WHERE name = 'calisma_duzeni') THEN
        RAISE EXCEPTION 'calisma_duzeni intent bulunamadi — ornek eklenemedi';
    END IF;
END $$;
