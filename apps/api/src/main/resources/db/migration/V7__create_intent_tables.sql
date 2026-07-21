-- V4__create_intent_tables.sql

CREATE TABLE intents (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE intent_examples (
    id         BIGSERIAL PRIMARY KEY,
    intent_id  BIGINT NOT NULL REFERENCES intents(id) ON DELETE CASCADE,
    phrase     VARCHAR(255) NOT NULL,
    embedding  vector(1024),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);