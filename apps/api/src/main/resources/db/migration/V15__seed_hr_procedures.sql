-- A-5: IK prosedur seed verisi (4 topic: onboarding, izin, fazla-mesai, mazeret-izni)
-- Bagimlilik: V14 (policy tablolari uc katmanli modele tasindi)
-- NOT: Prosedur adimlari ornek icerektir; gercek IK metinleri A-6 ile yuklenecektir.

-- ---------------------------------------------------------------------------
-- 0) Guvenlik kontrolleri
-- ---------------------------------------------------------------------------

-- FR-57: her prosedur yaniti sorumlu departman icermek zorunda.
-- Departman bulunamazsa sessizce NULL yazmak yerine migration'i durduruyoruz.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM department WHERE name = 'Insan Kaynaklari') THEN
        RAISE EXCEPTION
            'Seed durduruldu: department tablosunda ''Insan Kaynaklari'' bulunamadi (FR-57).';
    END IF;
END $$;

-- V8/V12 seed'leri id''leri acik yazdiysa sequence geride kalmis olabilir.
-- Yeni INSERT''lerin PK cakismasi yasamamasi icin senkronluyoruz.
DO $$
DECLARE
    seq text;
    t   text;
BEGIN
    FOREACH t IN ARRAY ARRAY['intents', 'intent_examples', 'response_templates'] LOOP
        seq := pg_get_serial_sequence(t, 'id');
        IF seq IS NOT NULL THEN
            EXECUTE format(
                'SELECT setval(%L, COALESCE((SELECT MAX(id) FROM %I), 1), true)', seq, t);
        END IF;
    END LOOP;
END $$;

-- ---------------------------------------------------------------------------
-- 1) mazeret_izni intent'i (mevcut 3 prosedur intent'i V8'de seed'lendi)
-- ---------------------------------------------------------------------------

INSERT INTO intents (name, description, is_virtual, created_at)
VALUES ('mazeret_izni',
        'Mazeret izni kosullari, gerekli belgeler ve basvuru adimlari (FR-56)',
        false, now());

-- ---------------------------------------------------------------------------
-- 2) Ornek cumleler
--    embedding = NULL birakiliyor; IntentSeedRunner acilista dolduruyor.
-- ---------------------------------------------------------------------------

-- 2a) Cakisma duzeltmesi: bu cumle mazeret sorusudur ama izin_prosedur'e
--     bagliydi. Silmiyoruz, tasiyoruz -- metin degismedigi icin embedding'i
--     gecerli kalir, yeniden hesaplanmasi gerekmez.
UPDATE intent_examples
SET intent_id = (SELECT id FROM intents WHERE name = 'mazeret_izni')
WHERE phrase = 'mazeret izni için ne yapmam gerekiyor';

-- 2b) mazeret_izni icin yeni cumleler
INSERT INTO intent_examples (intent_id, phrase, embedding, created_at)
SELECT i.id, v.phrase, NULL, now()
FROM intents i
CROSS JOIN (VALUES
    ('mazeret izni hangi durumlarda verilir'),
    ('mazeret izni için hangi belgeleri sunmam gerekiyor'),
    ('evlilik izni nasıl alınır'),
    ('cenaze izni kaç gün')
) AS v(phrase)
WHERE i.name = 'mazeret_izni';

-- 2c) izin_prosedur, mazeret cumlesini kaybettigi icin yillik izin
--     tarafindan guclendiriliyor.
INSERT INTO intent_examples (intent_id, phrase, embedding, created_at)
SELECT i.id, v.phrase, NULL, now()
FROM intents i
CROSS JOIN (VALUES
    ('yıllık izin bakiyem ne kadar'),
    ('yıllık iznimi bölerek kullanabilir miyim')
) AS v(phrase)
WHERE i.name = 'izin_prosedur';

-- ---------------------------------------------------------------------------
-- 3) hr_procedure -- prosedur kimligi + intent eslemesi + sorumlu birim
-- ---------------------------------------------------------------------------

INSERT INTO hr_procedure
    (title, category, intent_id, responsible_department_id, responsible_contact)
SELECT v.title,
       v.category,
       (SELECT id FROM intents    WHERE name = v.intent_name),
       (SELECT id FROM department WHERE name = 'Insan Kaynaklari'),
       'ik@sirket.com'
FROM (VALUES
    ('İşe Giriş ve Oryantasyon Prosedürü', 'onboarding',   'ise_giris_oryantasyon'),
    ('Yıllık İzin Prosedürü',              'izin',         'izin_prosedur'),
    ('Fazla Mesai Prosedürü',              'fazla-mesai',  'fazla_mesai'),
    ('Mazeret İzni Prosedürü',             'mazeret-izni', 'mazeret_izni')
) AS v(title, category, intent_name);

-- Esleme tutmadiysa (intent adi degismis vb.) sessiz gecmeyelim.
DO $$
DECLARE
    bos integer;
BEGIN
    SELECT count(*) INTO bos FROM hr_procedure WHERE intent_id IS NULL;
    IF bos > 0 THEN
        RAISE EXCEPTION
            'Seed durduruldu: % prosedur icin intent eslemesi bulunamadi (FR-54).', bos;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 4) policy_document -- dokuman kimligi (versiyonlar arasi sabit)
-- ---------------------------------------------------------------------------

INSERT INTO policy_document (procedure_id, title)
SELECT p.id, p.title || ' Dokümanı'
FROM hr_procedure p;

-- ---------------------------------------------------------------------------
-- 5) policy_version -- versiyonlanan icerik (FR-58: is_current = true)
-- ---------------------------------------------------------------------------

INSERT INTO policy_version
    (document_id, version_no, content, steps, effective_date, is_current)
SELECT d.id, 1, v.content, v.steps::jsonb, DATE '2026-01-01', true
FROM policy_document d
JOIN hr_procedure p ON p.id = d.procedure_id
JOIN (VALUES
    ('onboarding',
     'İşe yeni başlayan çalışanların ilk gün ve ilk hafta boyunca tamamlaması gereken adımlar, oryantasyon eğitimi ve departman tanışma süreci.',
     '[
        {"order":1,"title":"Evrak teslimi","detail":"İlk gün İnsan Kaynakları ile işe giriş evraklarını tamamla."},
        {"order":2,"title":"Ekipman ve hesap","detail":"Bilgi Teknolojileri biriminden bilgisayar, kart ve kurumsal hesap teslimini al."},
        {"order":3,"title":"Departman tanışması","detail":"Departman yöneticinle görüşerek görev tanımını ve ilk hafta planını netleştir."},
        {"order":4,"title":"Oryantasyon eğitimi","detail":"Şirket kuralları, çalışma saatleri ve iç kaynak kullanımını kapsayan oryantasyon eğitimini tamamla."}
      ]'),
    ('izin',
     'Yıllık izin hakkının hesaplanması, talep oluşturma ve onay süreci.',
     '[
        {"order":1,"title":"Bakiye kontrolü","detail":"Portal üzerinden güncel yıllık izin bakiyeni kontrol et."},
        {"order":2,"title":"Talep oluşturma","detail":"İzin talebini portal üzerinden tarih aralığı belirterek oluştur."},
        {"order":3,"title":"Yönetici onayı","detail":"Talebin departman yöneticinin onayına düşer; onay öncesi izin kullanımı başlatılmaz."},
        {"order":4,"title":"Kayıt","detail":"Onay sonrası izin kaydı İnsan Kaynakları tarafından otomatik işlenir."}
      ]'),
    ('fazla-mesai',
     'Fazla mesai talep ve onay süreci ile ücret veya izin karşılığı kullanım kuralları.',
     '[
        {"order":1,"title":"Önceden bildirim","detail":"Fazla mesai ihtiyacını yöneticine mesai başlamadan önce bildir."},
        {"order":2,"title":"Onay","detail":"Yönetici onayı olmadan fazla mesai başlatılmaz ve hesaplamaya dahil edilmez."},
        {"order":3,"title":"Kayıt","detail":"Çalışılan fazla mesai saatlerini portal üzerinden aynı hafta içinde işle."},
        {"order":4,"title":"Karşılık tercihi","detail":"Ücret veya serbest zaman (izin) karşılığı tercihini kayıt sırasında belirt."}
      ]'),
    ('mazeret-izni',
     'Mazeret izni kapsamındaki durumlar, gerekli belgeler ve başvuru adımları.',
     '[
        {"order":1,"title":"Durum bildirimi","detail":"Mazeret durumunu mümkün olan en kısa sürede İnsan Kaynakları''na bildir."},
        {"order":2,"title":"Belge hazırlığı","detail":"Duruma göre gerekli belgeyi hazırla (sağlık raporu, evlilik cüzdanı, vefat belgesi vb.)."},
        {"order":3,"title":"Talep oluşturma","detail":"Portal üzerinden mazeret izni talebi oluştur ve belgeyi talebe ekle."},
        {"order":4,"title":"Onay ve kayıt","detail":"İnsan Kaynakları onayı sonrası izin kaydı işlenir ve bakiyene yansıtılır."}
      ]')
) AS v(category, content, steps) ON v.category = p.category;

-- ---------------------------------------------------------------------------
-- 6) response_templates -- statik metinler dinamik hale getiriliyor
--    Degiskenler HrProcedureVariableResolver tarafindan doldurulur.
--    ({{kullanici_adi}} ve {{departman}} ChatVariableResolver'dan gelir.)
-- ---------------------------------------------------------------------------

UPDATE response_templates rt
SET template = v.template,
    updated_at = now()
FROM intents i, (VALUES
    ('ise_giris_oryantasyon',
     E'{{prosedur_basligi}} kapsamında izlemen gereken adımlar:\n\n{{prosedur_adimlari}}\n\nSorumlu birim: {{sorumlu_departman}} ({{sorumlu_iletisim}}). Ayrıca {{departman}} yöneticinle de görüşebilirsin.\n\nKaynak: {{gecerlilik_tarihi}} tarihli v{{versiyon}} dokümanı.'),
    ('izin_prosedur',
     E'{{prosedur_basligi}} kapsamında izlemen gereken adımlar:\n\n{{prosedur_adimlari}}\n\nSorumlu birim: {{sorumlu_departman}} ({{sorumlu_iletisim}}).\n\nKaynak: {{gecerlilik_tarihi}} tarihli v{{versiyon}} dokümanı.'),
    ('fazla_mesai',
     E'{{prosedur_basligi}} kapsamında izlemen gereken adımlar:\n\n{{prosedur_adimlari}}\n\nSorumlu birim: {{sorumlu_departman}} ({{sorumlu_iletisim}}).\n\nKaynak: {{gecerlilik_tarihi}} tarihli v{{versiyon}} dokümanı.')
) AS v(intent_name, template)
WHERE i.name = v.intent_name
  AND rt.intent_id = i.id;

INSERT INTO response_templates (intent_id, template, enabled, updated_at)
SELECT i.id,
       E'{{prosedur_basligi}} kapsamında izlemen gereken adımlar:\n\n{{prosedur_adimlari}}\n\nSorumlu birim: {{sorumlu_departman}} ({{sorumlu_iletisim}}).\n\nKaynak: {{gecerlilik_tarihi}} tarihli v{{versiyon}} dokümanı.',
       true,
       now()
FROM intents i
WHERE i.name = 'mazeret_izni';