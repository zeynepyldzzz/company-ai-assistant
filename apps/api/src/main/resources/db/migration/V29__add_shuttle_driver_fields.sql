-- Servis guzergahlarinda sofor bilgisi tutulmuyordu (ne mock ne gercek veri).
-- Tek guzergah = tek sofor varsayimiyla ayri bir Driver tablosu yerine
-- plate_number gibi duz kolonlar eklendi.

ALTER TABLE shuttle_route
    ADD COLUMN driver_name VARCHAR(255),
    ADD COLUMN driver_phone VARCHAR(50);

UPDATE shuttle_route SET driver_name = 'Ahmet Yilmaz', driver_phone = '0532 111 22 33'
    WHERE plate_number = '34 SR 101';
UPDATE shuttle_route SET driver_name = 'Mehmet Demir', driver_phone = '0533 222 33 44'
    WHERE plate_number = '34 SR 202';
