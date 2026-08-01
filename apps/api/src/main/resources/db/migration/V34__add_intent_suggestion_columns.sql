-- A-22 (#141): karsilama vitrini ve yonlendirme butonlari icin intent metadatasi.
--
-- Neden intents tablosunda: welcome ucu bir KOPRUDUR, icerik deposu degil. Vitrin sorusu
-- ve buton bilgisi intent'in kendi satirinda durursa yeni bir intent eklendiginde her sey
-- ayni migration'da tanimlanir; elle senkronlanacak ikinci bir liste (kod ici sabit dizi
-- ya da ayri tablo) olusmaz.
--
-- Neden intent_examples KULLANILAMAZ: oradaki cumleler embedding icin yazildi ve
-- kullaniciya gosterilmeye uygun degil ("34 SR 101", "Mehmet Demir'in dahilisi kac").
-- Vitrin sorusu ayri bir kavram: kisa, temsili ve tiklanabilir olmali.

ALTER TABLE intents
    ADD COLUMN suggested_question VARCHAR(255),
    ADD COLUMN suggestion_order   INTEGER,
    ADD COLUMN action_target      VARCHAR(64),
    ADD COLUMN action_label       VARCHAR(64);

COMMENT ON COLUMN intents.suggested_question IS
    'Karsilama mesajinda gosterilen ornek soru; NULL ise intent vitrine cikmaz';
COMMENT ON COLUMN intents.action_target IS
    'Semantik yonlendirme hedefi (or. directory_employees). Web URL DEGIL — Faz 2 mobil ayni yaniti kullanacak';

-- Buton ya tam ya hic: hedefsiz etiket tiklanamaz, etiketsiz hedef gosterilemez.
-- Uygulama tarafinda ikisini de kontrol etmek yerine kural DB'de zorlanir.
ALTER TABLE intents
    ADD CONSTRAINT ck_intents_action_pair
    CHECK ((action_target IS NULL) = (action_label IS NULL));

-- Sanal intent'ler siniflandirmaya aday degil (is_virtual = true, or. intent_bulunamadi).
-- Vitrinde gostermek anlamsiz olurdu: kullanici tiklar, cevap yine "anlamadim" olur.
ALTER TABLE intents
    ADD CONSTRAINT ck_intents_virtual_not_suggested
    CHECK (NOT (is_virtual AND suggested_question IS NOT NULL));

-- Vitrin sirasi belirsiz kalmamali: ayni sirayi iki intent paylasirsa chip'lerin dizilimi
-- sorgudan sorguya degisir. Kismi index, vitrine cikmayan satirlari serbest birakir.
CREATE UNIQUE INDEX uq_intents_suggestion_order
    ON intents (suggestion_order)
    WHERE suggested_question IS NOT NULL;

-- Vitrine cikan her satirin sirasi olmali; sirasiz satir listenin sonunda rastgele durur.
ALTER TABLE intents
    ADD CONSTRAINT ck_intents_suggestion_order
    CHECK ((suggested_question IS NULL) = (suggestion_order IS NULL));
