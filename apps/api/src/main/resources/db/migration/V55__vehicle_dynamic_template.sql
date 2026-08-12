-- A-41 (#213): arac_rezervasyon sablonu dinamiklestiriliyor.
--
-- Sablon V12'den beri TAMAMEN STATIK:
--     "Araç rezervasyon talebini ilgili form üzerinden oluşturabilirsin. Detaylar İdari
--      İşler bölümünde."
--
-- Kullanici somut veri soruyor, yonlendirme aliyordu. vehicle/ altinda resolver yoktu; diger
-- dokuz modulun hepsinde vardi. Olculdu: "hangi araçlar müsait" 0.627 ile esigin altinda
-- kaliyordu — ama esigi gecseydi bile ayni yonlendirmeyi alacakti. Yani kalibrasyon sorunu
-- degil, YETENEK boslugu.
--
-- Eski metin KAYBOLMUYOR: VehicleVariableResolver alan ipucu tasimayan mesajlarda ("araç
-- konusunda yardım") ayni yonlendirmeyi doner. Statik metnin mesru kaldigi tek yer orasi.
--
-- Buton degismiyor: action_target = 'vehicles' V35'te zaten ayarlanmis.
--
-- YONTEM KURALI (V30/V39): olculen sorgularin KENDISI ornek olarak eklenmez. Asagidakiler
-- kalibrasyon vakalarinin ("hangi araçlar müsait", "rezervasyonum var mı", "kaç araç var")
-- kendisi degil, ayni kaliplarin farkli yazimlari.
--
-- CAPRAZ KIRLENME UYARISI: A-39'da (V54) 'sayim' intent'ine "toplam çalışan sayısı" ornegi
-- eklendi. Buraya "toplam araç sayısı" yazmak iki intent'i birbirine yaklastirirdi; onun
-- yerine "filodaki araç sayısı" secildi — "filo" ayirt edici. IntentCalibrationIT'ye iki
-- yonlu nobetci eklendi.

UPDATE response_templates
SET template = '{{arac_bilgisi}}',
    updated_at = now()
WHERE intent_id = (SELECT id FROM intents WHERE name = 'arac_rezervasyon');

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    -- musaitlik: "hangi araçlar müsait" 0.627
    ('arac_rezervasyon', 'boşta araç var mı'),
    ('arac_rezervasyon', 'uygun araç listesi'),

    -- kullanicinin kendi rezervasyonlari
    ('arac_rezervasyon', 'rezervasyonlarımı göster'),
    ('arac_rezervasyon', 'araç rezervasyonum ne zaman'),

    -- filo buyuklugu ('toplam araç sayısı' BILEREK yok, yukaridaki uyariya bakiniz)
    ('arac_rezervasyon', 'filodaki araç sayısı')
) AS p(intent_name, phrase) ON i.name = p.intent_name
WHERE NOT EXISTS (
    SELECT 1 FROM intent_examples e WHERE e.intent_id = i.id AND e.phrase = p.phrase
);

-- Seed sessizce eksik uygulanmaz: sablon degismediyse chatbot ham {{arac_bilgisi}} basar.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM response_templates t
        JOIN intents i ON i.id = t.intent_id
        WHERE i.name = 'arac_rezervasyon' AND t.template = '{{arac_bilgisi}}'
    ) THEN
        RAISE EXCEPTION 'arac_rezervasyon sablonu guncellenemedi — A-41 seed''i yarim uygulandi';
    END IF;
END $$;
