-- B-4: Rehber ekranlarini gercek veriyle test edebilmek icin ornek departman/calisan verisi

INSERT INTO department (name, responsibilities) VALUES
    ('Insan Kaynaklari', 'Ise alim, ozluk isleri, performans degerlendirme'),
    ('Bilgi Teknolojileri', 'Yazilim gelistirme, altyapi, teknik destek'),
    ('Muhasebe ve Finans', 'Butce, faturalama, odemeler'),
    ('Satis ve Pazarlama', 'Musteri iliskileri, kampanyalar, satis operasyonlari');

INSERT INTO employee (name, email, phone, office_status, department_id, is_active) VALUES
    ('Ayse Kaya', 'ayse.kaya@company.com', '0505 050 50 50', 'Ofiste',
        (SELECT id FROM department WHERE name = 'Insan Kaynaklari'), true),
    ('Mehmet Demir', 'mehmet.demir@company.com', '0101 010 01 01', 'Uzaktan',
        (SELECT id FROM department WHERE name = 'Insan Kaynaklari'), true),
    ('Elif Sahin', 'elif.sahin@company.com', '0909 090 09 09', 'Ofiste',
        (SELECT id FROM department WHERE name = 'Bilgi Teknolojileri'), true),
    ('Burak Yildiz', 'burak.yildiz@company.com', '0707 070 70 70', 'Izinde',
        (SELECT id FROM department WHERE name = 'Bilgi Teknolojileri'), true),
    ('Zeynep Aydin', 'zeynep.aydin@company.com', '0404 040 04 04', 'Uzaktan',
        (SELECT id FROM department WHERE name = 'Bilgi Teknolojileri'), true),
    ('Can Ozturk', 'can.ozturk@company.com', '0303 030 30 30', 'Ofiste',
        (SELECT id FROM department WHERE name = 'Muhasebe ve Finans'), true),
    ('Deniz Celik', 'deniz.celik@company.com', '0606 060 60 60', 'Izinde',
        (SELECT id FROM department WHERE name = 'Muhasebe ve Finans'), true),
    ('Gizem Arslan', 'gizem.arslan@company.com', '0000 000 00 00', 'Ofiste',
        (SELECT id FROM department WHERE name = 'Satis ve Pazarlama'), true),
    ('Emre Koc', 'emre.koc@company.com', '0706 076 06 07', 'Uzaktan',
        (SELECT id FROM department WHERE name = 'Satis ve Pazarlama'), true);

UPDATE department SET manager_id = (SELECT id FROM employee WHERE email = 'ayse.kaya@company.com')
    WHERE name = 'Insan Kaynaklari';
UPDATE department SET manager_id = (SELECT id FROM employee WHERE email = 'elif.sahin@company.com')
    WHERE name = 'Bilgi Teknolojileri';
UPDATE department SET manager_id = (SELECT id FROM employee WHERE email = 'can.ozturk@company.com')
    WHERE name = 'Muhasebe ve Finans';
UPDATE department SET manager_id = (SELECT id FROM employee WHERE email = 'gizem.arslan@company.com')
    WHERE name = 'Satis ve Pazarlama';
