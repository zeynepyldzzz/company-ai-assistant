-- A-2: Template yanıt motoru şeması

-- 1) Sanal intent desteği: fallback gibi sınıflandırmaya girmeyen satırları işaretler
ALTER TABLE intents
    ADD COLUMN is_virtual BOOLEAN NOT NULL DEFAULT false;

-- 2) Yanıt şablonları (FR-77 admin paneline zemin)
CREATE TABLE response_templates (
    id          BIGSERIAL PRIMARY KEY,
    intent_id   BIGINT NOT NULL REFERENCES intents(id) ON DELETE CASCADE,
    template    TEXT   NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT true,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_response_templates_intent UNIQUE (intent_id)
);