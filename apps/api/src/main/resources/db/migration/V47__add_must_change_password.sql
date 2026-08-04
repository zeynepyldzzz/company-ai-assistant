-- A-29 (#178): ilk giriste sifre degistirme zorunlulugu.
--
-- Bugun admin, calisan olusturulurken sifreyi KENDI belirliyor ve o sifre kalici oluyor.
-- Iki sonucu var: (1) admin calisanin kalici sifresini biliyor, yani sifre bir kimlik
-- dogrulama araci olmaktan cikiyor; (2) calisan sifresini degistiremiyor, cunku sistemde
-- sifre degistirme ucu HIC YOK.
--
-- Bu kolon akisin anahtari: sistem tarafindan uretilen gecici sifrelerde true olur ve
-- kullanici kendi sifresini belirleyene kadar true kalir.
--
-- DEFAULT false: mevcut kayitlar etkilenmez. Onlarin sifresi admin tarafindan elle
-- belirlenmis durumda ve bu migration onlari giristen kilitlememeli.

ALTER TABLE employee
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN employee.must_change_password IS
    'true ise kullanici ilk girisinde sifresini degistirmek zorunda (A-29)';
