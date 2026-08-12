-- A-39 (#212): iki yeni intent — mevcut sayimi soran sorular ve sifre degistirme yonlendirmesi.
--
-- 1) sayim
--    Bu sorularin karsiligi yoktu ve sessizce baska bir soruya cevap veriliyordu. A-38 hatanin
--    BICIMINI degistirdi ama kaldirmadi: "kac calisan" kalibi ucuncu sahis desenine eklenince
--    "toplam kaç çalışan var" sorusu kullanicinin kendi planindan OFISTEKILERIN LISTESINE
--    dondu. Ikisi de yanlis; yenisi dolu ve formatli oldugu icin fark edilmesi daha zor.
--
--    ORNEKLERDE DURUM KELIMESI YOK. "kaç kişi ofiste" bir DURUM sorusudur, calisma_duzeni'ne
--    aittir ve bugun DOGRU calisiyor — iki intent anlamca komsu oldugu icin ornekler bilerek
--    durum kelimelerinden arindirildi. IntentCalibrationIT'ye nobetci eklendi.
--
-- 2) sifre_degistirme
--    A-29 ile sifre degistirme geldi ama chatbot bilmiyordu. Dinamik veri gerekmiyor: statik
--    sablon + buton. action_target SEMANTIK bir anahtar, URL degil (A-22) — istemcideki
--    chat-actions.ts haritasina 'change_password' -> '/change-password' eklendi. Harita
--    guncellenmeseydi buton sessizce hic gorunmezdi.
--
-- YONTEM KURALI (V30/V39): test sorgusunun KENDISI ornek olarak eklenmez. Asagidaki cumleler
-- kalibrasyon vakalarinin kendisi degil, ayni kalibin farkli yazimlari.

-- ---------------------------------------------------------------------------
-- 1) Intent'ler
-- ---------------------------------------------------------------------------
INSERT INTO intents (name, description, is_virtual)
VALUES ('sayim',
        'Mevcut kadro ve departman SAYISI (A-39). Durum sayimi degil — "kac kisi ofiste" calisma_duzeni''ne aittir.',
        false);

INSERT INTO intents (name, description, is_virtual, action_target, action_label)
VALUES ('sifre_degistirme',
        'Sifre degistirme yonlendirmesi (A-29 ekranina goturur). Dinamik veri yok, statik sablon.',
        false,
        'change_password',
        'Şifremi değiştir');

-- ---------------------------------------------------------------------------
-- 2) Sablonlar
-- ---------------------------------------------------------------------------
INSERT INTO response_templates (intent_id, template)
SELECT i.id, '{{sayim_bilgisi}}'
FROM intents i
WHERE i.name = 'sayim';

INSERT INTO response_templates (intent_id, template)
SELECT i.id,
       'Şifreni Şifre Değiştir ekranından güncelleyebilirsin. Mevcut şifreni bir kez '
       || 'girmen, ardından yeni şifreni iki kez yazman gerekiyor.'
FROM intents i
WHERE i.name = 'sifre_degistirme';

-- ---------------------------------------------------------------------------
-- 3) Ornek cumleler — embedding NULL birakiliyor, IntentSeedRunner acilista dolduruyor.
-- ---------------------------------------------------------------------------
INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    -- sayim: durum kelimesi ICERMEZ (yukaridaki gerekce)
    ('sayim', 'şirkette kaç kişi çalışıyor'),
    ('sayim', 'personel sayısı nedir'),
    ('sayim', 'toplam çalışan sayısı'),
    ('sayim', 'departman sayısı kaç'),
    ('sayim', 'kaç birim var'),

    ('sifre_degistirme', 'parolamı değiştirmek istiyorum'),
    ('sifre_degistirme', 'şifre değiştirme nasıl yapılır'),
    ('sifre_degistirme', 'yeni şifre belirlemek istiyorum'),
    ('sifre_degistirme', 'parola güncelleme')
) AS p(intent_name, phrase) ON i.name = p.intent_name
WHERE NOT EXISTS (
    SELECT 1 FROM intent_examples e WHERE e.intent_id = i.id AND e.phrase = p.phrase
);

-- ---------------------------------------------------------------------------
-- 4) Seed sessizce eksik uygulanmaz.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_missing TEXT;
BEGIN
    SELECT string_agg(p.intent_name, ', ') INTO v_missing
    FROM (VALUES ('sayim'), ('sifre_degistirme')) AS p(intent_name)
    WHERE NOT EXISTS (
        SELECT 1 FROM intents i
        JOIN response_templates t ON t.intent_id = i.id
        WHERE i.name = p.intent_name);

    IF v_missing IS NOT NULL THEN
        RAISE EXCEPTION 'Intent ya da sablon eksik: % — A-39 seed''i yarim uygulandi', v_missing;
    END IF;
END $$;
