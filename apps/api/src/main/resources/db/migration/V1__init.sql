-- =====================================================================
-- V1__init.sql
-- Company AI Assistant - ilk şema oluşturma migration'ı
-- ER diyagramındaki tüm tabloları oluşturur.
-- =====================================================================

-- ---------------------------------------------------------------------
-- ROLE & PERMISSION (RBAC)
-- ---------------------------------------------------------------------
CREATE TABLE role (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    permissions JSONB
);

CREATE TABLE permission (
    id              SERIAL PRIMARY KEY,
    permission_name VARCHAR(150) NOT NULL UNIQUE
);

-- ---------------------------------------------------------------------
-- DEPARTMENT & EMPLOYEE
-- ---------------------------------------------------------------------
CREATE TABLE department (
    id               SERIAL PRIMARY KEY,
    name             VARCHAR(150) NOT NULL,
    responsibilities TEXT,
    manager_id       INTEGER
    -- manager_id, employee tablosu oluşturulduktan sonra FK olarak bağlanacak (aşağıda ALTER ile).
);

CREATE TABLE employee (
    id               SERIAL PRIMARY KEY,
    name             VARCHAR(150) NOT NULL,
    email            VARCHAR(150) NOT NULL UNIQUE,
    phone            VARCHAR(30),
    office_status    VARCHAR(50),
    role_id          INTEGER REFERENCES role(id),
    department_id    INTEGER REFERENCES department(id),
    -- FR-64 tek kaynak kısıtı: haftalık çalışma düzeni ayrı tabloda değil,
    -- doğrudan employee kaydında jsonb olarak tutulur.
    weekly_schedule  JSONB
);

-- department.manager_id -> employee.id bağlantısını şimdi ekliyoruz
ALTER TABLE department
    ADD CONSTRAINT fk_department_manager
    FOREIGN KEY (manager_id) REFERENCES employee(id);

-- ---------------------------------------------------------------------
-- VEHICLE & RESERVATION
-- ---------------------------------------------------------------------
CREATE TABLE vehicle (
    id                  SERIAL PRIMARY KEY,
    plate               VARCHAR(20) NOT NULL UNIQUE,
    model               VARCHAR(100),
    maintenance_status  VARCHAR(50)
);

CREATE TABLE reservation (
    id           SERIAL PRIMARY KEY,
    vehicle_id   INTEGER NOT NULL REFERENCES vehicle(id),
    employee_id  INTEGER NOT NULL REFERENCES employee(id),
    start_time   TIMESTAMP NOT NULL,
    end_time     TIMESTAMP NOT NULL,
    status       VARCHAR(50) NOT NULL DEFAULT 'PENDING'
);

-- ---------------------------------------------------------------------
-- SHUTTLE ROUTE & STOP
-- ---------------------------------------------------------------------
CREATE TABLE shuttle_route (
    id            SERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    plate_number  VARCHAR(20)
);

CREATE TABLE shuttle_stop (
    id           SERIAL PRIMARY KEY,
    route_id     INTEGER NOT NULL REFERENCES shuttle_route(id) ON DELETE CASCADE,
    name         VARCHAR(150) NOT NULL,
    time         TIME,
    order_index  INTEGER NOT NULL
);

-- ---------------------------------------------------------------------
-- MEAL MENU & MEAL ITEM
-- ---------------------------------------------------------------------
CREATE TABLE meal_menu (
    id           SERIAL PRIMARY KEY,
    date         DATE NOT NULL,
    week_number  INTEGER
);

CREATE TABLE meal_item (
    id        SERIAL PRIMARY KEY,
    menu_id   INTEGER NOT NULL REFERENCES meal_menu(id) ON DELETE CASCADE,
    name      VARCHAR(150) NOT NULL,
    calories  INTEGER,
    allergens VARCHAR(255)
);

-- ---------------------------------------------------------------------
-- SURVEY / SURVEY_RESPONSE / FEEDBACK (anonim)
-- ---------------------------------------------------------------------
CREATE TABLE survey (
    id          SERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    created_by  INTEGER REFERENCES employee(id),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE survey_response (
    id           SERIAL PRIMARY KEY,
    survey_id    INTEGER NOT NULL REFERENCES survey(id) ON DELETE CASCADE,
    employee_id  INTEGER REFERENCES employee(id),  -- nullable: anonim yanıt verilebilir
    answers      JSONB
);

-- FR-43 anonimlik kısıtı: feedback tablosunda employeeId kolonu
-- BİLİNÇLİ OLARAK YOK. Anonimlik şema seviyesinde garanti edilir.
CREATE TABLE feedback (
    id          SERIAL PRIMARY KEY,
    survey_id   INTEGER REFERENCES survey(id) ON DELETE CASCADE,
    content     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- ANNOUNCEMENT & NOTIFICATION
-- ---------------------------------------------------------------------
CREATE TABLE announcement (
    id            SERIAL PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    content       TEXT,
    is_pinned     BOOLEAN NOT NULL DEFAULT false,
    published_by  INTEGER REFERENCES employee(id),
    published_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE notification (
    id               SERIAL PRIMARY KEY,
    employee_id      INTEGER NOT NULL REFERENCES employee(id),
    announcement_id  INTEGER REFERENCES announcement(id),  -- nullable
    type             VARCHAR(50),
    is_read          BOOLEAN NOT NULL DEFAULT false
);

-- ---------------------------------------------------------------------
-- CHAT / POLICY (RAG chatbot bilgi tabanı)
-- ---------------------------------------------------------------------
CREATE TABLE chat_history (
    id           SERIAL PRIMARY KEY,
    employee_id  INTEGER NOT NULL REFERENCES employee(id),
    question     TEXT,
    answer       TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE uploaded_document (
    id               SERIAL PRIMARY KEY,
    chat_history_id  INTEGER REFERENCES chat_history(id) ON DELETE CASCADE,
    file_name        VARCHAR(255),
    session_only     BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE hr_procedure (
    id              SERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    category        VARCHAR(100),
    content         TEXT,
    version         INTEGER NOT NULL DEFAULT 1,
    effective_date  DATE
);

CREATE TABLE policy_document (
    id           SERIAL PRIMARY KEY,
    procedure_id INTEGER REFERENCES hr_procedure(id) ON DELETE CASCADE,
    file_path    VARCHAR(500),
    embedding    vector(1024),  -- bge-m3 / multilingual-e5-large boyutuna göre ayarlanabilir
    uploaded_by  INTEGER REFERENCES employee(id)
);

-- ---------------------------------------------------------------------
-- İndeksler (performans için, arama sık yapılacak alanlarda)
-- ---------------------------------------------------------------------
CREATE INDEX idx_employee_department ON employee(department_id);
CREATE INDEX idx_employee_role ON employee(role_id);
CREATE INDEX idx_shuttle_stop_route ON shuttle_stop(route_id);
CREATE INDEX idx_meal_item_menu ON meal_item(menu_id);
CREATE INDEX idx_reservation_vehicle ON reservation(vehicle_id);
CREATE INDEX idx_reservation_employee ON reservation(employee_id);
CREATE INDEX idx_notification_employee ON notification(employee_id);
CREATE INDEX idx_chat_history_employee ON chat_history(employee_id);

-- pg_trgm ile isim/başlık aramalarını hızlandırmak için (bulanık arama)
CREATE INDEX idx_employee_name_trgm ON employee USING gin (name gin_trgm_ops);
CREATE INDEX idx_hr_procedure_title_trgm ON hr_procedure USING gin (title gin_trgm_ops);