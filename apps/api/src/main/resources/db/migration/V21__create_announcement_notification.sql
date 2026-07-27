-- C-9 (#53): Announcement/Notification endpoint'leri + sabitleme.
-- FR-45-47, 65-67.
-- NOT: "announcement" ve "notification" tablolari V1__init.sql'de zaten var
-- (is_pinned, published_by, published_at kolonlariyla). Burada sadece
-- FR-65-67 icin eksik olan bildirim tercihleri tablosu ekleniyor.

CREATE TABLE notification_preference (
    id                    SERIAL PRIMARY KEY,
    employee_id           INTEGER NOT NULL REFERENCES employee(id) UNIQUE,
    announcement_enabled  BOOLEAN NOT NULL DEFAULT true,
    schedule_enabled      BOOLEAN NOT NULL DEFAULT true,
    survey_enabled        BOOLEAN NOT NULL DEFAULT true,
    updated_at            TIMESTAMP NOT NULL DEFAULT now()
);
