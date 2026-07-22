-- A-5: hr_procedure/policy_document uc katmanli modele tasiniyor.
-- hr_procedure = prosedur kimligi, policy_document = dokuman kimligi,
-- policy_version = versiyonlanan icerik (FR-58, NFR-06).

-- 1) hr_procedure: icerik alanlari versiyona tasindi
ALTER TABLE hr_procedure
    DROP COLUMN content,
    DROP COLUMN version,
    DROP COLUMN effective_date,
    ALTER COLUMN category SET NOT NULL,
    ADD COLUMN intent_id                  bigint,
    ADD COLUMN responsible_department_id  integer,
    ADD COLUMN responsible_contact        varchar(255),
    ADD COLUMN created_at                 timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN updated_at                 timestamptz NOT NULL DEFAULT now();

ALTER TABLE hr_procedure
    ADD CONSTRAINT uq_hr_procedure_category UNIQUE (category),
    ADD CONSTRAINT uq_hr_procedure_intent   UNIQUE (intent_id),
    ADD CONSTRAINT fk_hr_procedure_intent
        FOREIGN KEY (intent_id) REFERENCES intents(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_hr_procedure_department
        FOREIGN KEY (responsible_department_id) REFERENCES department(id) ON DELETE SET NULL;

-- 2) policy_document: sadece kimlik. Dosya/yukleyen bilgisi versiyona tasindi,
--    cunku her yeni versiyon kendi dosyasiyla gelir (NFR-06).
ALTER TABLE policy_document
    DROP CONSTRAINT policy_document_procedure_id_fkey,
    DROP COLUMN embedding,
    DROP COLUMN file_path,
    DROP COLUMN uploaded_by,
    ALTER COLUMN procedure_id SET NOT NULL,
    ADD COLUMN title      varchar(255) NOT NULL,
    ADD COLUMN created_at timestamptz  NOT NULL DEFAULT now(),
    ADD COLUMN deleted_at timestamptz;   -- A-6 soft delete icin

ALTER TABLE policy_document
    ADD CONSTRAINT fk_policy_document_procedure
        FOREIGN KEY (procedure_id) REFERENCES hr_procedure(id) ON DELETE RESTRICT;

CREATE INDEX idx_policy_document_procedure ON policy_document (procedure_id);

-- 3) policy_version: versiyonlanan icerik
CREATE TABLE policy_version (
    id             SERIAL PRIMARY KEY,
    document_id    integer     NOT NULL REFERENCES policy_document(id) ON DELETE RESTRICT,
    version_no     integer     NOT NULL,
    content        text,
    steps          jsonb       NOT NULL DEFAULT '[]'::jsonb,
    file_path      varchar(500),
    effective_date date        NOT NULL,
    is_current     boolean     NOT NULL DEFAULT false,
    created_at     timestamptz NOT NULL DEFAULT now(),
    created_by     integer     REFERENCES employee(id),
    CONSTRAINT uq_policy_version_no    UNIQUE (document_id, version_no),
    CONSTRAINT ck_policy_version_no    CHECK (version_no > 0),
    CONSTRAINT ck_policy_version_steps CHECK (jsonb_typeof(steps) = 'array')
);

-- FR-58 / A-6: bir dokumanin en fazla tek current versiyonu olabilir
CREATE UNIQUE INDEX ux_policy_version_current
    ON policy_version (document_id)
    WHERE is_current;

CREATE INDEX idx_policy_version_document ON policy_version (document_id);