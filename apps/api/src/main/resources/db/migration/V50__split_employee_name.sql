-- A-35 (#196): calisan adi ad/soyad olarak ayrilir.
--
-- Neden: tek kolon oldugu icin rehber ada gore siralaniyordu (klasigi soyada gore) ve
-- A-34'un "kelime basi" aramasi bosluk karakteriyle taklit edilmek zorunda kalmisti
-- (LIKE '% ' || :s || '%'). Veri modeli ayrildiginda o numara gereksizlesiyor.
--
-- name kolonu KALDIRILIYOR. Birakilsaydi iki kaynak olurdu ve senkron tutulmasi gerekirdi;
-- A-32'de tam olarak bu yuzden bir celiskiyle ugrastik (office_status kolonu "Ofiste"
-- derken plan "REMOTE" diyordu). Java tarafi Employee.getName() ile turetilmis deger
-- okumaya devam edecek.

ALTER TABLE employee
    ADD COLUMN first_name VARCHAR(100),
    ADD COLUMN last_name  VARCHAR(100);

-- Bolme kurali: SON kelime soyad, kalani ad.
-- Olculdu (16 kayit): 12'si iki kelimeli, 4'u tek kelimeli (hepsi test hesabi).
-- Uc veya daha fazla kelimeli isim YOK, dolayisiyla kural belirsizlige dusmuyor.
-- Tek kelimeli kayitlarda last_name NULL kalir; bos string yazmak "soyadi yok" ile
-- "soyadi bos" ayrimini silerdi.
UPDATE employee
SET first_name = CASE
        WHEN trim(name) LIKE '% %' THEN regexp_replace(trim(name), '\s+\S+$', '')
        ELSE trim(name)
    END,
    last_name = CASE
        WHEN trim(name) LIKE '% %' THEN regexp_replace(trim(name), '^.*\s+', '')
        ELSE NULL
    END;

-- Bolme sessizce basarisiz olmamali: bos bir first_name, o calisanin rehberde ve
-- chatbot yanitlarinda adsiz gorunmesi demek olurdu.
DO $$
DECLARE bolunemeyen INTEGER;
BEGIN
    SELECT COUNT(*) INTO bolunemeyen
    FROM employee
    WHERE first_name IS NULL OR trim(first_name) = '';

    IF bolunemeyen > 0 THEN
        RAISE EXCEPTION 'A-35: % calisanin adi bolunemedi, migration durduruldu', bolunemeyen;
    END IF;
END $$;

ALTER TABLE employee ALTER COLUMN first_name SET NOT NULL;

ALTER TABLE employee DROP COLUMN name;
