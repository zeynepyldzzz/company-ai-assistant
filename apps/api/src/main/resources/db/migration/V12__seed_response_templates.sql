-- A-2: Fallback intent'i + yanıt şablonları seed'i

-- 1) Sanal fallback intent (sınıflandırmaya girmez, embedding'i olmaz)
INSERT INTO intents (name, is_virtual)
VALUES ('intent_bulunamadi', true);

-- 2) Şablonlar (13 intent + fallback)
INSERT INTO response_templates (intent_id, template)
SELECT i.id, t.template
FROM (VALUES
    ('selamlama',           'Merhaba {{kullanici_adi}}! Sana nasıl yardımcı olabilirim? Yemek menüsü, servis saatleri, izin prosedürleri ve daha fazlasını sorabilirsin.'),
    ('yemek_menusu',        'Günün ve haftanın menüsünü uygulamanın Yemek Menüsü bölümünden görebilirsin.'),
    ('servis_saatleri',     'Servis kalkış saatlerini Servis Bilgileri bölümünden görebilirsin.'),
    ('servis_guzergah',     'Servis güzergahlarını ve durakları Servis Bilgileri bölümünden inceleyebilirsin.'),
    ('izin_prosedur',       'İzin talep sürecine dair adımları İK Prosedürleri bölümünde bulabilirsin. Talebini portal üzerinden oluşturabilirsin.'),
    ('fazla_mesai',         'Fazla mesai kuralları ve onay süreci İK Prosedürleri bölümünde yer alıyor.'),
    ('ise_giris_oryantasyon','Oryantasyon sürecine dair bilgileri İK Prosedürleri bölümünde bulabilirsin. Soruların için {{departman}} yöneticinle de görüşebilirsin.'),
    ('calisma_duzeni',      'Çalışma düzeni ve mesai saatlerine dair kurallar İK Prosedürleri bölümünde açıklanıyor.'),
    ('rehber_kisi',         'Aradığın kişiye Şirket Rehberi bölümünden isimle arama yaparak ulaşabilirsin.'),
    ('rehber_departman',    'Departman iletişim bilgilerine Şirket Rehberi bölümünden ulaşabilirsin.'),
    ('duyurular',           'Güncel duyuruları uygulamanın Duyurular bölümünden takip edebilirsin.'),
    ('anket',               'Aktif anketleri Anketler bölümünde görebilir ve katılım sağlayabilirsin.'),
    ('arac_rezervasyon',    'Araç rezervasyon talebini ilgili form üzerinden oluşturabilirsin. Detaylar İdari İşler bölümünde.'),
    ('intent_bulunamadi',   'Üzgünüm {{kullanici_adi}}, sorunu tam anlayamadım. Farklı şekilde ifade etmeyi deneyebilir ya da İK ile iletişime geçebilirsin.')
) AS t(intent_name, template)
JOIN intents i ON i.name = t.intent_name;