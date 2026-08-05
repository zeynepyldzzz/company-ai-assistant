-- B-29 (#179): V42'deki 4 Izmir guzergahinin duraklari gercek bir surus sirasini
-- takip etmiyordu (or. Gaziemir hattinda Ucyol, Gaziemir ile Karabaglar arasinda
-- cografi olarak tutarsizdi). Bu migration her hat icin sade, sirali bir durak
-- listesi tanimliyor.
--
-- B-25 (#165) ile birlikte planlandigi uzere: her hattin son duragi artik sabit
-- sirket adresi (Egemenlik, Kemalpasa Cd. No:250 A, 35070 Bornova/Izmir).

DELETE FROM shuttle_stop
WHERE route_id IN (
    SELECT id FROM shuttle_route WHERE plate_number IN ('35 SR 301', '35 SR 302', '35 SR 303', '35 SR 304')
);

INSERT INTO shuttle_stop (route_id, name, time, order_index, latitude, longitude) VALUES
    -- Gaziemir - Karabaglar Hatti (35 SR 301)
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 301'),
        'Gaziemir Merkez', '07:00', 1, 38.3263, 27.1400),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 301'),
        'Optimum', '07:12', 2, 38.3379, 27.1348),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 301'),
        'Beyazevler', '07:22', 3, 38.3426, 27.1438),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 301'),
        'Eskiizmir', '07:35', 4, 38.3763, 27.1075),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 301'),
        'Karabaglar Merkez', '07:45', 5, 38.3865, 27.1347),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 301'),
        'Yasarbilgi (Sirket Merkezi)', '08:05', 6, 38.4306, 27.2231),

    -- Bornova - Karsiyaka Hatti (35 SR 302)
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 302'),
        'Karsiyaka Iskele', '07:05', 1, 38.4614, 27.1161),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 302'),
        'Bayrakli', '07:25', 2, 38.4622, 27.1614),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 302'),
        'Bornova Metro', '07:45', 3, 38.4691, 27.2170),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 302'),
        'Yasarbilgi (Sirket Merkezi)', '08:05', 4, 38.4306, 27.2231),

    -- Cesme Hatti (35 SR 303)
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 303'),
        'Cesme Merkez', '06:30', 1, 38.3226, 26.3033),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 303'),
        'Urla Merkez', '07:10', 2, 38.3235, 26.7657),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 303'),
        'Konak Iskele', '07:50', 3, 38.4192, 27.1287),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 303'),
        'Yasarbilgi (Sirket Merkezi)', '08:20', 4, 38.4306, 27.2231),

    -- Hatay Hatti (35 SR 304)
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 304'),
        'Goztepe', '07:00', 1, 38.3973, 27.0919),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 304'),
        'Hatay Mahallesi', '07:15', 2, 38.4015, 27.1028),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 304'),
        'Konak Iskele', '07:35', 3, 38.4192, 27.1287),
    ((SELECT id FROM shuttle_route WHERE plate_number = '35 SR 304'),
        'Yasarbilgi (Sirket Merkezi)', '08:00', 4, 38.4306, 27.2231);
