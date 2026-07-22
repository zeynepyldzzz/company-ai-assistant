-- B-6: FR-26/27 - en yakin durak onerisi icin durak koordinatlari gerekiyor.
ALTER TABLE shuttle_stop
    ADD COLUMN latitude DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION;
