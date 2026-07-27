-- B-8/B-10: Arac rezervasyon ekranini gercek veriyle test edebilmek icin
-- ornek arac verisi (biri MAINTENANCE, digerleri AVAILABLE - FR-38/41 filtresi icin).

INSERT INTO vehicle (plate, model, maintenance_status) VALUES
    ('34 AB 1234', 'TOYOTA COROLLA', 'AVAILABLE'),
    ('34 CD 5678', 'RENAULT MEGANE', 'AVAILABLE'),
    ('34 EF 9012', 'VOLKSWAGEN TRANSPORTER', 'AVAILABLE'),
    ('34 GH 3456', 'FORD TRANSIT', 'MAINTENANCE');
