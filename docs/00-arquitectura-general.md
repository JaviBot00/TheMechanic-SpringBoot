# 00 — Arquitectura general

## ¿Qué es Workshop Management API?

Es el backend de un sistema de gestión para talleres mecánicos. Expone una **API REST** (interfaz de comunicación basada en HTTP) que cualquier cliente puede consumir: una aplicación web, una aplicación móvil, Postman, o cualquier otro sistema.

El backend se encarga de toda la lógica de negocio, la persistencia de datos y la seguridad. El frontend (no incluido en este proyecto) solo pinta los datos y envía acciones del usuario.

---

## Diagrama de la arquitectura

```cmd
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENTE (Frontend)                      │
│                    (Web App / Mobile / Postman)                 │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP/HTTPS
                             │ JSON
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT APPLICATION                      │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    SECURITY LAYER                       │    │
│  │  JWT Filter → Autenticación → Autorización por rol      │    │
│  └─────────────────────────────────────────────────────────┘    │
│                            │                                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │  Client  │ │ Mechanic │ │ Vehicle  │ │   Task   │  ...       │
│  │          │ │          │ │          │ │          │            │
│  │Controller│ │Controller│ │Controller│ │Controller│            │
│  │ Service  │ │ Service  │ │ Service  │ │ Service  │            │
│  │   Repo   │ │   Repo   │ │   Repo   │ │   Repo   │            │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘            │
│       └────────────┴────────────┴────────────┘                  │
│                            │                                    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              Spring Data JPA + Hibernate                │    │
│  └─────────────────────────────────────────────────────────┘    │
└────────────────────────────┬────────────────────────────────────┘
                             │ JDBC
                             ▼
         ┌───────────────────────────────────┐
         │            BASE DE DATOS          │
         │   H2 (dev) / PostgreSQL (prod)    │
         └───────────────────────────────────┘
```

---

## Flujo de una petición HTTP

Cuando el frontend hace una petición a la API, pasa por estas capas en orden:

```cmd
HTTP Request
    │
    ▼
┌─────────────────────────────────┐
│      JWT FILTER                 │  ← ¿El token JWT es válido?
│  (OncePerRequestFilter)         │     Si no → 401 Unauthorized
└─────────────┬───────────────────┘
              │
              ▼
┌─────────────────────────────────┐
│      CONTROLLER                 │  ← Recibe la petición HTTP
│  (@RestController)              │     Valida los datos de entrada (DTOs)
│  (@RequestMapping)              │     Llama al Service
└─────────────┬───────────────────┘
              │
              ▼
┌─────────────────────────────────┐
│      SERVICE                    │  ← Contiene la lógica de negocio
│  (@Service)                     │     Orquesta las operaciones
│  (@Transactional)               │     Lanza excepciones de negocio
└─────────────┬───────────────────┘
              │
              ▼
┌─────────────────────────────────┐
│      REPOSITORY                 │  ← Accede a la base de datos
│  (JpaRepository)                │     Spring genera la implementación
│  (Queries JPQL/SQL)             │     automáticamente
└─────────────┬───────────────────┘
              │
              ▼
┌─────────────────────────────────┐
│      BASE DE DATOS              │
│  (H2 / PostgreSQL)              │
└─────────────────────────────────┘
              │
              │ (respuesta sube por las mismas capas)
              ▼
HTTP Response (JSON)
```

---

## Decisiones de diseño y por qué

### Arquitectura por feature (no por capa)

El código se organiza por funcionalidad de negocio, no por tipo técnico:

```cmd
✅ Por feature (elegido)         ❌ Por capa (descartado)
─────────────────────────        ─────────────────────────
client/                          controller/
  ClientController.java            ClientController.java
  ClientService.java               MechanicController.java
  ClientRepository.java            VehicleController.java
  ClientMapper.java              service/
  dto/                             ClientService.java
    ClientRequestDto.java          MechanicService.java
    ClientResponseDto.java       repository/
mechanic/                          ClientRepository.java
  MechanicController.java          MechanicRepository.java
  ...                            model/
                                   Client.java
                                   Mechanic.java
```

**Ventaja principal**: cuando necesitas trabajar en una funcionalidad, todos sus ficheros están juntos. No tienes que saltar entre 4 paquetes distintos para añadir un campo nuevo a un cliente.

### Entidad User separada del modelo de negocio

Existe una entidad `User` que gestiona la autenticación, vinculada opcionalmente a `Client` o `Mechanic`:

```cmd
User (credenciales: username, password, role)
  │
  ├─── OneToOne ──→ Client (datos de negocio del cliente)
  │
  └─── OneToOne ──→ Mechanic (datos de negocio del mecánico)
```

Un `User` con rol `ADMIN` no necesita ser cliente ni mecánico, por eso la relación es opcional.

**Por qué no meter `password` en `Client` directamente**: un cliente no "sabe" que existe un sistema de autenticación. Si mañana cambiamos de JWT a OAuth2, solo tocamos la entidad `User`, no el modelo de negocio.

### DTOs en lugar de entidades directas en la API

Las entidades JPA nunca se devuelven directamente como respuesta JSON. Siempre se convierten a DTOs (Data Transfer Objects):

```cmd
Entidad JPA      →   MapStruct   →   DTO   →   JSON Response
(Client.java)         Mapper         (ClientResponseDto)
```

**Por qué**: las entidades JPA pueden tener campos que no queremos exponer (`password`, `deletedAt`, referencias circulares que provocan bucles infinitos al serializar). Los DTOs nos dan control total sobre qué datos mandamos al cliente.

### Soft delete

Cuando se "borra" un cliente, mecánico o vehículo, en realidad se rellena el campo `deletedAt` con la fecha actual. El registro sigue en la BD pero todas las queries lo excluyen automáticamente.

**Por qué**: un taller no puede perder el historial de un cliente. Si se borra un cliente, sus reparaciones históricas deben seguir existiendo para auditoría.

### Flyway para migraciones de BD

Los scripts SQL que crean/modifican tablas se versionan como código en `src/main/resources/db/migration/`. Cada script tiene un número de versión:

```cmd
V1__init_schema.sql
V2__add_users_table.sql
V3__add_soft_delete_columns.sql
```

Flyway registra qué scripts ya se han ejecutado en una tabla especial (`flyway_schema_history`). Al arrancar la aplicación, solo ejecuta los scripts nuevos. Así, todos los entornos (dev, prod, el entorno de un compañero) tienen siempre el mismo esquema.

---

## Roles y permisos

| Acción | ADMIN | MECHANIC | CLIENT |
|---|---|---|---|
| Gestionar usuarios (CRUD) | ✅ | ❌ | ❌ |
| Ver todos los clientes | ✅ | ✅ | ❌ |
| Ver sus propios datos | ✅ | ✅ | ✅ |
| CRUD vehículos | ✅ | ✅ | ❌ |
| Ver sus vehículos | ✅ | ✅ | ✅ |
| CRUD tareas de taller | ✅ | ✅ | ❌ |
| Ver sus tareas | ✅ | ✅ | ✅ |
| Marcar tarea como pagada | ✅ | ❌ | ❌ |
| Ver reportes | ✅ | ✅ | ❌ |

---

## Estructura de URLs de la API

```cmd
/api/v1/auth/login              POST   → Obtener tokens JWT
/api/v1/auth/refresh            POST   → Renovar access token
/api/v1/auth/logout             POST   → Invalidar refresh token

/api/v1/clients                 GET    → Listar clientes
/api/v1/clients                 POST   → Crear cliente
/api/v1/clients/{id}            GET    → Obtener cliente
/api/v1/clients/{id}            PUT    → Actualizar cliente
/api/v1/clients/{id}            DELETE → Eliminar cliente (soft delete)

/api/v1/mechanics               GET/POST/PUT/DELETE
/api/v1/vehicles                GET/POST/PUT/DELETE
/api/v1/tasks                   GET/POST/PUT/DELETE

/api/v1/reports/summary         GET    → Resumen general
/api/v1/reports/revenue         GET    → Informe de facturación
/api/v1/reports/mechanic-load   GET    → Carga de trabajo por mecánico
```

El prefijo `/api/v1/` permite en el futuro tener una versión 2 de la API sin romper los clientes que usan la versión 1.

---

## Entornos y cómo se activan

```cmd
┌─────────────────────────────────────────────────────────┐
│  DESARROLLO (dev)                                       │
│  ────────────────────────────────────────────────────── │
│  Activación: -Dspring.profiles.active=dev (IntelliJ)    │
│  BD: H2 en memoria (se destruye al parar)               │
│  Datos: Scripts de seed con datos de prueba             │
│  Swagger: Activo en /swagger-ui.html                    │
│  Logs SQL: Visibles en consola                          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  PRODUCCIÓN (prod)                                      │
│  ────────────────────────────────────────────────────── │
│  Activación: SPRING_PROFILES_ACTIVE=prod (Docker)       │
│  BD: PostgreSQL (persistente en volumen Docker)         │
│  Datos: Solo el esquema (sin datos de prueba)           │
│  Swagger: Desactivado                                   │
│  Logs SQL: Desactivados                                 │
│  Credenciales: Variables de entorno, nunca en código    │
└─────────────────────────────────────────────────────────┘
```
