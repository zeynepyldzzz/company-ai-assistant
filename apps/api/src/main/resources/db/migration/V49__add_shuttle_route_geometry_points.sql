-- B-31: admin haritada tiklayarak guzergah cizebilsin (OSRM match ile yola
-- oturtulmus). shuttle_stop'un ayni sekli - guzergah id'sine gore siirali,
-- silinip-yeniden-yaratilarak guncellenir. Bos ise ShuttleService eski
-- OSRM-route-arasi-duraklar fallback'ine duser (geriye donuk uyumluluk).
CREATE TABLE shuttle_route_point (
    id           SERIAL PRIMARY KEY,
    route_id     INTEGER NOT NULL REFERENCES shuttle_route(id) ON DELETE CASCADE,
    order_index  INTEGER NOT NULL,
    latitude     DOUBLE PRECISION NOT NULL,
    longitude    DOUBLE PRECISION NOT NULL
);
