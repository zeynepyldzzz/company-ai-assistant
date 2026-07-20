-- ISSUE-003: Auth altyapısı için şema ekleri
-- 1) RBAC düzeltmesi: izinler jsonb yerine join tablosundan okunur
--    (ERD revizyonu — role.permissions çift kaynak yaratıyordu)
CREATE TABLE role_permission (
    role_id       INTEGER NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

ALTER TABLE role DROP COLUMN permissions;

-- 2) Employee'ye kimlik doğrulama kolonları
--    password_hash nullable: İK yeni çalışan oluşturduğunda şifre henüz atanmamış olabilir;
--    login servisinde hash NULL ise giriş reddedilir.
ALTER TABLE employee
    ADD COLUMN password_hash VARCHAR(255),
    ADD COLUMN is_active     BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN totp_secret   VARCHAR(64),
    ADD COLUMN totp_enabled  BOOLEAN NOT NULL DEFAULT false;

-- 3) Refresh token'lar DB'de hash'li tutulur (ham token asla saklanmaz);
--    logout = revoked_at doldurmak, süresi geçen/revoke edilen token reddedilir.
CREATE TABLE refresh_token (
    id          SERIAL PRIMARY KEY,
    employee_id INTEGER NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    revoked_at  TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_token_employee ON refresh_token(employee_id);