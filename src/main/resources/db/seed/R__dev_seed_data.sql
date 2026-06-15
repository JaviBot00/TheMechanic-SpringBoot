-- =============================================================================
-- R__dev_seed_data.sql  (prefijo R = Repeatable migration)
-- Datos de prueba para el perfil de desarrollo.
--
-- Las migraciones "Repeatable" (prefijo R__) se re-ejecutan cada vez que
-- cambia su checksum. Flyway las detecta modificadas y las vuelve a correr.
-- Son ideales para datos de prueba que pueden necesitar actualizarse.
--
-- Este fichero SOLO se carga en el perfil dev, porque application-dev.yml
-- añade classpath:db/seed a las locations de Flyway.
-- En producción, este directorio no existe en las locations.
--
-- Contraseñas: todas son "password123" encriptadas con BCrypt (cost 10).
-- Hash BCrypt de "password123": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- =============================================================================

-- Limpiamos en orden inverso a las FK para evitar violaciones de integridad
DELETE
FROM refresh_tokens;
DELETE
FROM workshop_tasks;
DELETE
FROM vehicles;
DELETE
FROM users;
DELETE
FROM clients;
DELETE
FROM mechanics;

-- -----------------------------------------------------------------------------
-- CLIENTES
-- -----------------------------------------------------------------------------
INSERT INTO clients (id, client_code, name, surname1, surname2, nif, email, telephone, created_at, updated_at)
VALUES (1, 1, 'Luis', 'Martinez', 'Garcia', '87654321B', 'luismartinez@gmail.com', '600654321', NOW(), NOW()),
       (2, 2, 'Ana', 'Gomez', 'Lopez', '12345678A', 'anagomez@gmail.com', '600123456', NOW(), NOW()),
       (3, 3, 'Carlos', 'Perez', 'Ruiz', '23456789C', 'carlosperez@gmail.com', '611234567', NOW(), NOW()),
       (4, 4, 'Maria', 'Lopez', 'Sanchez', '34567890D', 'marialopez@gmail.com', '622345678', NOW(), NOW()),
       (5, 5, 'Javier', 'Fernandez', 'Moreno', '45678901E', 'javierf@gmail.com', '633456789', NOW(), NOW()),
       (6, 6, 'Laura', 'Diaz', 'Torres', '56789012F', 'lauradiaz@gmail.com', '644567890', NOW(), NOW()),
       (7, 7, 'Pedro', 'Hernandez', 'Jimenez', '67890123G', 'pedroh@gmail.com', '655678901', NOW(), NOW()),
       (8, 8, 'Sofia', 'Romero', 'Navarro', '78901234H', 'sofiaromero@gmail.com', '666789012', NOW(), NOW()),
       (9, 9, 'Daniel', 'Alvarez', 'Castro', '89012345J', 'danielalvarez@gmail.com', '677890123', NOW(), NOW()),
       (10, 10, 'Elena', 'Ortega', 'Vidal', '90123456K', 'elenaortega@gmail.com', '688901234', NOW(), NOW());

-- -----------------------------------------------------------------------------
-- MECÁNICOS
-- -----------------------------------------------------------------------------
INSERT INTO mechanics (id, name, surname1, surname2, nif, email, telephone, registration_date, specialty, created_at,
                       updated_at)
VALUES (1, 'Paco', 'Omero', 'Garcia', '25658192R', 'pacomelero@gmail.com', '654738129', '2018-09-01',
        'Mecánica General', NOW(), NOW()),
       (2, 'Ana', 'Lopez', 'Martinez', '37849210A', 'analopez@gmail.com', '612345678', '2017-05-15', 'Electricidad',
        NOW(), NOW()),
       (3, 'Carlos', 'Sanchez', 'Ruiz', '48920317B', 'carlossanchez@gmail.com', '623456789', '2019-03-22',
        'Chapa y Pintura', NOW(), NOW()),
       (4, 'Laura', 'Fernandez', 'Moreno', '59031428C', 'laurafernandez@gmail.com', '634567890', '2016-01-10',
        'Mecánica General', NOW(), NOW()),
       (5, 'Javier', 'Hernandez', 'Lopez', '60142539D', 'javierh@gmail.com', '645678901', '2020-01-05', 'Electricidad',
        NOW(), NOW()),
       (6, 'Sofia', 'Diaz', 'Torres', '71253640E', 'sofiadiaz@gmail.com', '656789012', '2018-11-30', 'Mecánica General',
        NOW(), NOW()),
       (7, 'Pedro', 'Romero', 'Navarro', '82364751F', 'pedroromero@gmail.com', '667890123', '2014-02-18',
        'Chapa y Pintura', NOW(), NOW()),
       (8, 'Elena', 'Ortega', 'Vidal', '93475862G', 'elenaortega@gmail.com', '678901234', '2021-04-12', 'Electricidad',
        NOW(), NOW()),
       (9, 'Daniel', 'Alvarez', 'Castro', '14586973H', 'danielalvarez2@gmail.com', '689012345', '2015-09-07',
        'Mecánica General', NOW(), NOW());

-- -----------------------------------------------------------------------------
-- VEHÍCULOS
-- -----------------------------------------------------------------------------
INSERT INTO vehicles (id, registration_code, model, type, client_id, created_at, updated_at)
VALUES (1, 'TOYOTA-001', 'Toyota Corolla', 'CAR', 1, NOW(), NOW()),
       (2, 'FORD-001', 'Ford Focus', 'CAR', 2, NOW(), NOW()),
       (3, 'BMW-MOTO-001', 'BMW R1250GS', 'MOTORCYCLE', 1, NOW(), NOW()),
       (4, 'VW-GOLF-001', 'Volkswagen Golf', 'CAR', 3, NOW(), NOW()),
       (5, 'HONDA-001', 'Honda Civic', 'CAR', 5, NOW(), NOW()),
       (6, 'RENAULT-001', 'Renault Megane', 'CAR', 8, NOW(), NOW()),
       (7, 'VW-PASSAT-001', 'Volkswagen Passat', 'VAN', 10, NOW(), NOW()),
       (8, 'SEAT-IBIZA-001', 'Seat Ibiza', 'VAN', 6, NOW(), NOW());

-- -----------------------------------------------------------------------------
-- USUARIOS (vinculados a clientes y mecánicos)
-- Contraseña para todos: "password123"
-- -----------------------------------------------------------------------------
INSERT INTO users (id, username, password, role, enabled, client_id, mechanic_id, created_at, updated_at)
VALUES
    -- Admin puro (sin client ni mechanic vinculado)
    (1, 'admin', '$2a$10$12Ak1Nhxvx4Qnqq/wUp5xOPcg8dTLss1Mg7LpSlNhS4fKHRsOsKBO', 'ADMIN', TRUE, NULL, NULL, NOW(),
     NOW()),
    -- Clientes
    (2, 'luismartinez', '$2a$10$12Ak1Nhxvx4Qnqq/wUp5xOPcg8dTLss1Mg7LpSlNhS4fKHRsOsKBO', 'CLIENT', TRUE, 1, NULL, NOW(),
     NOW()),
    (3, 'anagomez', '$2a$10$12Ak1Nhxvx4Qnqq/wUp5xOPcg8dTLss1Mg7LpSlNhS4fKHRsOsKBO', 'CLIENT', TRUE, 2, NULL, NOW(),
     NOW()),
    (4, 'carlosperez', '$2a$10$12Ak1Nhxvx4Qnqq/wUp5xOPcg8dTLss1Mg7LpSlNhS4fKHRsOsKBO', 'CLIENT', TRUE, 3, NULL, NOW(),
     NOW()),
    -- Mecánicos
    (5, 'paco.omero', '$2a$10$12Ak1Nhxvx4Qnqq/wUp5xOPcg8dTLss1Mg7LpSlNhS4fKHRsOsKBO', 'MECHANIC', TRUE, NULL, 1, NOW(),
     NOW()),
    (6, 'ana.lopez', '$2a$10$12Ak1Nhxvx4Qnqq/wUp5xOPcg8dTLss1Mg7LpSlNhS4fKHRsOsKBO', 'MECHANIC', TRUE, NULL, 2, NOW(),
     NOW()),
    (7, 'carlos.sanchez', '$2a$10$12Ak1Nhxvx4Qnqq/wUp5xOPcg8dTLss1Mg7LpSlNhS4fKHRsOsKBO', 'MECHANIC', TRUE, NULL, 3,
     NOW(), NOW());

-- -----------------------------------------------------------------------------
-- TAREAS DE TALLER
-- -----------------------------------------------------------------------------
INSERT INTO workshop_tasks (id, diagnostic, solution, preview_hours, real_hours, is_finished, is_paid, init_date, notes,
                            client_id, vehicle_id, mechanic_id, created_at, updated_at)
VALUES (1, 'Cambio de aceite y filtro', 'Aceite 5W30 sintético + filtro Mann', 2, 2, TRUE, TRUE, '2026-02-17', NULL, 1,
        1, 1, NOW(), NOW()),
       (2, 'Revisión de frenos', 'Pastillas delanteras sustituidas', 4, 3, TRUE, TRUE, '2026-03-15',
        'Discos en buen estado', 2, 2, 2, NOW(), NOW()),
       (3, 'Rotación de neumáticos', NULL, 1, 1.5, FALSE, FALSE, '2026-04-10', NULL, 1, 3, 3, NOW(), NOW()),
       (4, 'Diagnóstico motor', NULL, 8, 4, FALSE, FALSE, '2026-05-15', 'Pendiente de pieza de recambio', 3, 4, 4,
        NOW(), NOW()),
       (5, 'Sustitución batería', 'Batería Varta 72Ah instalada', 1, 1, TRUE, FALSE, '2026-06-10', NULL, 5, 5, 5, NOW(),
        NOW()),
       (6, 'Revisión caja de cambios', NULL, 6, 0, FALSE, FALSE, '2026-07-15', NULL, 6, 6, 6, NOW(), NOW()),
       (7, 'Cambio filtro de aire', 'Filtro Mann C25014 montado', 0.5, 0.5, TRUE, TRUE, '2026-08-10', NULL, 1, 7, 7,
        NOW(), NOW()),
       (8, 'Vaciado y llenado líquido refrigerante', NULL, 2, 1, FALSE, FALSE, '2026-09-15', NULL, 8, 8, 8, NOW(),
        NOW());

-- Reset sequences after manual ID inserts
ALTER TABLE clients ALTER COLUMN id RESTART WITH 11;
ALTER TABLE mechanics ALTER COLUMN id RESTART WITH 10;
ALTER TABLE vehicles ALTER COLUMN id RESTART WITH 9;
ALTER TABLE users ALTER COLUMN id RESTART WITH 8;
ALTER TABLE workshop_tasks ALTER COLUMN id RESTART WITH 9;
ALTER TABLE refresh_tokens ALTER COLUMN id RESTART WITH 1;
