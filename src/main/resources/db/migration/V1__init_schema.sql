-- =============================================================================
-- V1__init_schema.sql
-- Migración inicial: crea el esquema completo de la base de datos.
--
-- Convención de nombres Flyway:
--   V{versión}__{descripción}.sql
--   Versión: número entero o decimal (1, 1.1, 2...)
--   Descripción: palabras separadas por guión bajo
--
-- IMPORTANTE: Este script NUNCA se modifica una vez ejecutado en cualquier
-- entorno. Para cambios futuros, crear un nuevo script V2__....sql.
-- Flyway detecta modificaciones en scripts ya ejecutados y lanza error.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- TABLA: users
-- Cuentas de acceso al sistema. Separada del modelo de negocio.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY,
    username
    VARCHAR
(
    100
) NOT NULL,
    password VARCHAR
(
    255
) NOT NULL, -- Hash BCrypt, nunca texto plano
    role VARCHAR
(
    20
) NOT NULL, -- ADMIN | MECHANIC | CLIENT
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    client_id BIGINT, -- FK opcional a clients
    mechanic_id BIGINT, -- FK opcional a mechanics
-- Campos de auditoría (de AuditableEntity)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP, -- NULL = activo, valor = borrado lógico

    CONSTRAINT pk_users PRIMARY KEY
(
    id
),
    CONSTRAINT uq_users_username UNIQUE
(
    username
),
    CONSTRAINT uq_users_client_id UNIQUE
(
    client_id
),
    CONSTRAINT uq_users_mechanic_id UNIQUE
(
    mechanic_id
),
    CONSTRAINT chk_users_role CHECK
(
    role
    IN
(
    'ADMIN',
    'MECHANIC',
    'CLIENT'
))
    );

-- -----------------------------------------------------------------------------
-- TABLA: clients
-- Clientes del taller. Hereda campos de Person.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clients
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY,
    client_code
    INT
    NOT
    NULL,
    name
    VARCHAR
(
    100
) NOT NULL,
    surname1 VARCHAR
(
    100
) NOT NULL,
    surname2 VARCHAR
(
    100
),
    nif VARCHAR
(
    20
) NOT NULL,
    email VARCHAR
(
    150
) NOT NULL,
    telephone VARCHAR
(
    20
),
    -- Campos de auditoría
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT pk_clients PRIMARY KEY
(
    id
),
    CONSTRAINT uq_clients_client_code UNIQUE
(
    client_code
),
    CONSTRAINT uq_clients_nif UNIQUE
(
    nif
)
    );

-- -----------------------------------------------------------------------------
-- TABLA: mechanics
-- Mecánicos del taller. Hereda campos de Person.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mechanics
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY,
    name
    VARCHAR
(
    100
) NOT NULL,
    surname1 VARCHAR
(
    100
) NOT NULL,
    surname2 VARCHAR
(
    100
),
    nif VARCHAR
(
    20
) NOT NULL,
    email VARCHAR
(
    150
) NOT NULL,
    telephone VARCHAR
(
    20
),
    registration_date DATE NOT NULL,
    specialty VARCHAR
(
    100
) NOT NULL,
    -- Campos de auditoría
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT pk_mechanics PRIMARY KEY
(
    id
),
    CONSTRAINT uq_mechanics_nif UNIQUE
(
    nif
)
    );

-- -----------------------------------------------------------------------------
-- TABLA: vehicles
-- Vehículos registrados en el taller.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vehicles
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY,
    registration_code
    VARCHAR
(
    20
) NOT NULL,
    model VARCHAR
(
    150
) NOT NULL,
    type VARCHAR
(
    20
) NOT NULL, -- MOTORCYCLE | CAR | VAN | TRUCK
    client_id BIGINT NOT NULL, -- FK al propietario
-- Campos de auditoría
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT pk_vehicles PRIMARY KEY
(
    id
),
    CONSTRAINT uq_vehicles_registration_code UNIQUE
(
    registration_code
),
    CONSTRAINT chk_vehicles_type CHECK
(
    type
    IN
(
    'MOTORCYCLE',
    'CAR',
    'VAN',
    'TRUCK'
)),
    CONSTRAINT fk_vehicles_client FOREIGN KEY
(
    client_id
) REFERENCES clients
(
    id
)
    );

-- -----------------------------------------------------------------------------
-- TABLA: workshop_tasks
-- Órdenes de trabajo. Vincula vehículo, mecánico y cliente.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS workshop_tasks
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY,
    diagnostic
    VARCHAR
(
    500
) NOT NULL,
    solution VARCHAR
(
    500
),
    preview_hours FLOAT NOT NULL,
    real_hours FLOAT NOT NULL DEFAULT 0,
    is_finished BOOLEAN NOT NULL DEFAULT FALSE,
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    init_date DATE NOT NULL,
    notes VARCHAR
(
    500
),
    client_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    mechanic_id BIGINT NOT NULL,
    -- Campos de auditoría
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT pk_workshop_tasks PRIMARY KEY
(
    id
),
    CONSTRAINT fk_tasks_client FOREIGN KEY
(
    client_id
) REFERENCES clients
(
    id
),
    CONSTRAINT fk_tasks_vehicle FOREIGN KEY
(
    vehicle_id
) REFERENCES vehicles
(
    id
),
    CONSTRAINT fk_tasks_mechanic FOREIGN KEY
(
    mechanic_id
) REFERENCES mechanics
(
    id
)
    );

-- -----------------------------------------------------------------------------
-- TABLA: refresh_tokens
-- Tokens de refresco JWT almacenados para permitir revocación.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_tokens
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY,
    token
    VARCHAR
(
    255
) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    user_id BIGINT NOT NULL,
    -- Campos de auditoría
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY
(
    id
),
    CONSTRAINT uq_refresh_tokens_token UNIQUE
(
    token
),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY
(
    user_id
) REFERENCES users
(
    id
)
    );

-- -----------------------------------------------------------------------------
-- FOREIGN KEYS diferidas de users (creadas aquí porque clients y mechanics
-- ya existen en este punto del script)
-- -----------------------------------------------------------------------------
ALTER TABLE users
    ADD CONSTRAINT fk_users_client FOREIGN KEY (client_id) REFERENCES clients (id);

ALTER TABLE users
    ADD CONSTRAINT fk_users_mechanic FOREIGN KEY (mechanic_id) REFERENCES mechanics (id);

-- -----------------------------------------------------------------------------
-- ÍNDICES DE RENDIMIENTO
-- Los índices aceleran las búsquedas más frecuentes a costa de algo de espacio.
-- Se crean sobre columnas usadas habitualmente en filtros (WHERE) y joins.
-- -----------------------------------------------------------------------------

-- Búsqueda de clientes por NIF (login, búsquedas frecuentes)
CREATE INDEX IF NOT EXISTS idx_clients_nif ON clients(nif);

-- Búsqueda de clientes por apellido
CREATE INDEX IF NOT EXISTS idx_clients_surname1 ON clients(surname1);

-- Búsqueda de mecánicos por NIF
CREATE INDEX IF NOT EXISTS idx_mechanics_nif ON mechanics(nif);

-- Búsqueda de vehículos por matrícula
CREATE INDEX IF NOT EXISTS idx_vehicles_registration ON vehicles(registration_code);

-- Búsqueda de vehículos por propietario
CREATE INDEX IF NOT EXISTS idx_vehicles_client ON vehicles(client_id);

-- Búsqueda de tareas por vehículo, mecánico y cliente
CREATE INDEX IF NOT EXISTS idx_tasks_vehicle ON workshop_tasks(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_tasks_mechanic ON workshop_tasks(mechanic_id);
CREATE INDEX IF NOT EXISTS idx_tasks_client ON workshop_tasks(client_id);

-- Búsqueda de refresh tokens por token (validación JWT)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);

-- Búsqueda de refresh tokens por usuario (logout: revocar todos los del usuario)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
