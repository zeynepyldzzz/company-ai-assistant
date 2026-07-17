-- ISSUE-003: Rol/izin seed verisi + test kullanıcıları

INSERT INTO role (name) VALUES
    ('employee'),
    ('hr_admin'),
    ('fleet_admin'),
    ('shuttle_admin'),
    ('canteen_admin'),
    ('system_admin');

INSERT INTO permission (permission_name) VALUES
    ('hr:manage'),
    ('fleet:manage'),
    ('shuttle:manage'),
    ('canteen:manage'),
    ('system:manage');

-- Her admin alt tipi kendi modül iznini alır; system_admin hepsini alır
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE (r.name = 'hr_admin'      AND p.permission_name = 'hr:manage')
   OR (r.name = 'fleet_admin'   AND p.permission_name = 'fleet:manage')
   OR (r.name = 'shuttle_admin' AND p.permission_name = 'shuttle:manage')
   OR (r.name = 'canteen_admin' AND p.permission_name = 'canteen:manage')
   OR (r.name = 'system_admin');

-- Test kullanıcıları (şifre: Passw0rd!)
INSERT INTO employee (name, email, password_hash, is_active, role_id)
VALUES
    ('Test Calisan', 'calisan@company.com',
     '$2a$10$iDX0MF4OkUQaF5bGYaQ1UOSA430MKKo4YJGyjViRaI7rhgvXnqSdO', true,
     (SELECT id FROM role WHERE name = 'employee')),
    ('Test Admin', 'admin@company.com',
     '$2a$10$iDX0MF4OkUQaF5bGYaQ1UOSA430MKKo4YJGyjViRaI7rhgvXnqSdO', true,
     (SELECT id FROM role WHERE name = 'system_admin'));