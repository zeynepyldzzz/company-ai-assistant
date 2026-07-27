-- =====================================================================
-- V20__add_survey_published.sql
-- C-8 (#52): Admin anket olusturma + yayimlama.
-- C-7'de (#51) semada "aktif/pasif" ayrimi yapan bir kolon olmadigi icin
-- MVP geregi TUM anketler aktif sayiliyordu. Bu migration ile duzeltiliyor:
-- anketler artik taslak (published = false) olarak olusturulur, admin
-- PUT /admin/surveys/{id}/publish ile yayimladiginda GET /surveys/active'te
-- gorunur hale gelir.
-- (Not: V19 numarasi baska bir PR'da (vehicle seed) kullanildigi icin V20.)
-- =====================================================================

ALTER TABLE survey
    ADD COLUMN published BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_survey_published ON survey(published);
