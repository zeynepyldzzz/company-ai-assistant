-- B-4: Rehber ekranlarini gercek veriyle test edebilmek icin ornek departman/calisan verisi

INSERT INTO department (name, responsibilities) VALUES
    ('Insan Kaynaklari', 'Ise alim, ozluk isleri, performans degerlendirme'),
    ('Bilgi Teknolojileri', 'Yazilim gelistirme, altyapi, teknik destek'),
    ('Muhasebe ve Finans', 'Butce, faturalama, odemeler'),
    ('Satis ve Pazarlama', 'Musteri iliskileri, kampanyalar, satis operasyonlari');

INSERT INTO employee (name, email, phone, office_status, department_id, is_active) VALUES
    ('Ayse Kaya', 'ayse.kaya@company.com', '1001', 'Ofiste',
        (SELECT id FROM department WHERE name = 'Insan Kaynaklari'), true),
    ('Mehmet Demir', 'mehmet.demir@company.com', '1002', 'Uzaktan',
        (SELECT id FROM department WHERE name = 'Insan Kaynaklari'), true),
    ('Elif Sahin', 'elif.sahin@company.com', '1101', 'Ofiste',
        (SELECT id FROM department WHERE name = 'Bilgi Teknolojileri'), true),
    ('Burak Yildiz', 'burak.yildiz@company.com', '1102', 'Izinde',
        (SELECT id FROM department WHERE name = 'Bilgi Teknolojileri'), true),
    ('Zeynep Aydin', 'zeynep.aydin@company.com', '1103', 'Uzaktan',
        (SELECT id FROM department WHERE name = 'Bilgi Teknolojileri'), true),
    ('Can Ozturk', 'can.ozturk@company.com', '1201', 'Ofiste',
        (SELECT id FROM department WHERE name = 'Muhasebe ve Finans'), true),
    ('Deniz Celik', 'deniz.celik@company.com', '1202', 'Izinde',
        (SELECT id FROM department WHERE name = 'Muhasebe ve Finans'), true),
    ('Gizem Arslan', 'gizem.arslan@company.com', '1301', 'Ofiste',
        (SELECT id FROM department WHERE name = 'Satis ve Pazarlama'), true),
    ('Emre Koc', 'emre.koc@company.com', '1302', 'Uzaktan',
        (SELECT id FROM department WHERE name = 'Satis ve Pazarlama'), true);

UPDATE department SET manager_id = (SELECT id FROM employee WHERE email = 'ayse.kaya@company.com')
    WHERE name = 'Insan Kaynaklari';
UPDATE department SET manager_id = (SELECT id FROM employee WHERE email = 'elif.sahin@company.com')
    WHERE name = 'Bilgi Teknolojileri';
UPDATE department SET manager_id = (SELECT id FROM employee WHERE email = 'can.ozturk@company.com')
    WHERE name = 'Muhasebe ve Finans';
UPDATE department SET manager_id = (SELECT id FROM employee WHERE email = 'gizem.arslan@company.com')
    WHERE name = 'Satis ve Pazarlama';
