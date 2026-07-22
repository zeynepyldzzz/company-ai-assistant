-- =====================================================================
-- V15__create_weekly_schedule.sql
-- C-4 (#29): Haftalik calisma duzeni — tek kaynak tablolar (FR-59..64)
-- =====================================================================

CREATE TABLE weekly_schedule (
    id               SERIAL PRIMARY KEY,
    employee_id      INTEGER NOT NULL REFERENCES employee(id),
    week_start_date  DATE NOT NULL,
    -- FR-64: ayni calisan + ayni hafta icin yalnizca TEK kayit olabilir
    CONSTRAINT uq_weekly_schedule_employee_week UNIQUE (employee_id, week_start_date)
);

CREATE TABLE schedule_day (
    id           SERIAL PRIMARY KEY,
    schedule_id  INTEGER NOT NULL REFERENCES weekly_schedule(id) ON DELETE CASCADE,
    day_of_week  VARCHAR(10) NOT NULL,   -- monday..friday
    status       VARCHAR(10) NOT NULL,   -- office / remote / leave
    -- Ayni hafta icinde ayni gun iki kez kaydedilemez
    CONSTRAINT uq_schedule_day_per_week UNIQUE (schedule_id, day_of_week)
);

CREATE INDEX idx_weekly_schedule_employee ON weekly_schedule(employee_id);