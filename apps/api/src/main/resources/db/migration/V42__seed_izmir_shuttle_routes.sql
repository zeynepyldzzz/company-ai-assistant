-- B-22: sirket Izmir'de, ancak ornek servis guzergahi verisi (V18) Istanbul
-- lokasyonlarini kullaniyordu. Mevcut Istanbul verisi kaldirilmadan, demo
-- amacli 4 Izmir guzergahi eklendi (rota/durak detaylari gercek veri degil).

INSERT INTO shuttle_route (name, plate_number, driver_name, driver_phone) VALUES
    ('Gaziemir - Karabaglar Hatti', '35 SR 301', 'Hasan Kaya', '0535 444 55 66'),
    ('Bornova - Karsiyaka Hatti', '35 SR 302', 'Fatma Sahin', '0536 555 66 77'),
    ('Cesme Hatti', '35 SR 303', 'Ali Ozturk', '0537 666 77 88'),
    ('Hatay Hatti', '35 SR 304', 'Ayse Yildirim', '0538 777 88 99');

INSERT INTO shuttle_stop (route_id, name, time, order_index, latitude, longitude) VALUES
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 301'),
        'Gaziemir Metro', '07:00', 1, 38.2925, 27.1400),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 301'),
        'Ucyol', '07:20', 2, 38.4028, 27.1367),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 301'),
        'Karabaglar Merkez', '07:35', 3, 38.3900, 27.1300),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 302'),
        'Bornova Metro', '07:05', 1, 38.4691, 27.2170),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 302'),
        'Bayrakli', '07:25', 2, 38.4622, 27.1614),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 302'),
        'Karsiyaka Iskele', '07:45', 3, 38.4614, 27.1161),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 303'),
        'Konak Iskele', '06:45', 1, 38.4192, 27.1287),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 303'),
        'Urla Merkez', '07:20', 2, 38.3235, 26.7657),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 303'),
        'Cesme Merkez', '08:00', 3, 38.3226, 26.3033),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 304'),
        'Konak Iskele', '07:10', 1, 38.4192, 27.1287),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 304'),
        'Goztepe', '07:25', 2, 38.4066, 27.1219),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 304'),
        'Hatay Mahallesi', '07:40', 3, 38.3844, 27.1103);
