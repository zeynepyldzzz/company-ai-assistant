-- C-13 (#121): anket veri modeli + oylama akisi.
-- 1) survey.deadline: anketin son yanit tarihi (opsiyonel).
ALTER TABLE survey ADD COLUMN deadline TIMESTAMP NULL;

-- 2) survey_option: sabit coktan secmeli secenek listesi (min 2 secenek, admin tarafinda dogrulanir).
CREATE TABLE survey_option (
    id SERIAL PRIMARY KEY,
    survey_id INTEGER NOT NULL REFERENCES survey(id),
    option_text VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);

-- 3) survey_response: serbest "answers" JSONB yerine sabit secenege referans.
--    Eski kolon kaldirilir (henuz production'da veri yok, backlog asamasinda).
ALTER TABLE survey_response DROP COLUMN IF EXISTS answers;
ALTER TABLE survey_response ADD COLUMN option_id INTEGER REFERENCES survey_option(id);

-- 4) Bir calisan bir ankete sadece bir kez oy verebilir.
ALTER TABLE survey_response
    ADD CONSTRAINT uq_survey_response_survey_employee UNIQUE (survey_id, employee_id);
