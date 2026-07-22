-- V5, ekibin authenticator app'inde zaten kayitli olan secret yerine bilinen
-- bir demo secret kullanmisti. V5 uygulanmis oldugu icin dosyasini degistirmek
-- yerine (checksum kirilmasin diye) dogru degeri burada set ediyoruz.
UPDATE employee
SET totp_secret = 'NSCFKTRH3WVPM4QX6ZGELBYU75J2IODA'
WHERE email = 'admin@company.com';
