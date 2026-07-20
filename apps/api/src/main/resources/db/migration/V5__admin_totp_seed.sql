-- Admin girisi zorunlu 2FA istiyor (ISSUE-003) ama seed admin kullanicisinda
-- totp_secret hic set edilmemisti; bu yuzden 2FA dogrulama adimina hicbir zaman
-- ulasilamiyordu. Gelistirme/test icin bilinen bir TOTP secret atiyoruz.
UPDATE employee
SET totp_secret = 'JBSWY3DPEHPK3PXP',
    totp_enabled = true
WHERE email = 'admin@company.com';
