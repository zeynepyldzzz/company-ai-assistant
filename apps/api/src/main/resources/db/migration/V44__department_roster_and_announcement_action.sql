-- A-25 (#169): iki ayri is, ikisi de yalnizca seed.

-- ============================================================================
-- 1) Departman calisan listesi ornekleri
-- ============================================================================
-- Olculdu (chat_message_log, 2026-08-03, esik 0.68):
--   "Muhasebe çalışanları"            0.644  intent_bulunamadi (en yakin: "çalışan rehberi")
--   "Muhasebede kimler var"           0.590  intent_bulunamadi (en yakin: "şirkette kimler var")
--   "Bilgi teknolojileri çalışanları" 0.613  intent_bulunamadi (en yakin: "çalışan rehberi")
--   "Satış ekibinde kimler çalışıyor" 0.751  rehber_departman — ESLESIYOR ama YANLIS cevap
--                                            (yonetici bilgisi donuyordu, liste degil)
--
-- Kritik gozlem: en yakin cumleler IKI AYRI kategoriye dagiliyordu (rehber ve calisma
-- duzeni). Siniflandirici bile karar veremiyordu, cunku ortada boyle bir kalip yoktu —
-- bu, sorunun ornek eksikligi DEGIL yetenek eksikligi oldugunun gostergesi. Yetenek
-- DepartmentVariableResolver'a eklendi; bu ornekler siniflandirma tarafini tamamliyor.
--
-- YONTEM KURALI: test sorgulari aynen eklenmez, ayni kalip farkli cumleyle temsil edilir.

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    ('rehber_departman', 'muhasebe departmanında kimler çalışıyor'),
    ('rehber_departman', 'pazarlama ekibinde kimler var'),
    ('rehber_departman', 'departman çalışan listesi'),
    ('rehber_departman', 'insan kaynakları personeli kimler')
) AS p(intent_name, phrase) ON i.name = p.intent_name
WHERE NOT EXISTS (
    SELECT 1 FROM intent_examples e WHERE e.intent_id = i.id AND e.phrase = p.phrase
);

-- ============================================================================
-- 2) Duyuru yaniti icin yonlendirme butonu
-- ============================================================================
-- A-22'de bu buton BILEREK bos birakilmisti: calisanin gidebilecegi bir duyuru ekrani
-- yoktu, duyurular yalnizca /admin/announcements altindaydi ve o sayfa admin yetkisi
-- istiyordu. Butonun bir yere goturmesi gerekir; goturecek yer yoktu.
--
-- B-21 ile calisana acik /announcements ekrani geldi. Buton ilkesi (A-22) hala saglanıyor:
-- buton chatbot'un SINIRA DAYANDIGI yerde anlamlidir ve duyuru yaniti son birkac duyuruyu
-- gosteriyor, tamamini degil.
--
-- Sema degisikligi yok; kolonlar V34'te eklenmisti.

UPDATE intents
SET action_target = 'announcements', action_label = 'Tüm duyurular'
WHERE name = 'duyurular';

-- ============================================================================
-- Dogrulama
-- ============================================================================
DO $$
DECLARE
    v_examples INTEGER;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM intents WHERE name = 'rehber_departman') THEN
        RAISE EXCEPTION 'rehber_departman intent bulunamadi — ornekler eklenemedi';
    END IF;

    SELECT count(*) INTO v_examples
    FROM intent_examples e JOIN intents i ON i.id = e.intent_id
    WHERE i.name = 'rehber_departman' AND e.phrase IN (
        'muhasebe departmanında kimler çalışıyor',
        'pazarlama ekibinde kimler var',
        'departman çalışan listesi',
        'insan kaynakları personeli kimler');
    IF v_examples <> 4 THEN
        RAISE EXCEPTION 'Beklenen 4 departman listesi ornegi, bulunan %', v_examples;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM intents
        WHERE name = 'duyurular' AND action_target = 'announcements'
    ) THEN
        RAISE EXCEPTION 'duyurular intent''ine yonlendirme butonu yazilamadi';
    END IF;
END $$;
