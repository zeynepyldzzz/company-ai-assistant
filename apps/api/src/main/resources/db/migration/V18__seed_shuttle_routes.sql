-- B-5/B-6: Servis guzergahi ekranlarini ve en yakin durak onerisini gercek
-- veriyle test edebilmek icin ornek guzergah/durak verisi.

INSERT INTO shuttle_route (name, plate_number) VALUES
    ('Anadolu Yakasi - Kadikoy Hatti', '34 SR 101'),
    ('Avrupa Yakasi - Besiktas Hatti', '34 SR 202');

INSERT INTO shuttle_stop (route_id, name, time, order_index, latitude, longitude) VALUES
    ((SELECT id FROM shuttle_route WHERE plate_number = '34 SR 101'),
        'Kadikoy Iskele', '07:00', 1, 40.9926, 29.0244),
    ((SELECT id FROM shuttle_route WHERE plate_number = '34 SR 101'),
        'Bostanci', '07:20', 2, 40.9614, 29.0928),
    ((SELECT id FROM shuttle_route WHERE plate_number = '34 SR 101'),
        'Atasehir', '07:40', 3, 40.9923, 29.1244),
    ((SELECT id FROM shuttle_route WHERE plate_number = '34 SR 202'),
        'Besiktas Iskele', '07:15', 1, 41.0422, 29.0061),
    ((SELECT id FROM shuttle_route WHERE plate_number = '34 SR 202'),
        'Mecidiyekoy', '07:35', 2, 41.0662, 28.9938),
    ((SELECT id FROM shuttle_route WHERE plate_number = '34 SR 202'),
        'Levent', '07:50', 3, 41.0815, 29.0090);
