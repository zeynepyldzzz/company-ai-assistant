-- V13__create_chat_message_log.sql
-- A-4 (#21): chatbot mesajlarinin kalibrasyon/analiz logu.
-- Kullaniciya donuk sohbet gecmisi DEGIL; esik kalibrasyonu, eksik intent
-- tespiti ve Faz 2 RAG test verisi icin append-only kayit.
-- Ekip karari: kullanici kimligi (employee_id) TUTULMUYOR - analiz amaclarinin
-- hicbiri kimlik gerektirmiyor.

CREATE TABLE chat_message_log (
    id               BIGSERIAL     PRIMARY KEY,
    question         TEXT          NOT NULL,
    intent           VARCHAR(100)  NOT NULL,
    similarity       NUMERIC(6,5)  NOT NULL,
    matched_phrase   TEXT          NULL,
    matched          BOOLEAN       NOT NULL,
    threshold        NUMERIC(6,5)  NOT NULL,
    response_time_ms INTEGER       NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_message_log_intent_created_at
    ON chat_message_log (intent, created_at DESC);

CREATE INDEX idx_chat_message_log_created_at
    ON chat_message_log (created_at DESC);

COMMENT ON TABLE chat_message_log IS
    'Chatbot soru-cevap kalibrasyon logu (A-4). Kullaniciya donuk sohbet gecmisi degildir, kullanici kimligi tutulmaz.';
COMMENT ON COLUMN chat_message_log.threshold IS
    'Kayit anindaki benzerlik esigi; esik degistiginde eski satirlar yeniden degerlendirilebilsin diye tutulur.';
COMMENT ON COLUMN chat_message_log.matched_phrase IS
    'Vektor aramasinda en yakin ifade. matched=false olsa da doldurulur; esik altinda kalan sorularin neye benzedigini gormek kalibrasyonun asil verisidir.';