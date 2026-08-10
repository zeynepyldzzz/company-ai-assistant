-- A-37 (#203): acik tarihli sorgular icin ornekler.
--
-- Olculdu (chat_message_log): "17 ağustos yemek menüsü" 0.644, en yakin cumle "bu haftanın
-- yemek listesi". Yani siralama DOGRU ama skor esigin (0.68) altinda kaliyor ve soru
-- intent_bulunamadi'ya dusuyor. Mevcut orneklerin hicbirinde ACIK TARIH bicimi yok; hepsi
-- goreli ifade tasiyor ("bugun", "yarin", "bu hafta").
--
-- SIRA NOTU: bu migration, ayni issue'daki resolver duzeltmesinden SONRA anlamli. Once
-- eklenseydi soru menu intent'ine ulasir ve MenuVariableResolver taninmayan tarihi sessizce
-- bugune cevirirdi — kullanici 17 Agustos'u sorup 10 Agustos'un menusunu alirdi. Siniflandirma
-- iyilestirmesinin, arkasindaki resolver hazir olmadan yapilmasi yanlis cevap uretir.
--
-- YONTEM KURALI: test sorgusu ("17 ağustos yemek menüsü") aynen eklenmez; ayni BICIMDEN
-- farkli ornekler eklenir.
--
-- IKINCI TUR: ilk denemede yalnizca uc menu ornegi vardi ve olculdu:
--   "ağustos menüsü"     0.628 -> 0.683 GECTI
--   "17 ağustos menüsü"  0.632 -> 0.668 KACIRDI (esige 0.012 kaldi)
-- Ikisinde de en yakin cumle "5 eylül menüsü" oldu, yani siralama dogruydu. Aradaki fark
-- SAYI bileseni: ay adi tek basinayken ornekle ayni uzunlukta kaliyor, sayi eklenince
-- fazladan bir bilesen oluyor ve tek ornek bu kalibi temsil etmeye yetmiyor. Daha once
-- olculen "kisa sorguda somut kelimeler baskin" bulgusunun ayni tezahuru.
-- Bu yuzden ayni kalip FARKLI sayi ve aylarla cogaltildi; amac modelin tek bir aya degil
-- "sayi + ay + menu" yapisina yaslanmasi.

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    ('yemek_menusu', '5 eylül menüsü'),
    ('yemek_menusu', '12 mart yemekte ne var'),
    ('yemek_menusu', '03.09 menüsü'),
    ('yemek_menusu', '23 ekim menüsü'),
    ('yemek_menusu', '8 nisan yemek menüsü'),
    ('yemek_menusu', '30 kasım menüsü'),
    ('yemek_menusu', '17 temmuz yemekte ne var'),
    ('calisma_duzeni', '5 eylül ofiste miyim'),
    ('calisma_duzeni', '12 mart çalışma düzenim'),
    ('calisma_duzeni', '23 ekim ofiste miyim'),
    ('calisma_duzeni', '8 nisan çalışma düzenim')
) AS p(intent_name, phrase) ON i.name = p.intent_name
WHERE NOT EXISTS (
    SELECT 1 FROM intent_examples e WHERE e.intent_id = i.id AND e.phrase = p.phrase
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM intents WHERE name = 'yemek_menusu') THEN
        RAISE EXCEPTION 'yemek_menusu intent bulunamadi — acik tarih ornekleri eklenemedi';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM intents WHERE name = 'calisma_duzeni') THEN
        RAISE EXCEPTION 'calisma_duzeni intent bulunamadi — acik tarih ornekleri eklenemedi';
    END IF;
END $$;
