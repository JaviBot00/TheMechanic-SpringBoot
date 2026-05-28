# 🔧 Workshop Management API

API REST para la **gestión integral de un taller mecánico**, desarrollada con Spring Boot 4 y Java 21.

Permite gestionar clientes, vehículos, mecánicos y órdenes de trabajo, con autenticación segura mediante JWT y control de acceso por roles.

---

## 📋 Tabla de contenidos

- [Descripción](#-descripción)
- [Stack tecnológico](#-stack-tecnológico)
- [Arquitectura](#-arquitectura)
- [Requisitos previos](#-requisitos-previos)
- [Instalación y arranque](#-instalación-y-arranque)
- [Perfiles de entorno](#-perfiles-de-entorno)
- [Documentación de la API](#-documentación-de-la-api)
- [Tests](#-tests)
- [Estructura del proyecto](#-estructura-del-proyecto)
- [Documentación técnica](#-documentación-técnica)

---

## 📖 Descripción

Workshop Management API es el backend de un sistema de gestión para talleres mecánicos. Implementa las siguientes funcionalidades principales:

- **Gestión de clientes**: alta, baja, modificación y búsqueda de clientes
- **Gestión de vehículos**: registro de vehículos por cliente, con tipos diferenciados (moto, coche, furgoneta, camión) y tarifas horarias propias
- **Gestión de mecánicos**: registro de mecánicos con especialidades y seguimiento de carga de trabajo
- **Órdenes de trabajo (WorkshopTask)**: creación, asignación, seguimiento de progreso, facturación y gestión de pagos
- **Reportes y estadísticas**: informes de actividad, facturación y rendimiento
- **Autenticación y autorización**: JWT con roles ADMIN, MECHANIC y CLIENT

---

## 🔨 Stack tecnológico

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 (LTS) | Lenguaje principal |
| Spring Boot | 4.0.6 | Framework base |
| Spring Security | 7.0.x | Autenticación y autorización |
| Spring Data JPA | 2025.1.x | Acceso a base de datos |
| Hibernate | 7.1.x | ORM |
| PostgreSQL | 16+ | Base de datos (producción) |
| H2 | Embebida | Base de datos (desarrollo) |
| Flyway | 11.x | Migraciones de esquema |
| JJWT | 0.13.0 | Generación y validación de JWT |
| MapStruct | 1.6.3 | Mapeo entidad ↔ DTO |
| Lombok | 1.18.x | Reducción de boilerplate |
| SpringDoc OpenAPI | 3.0.3 | Documentación automática |
| JUnit 5 | 5.x | Tests unitarios |
| Mockito | 5.x | Mocking en tests |
| JaCoCo | 0.8.x | Cobertura de tests |
| Docker + Compose | 24+ | Contenedorización |
| Maven | 3.9+ | Gestión de dependencias y build |

---

## 📦 Arquitectura

El proyecto sigue una **arquitectura por feature** (también llamada Vertical Slice), donde cada funcionalidad de negocio agrupa todas sus capas:

```cmd
com.hotguy.workshopmanagement/
├── auth/           → Autenticación JWT (login, refresh, logout)
├── client/         → Gestión de clientes
├── mechanic/       → Gestión de mecánicos
├── vehicle/        → Gestión de vehículos
├── task/           → Órdenes de trabajo (WorkshopTask)
├── report/         → Reportes y estadísticas
├── common/         → Código compartido (excepciones, auditoría, config)
└── config/         → Configuración de Spring Security, OpenAPI, etc.
```

Cada feature contiene:

- `model/` — Entidad JPA
- `repository/` — Interfaz Spring Data (acceso a BD)
- `service/` — Lógica de negocio
- `controller/` — Endpoints REST
- `dto/` — Objetos de transferencia de datos
- `mapper/` — Conversión entidad ↔ DTO (MapStruct)

---

## 🧩 Requisitos previos

- **Java 21** o superior ([Descargar](https://adoptium.net/))
- **Maven 3.9+** ([Descargar](https://maven.apache.org/download.cgi))
- **Docker y Docker Compose** (para el perfil de producción)
- **IntelliJ IDEA** recomendado (con plugins Lombok y MapStruct)

---

## 🚀 Instalación y arranque

### Perfil de desarrollo (H2, sin Docker)

```bash
# 1. Clonar el repositorio
git clone https://github.com/tu-usuario/workshop-management-api.git
cd workshop-management-api

# 2. Copiar plantilla de variables de entorno
cp .env.example .env

# 3. Compilar el proyecto
mvn clean package -DskipTests

# 4. Arrancar con perfil dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

La API estará disponible en: `http://localhost:8080`
La consola H2 estará en: `http://localhost:8080/h2-console`
Swagger UI estará en: `http://localhost:8080/swagger-ui.html`

### Perfil de producción (PostgreSQL, Docker)

```bash
# 1. Copiar y configurar variables de entorno
cp .env.example .env
# Editar .env con las credenciales reales

# 2. Arrancar todos los servicios
docker-compose up -d

# 3. Ver logs
docker-compose logs -f api
```

---

## 🌍 Perfiles de entorno

| Perfil | BD | Activación | Swagger | Datos de prueba |
|---|---|---|---|---|
| `dev` | H2 en memoria | IntelliJ / `-Dspring.profiles.active=dev` | ✅ Activo | ✅ Cargados |
| `prod` | PostgreSQL | Variable de entorno `SPRING_PROFILES_ACTIVE=prod` | ❌ Desactivado | ❌ Solo esquema |

---

## 📚 Documentación de la API

Con el perfil `dev` activo, la documentación interactiva está disponible en:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api-docs`

### Autenticación

La API usa **JWT Bearer Token**. Flujo de autenticación:

```cmd
POST /api/v1/auth/login          → Obtiene access token + refresh token
POST /api/v1/auth/refresh        → Renueva el access token
POST /api/v1/auth/logout         → Invalida el refresh token
```

Para usar un endpoint protegido, incluir el header:

```cmd
Authorization: Bearer <access_token>
```

---

## 🧪 Tests

```bash
# Ejecutar todos los tests
mvn test

# Ejecutar tests con reporte de cobertura
mvn verify

# Ver reporte de cobertura
open target/site/jacoco/index.html
```

---

## 📁 Estructura del proyecto

```cmd
workshop-management-api/
├── src/
│   ├── main/
│   │   ├── java/com/hotguy/workshopmanagement/
│   │   │   ├── WorkshopManagementApplication.java
│   │   │   ├── auth/
│   │   │   ├── client/
│   │   │   ├── mechanic/
│   │   │   ├── vehicle/
│   │   │   ├── task/
│   │   │   ├── report/
│   │   │   ├── common/
│   │   │   └── config/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/
│   │           ├── migration/      ← Scripts Flyway (esquema)
│   │           └── seed/           ← Datos de prueba (solo dev)
│   └── test/
│       └── java/com/hotguy/workshopmanagement/
├── docs/                           ← Documentación técnica
│   ├── 00-arquitectura-general.md
│   ├── 01-base-proyecto.md
│   └── ...
├── docker-compose.yml
├── Dockerfile
├── .env.example
├── .gitignore
└── pom.xml
```

---

## 📖 Documentación técnica

La documentación técnica detallada se encuentra en la carpeta [`docs/`](./docs/):

| Documento | Descripción |
|---|---|
| [00 - Arquitectura general](./docs/00-arquitectura-general.md) | Visión global del sistema y decisiones de diseño |
| [01 - Base del proyecto](./docs/01-base-proyecto.md) | pom.xml, perfiles, configuración |
| [02 - Modelo de datos](./docs/02-modelo-datos.md) | Entidades JPA y migraciones Flyway |
| [03 - Seguridad JWT](./docs/03-seguridad-jwt.md) | Autenticación, tokens y roles |
| [04 - Feature Client](./docs/04-feature-client.md) | API de gestión de clientes |
| [05 - Feature Mechanic](./docs/05-feature-mechanic.md) | API de gestión de mecánicos |
| [06 - Feature Vehicle](./docs/06-feature-vehicle.md) | API de gestión de vehículos |
| [07 - Feature WorkshopTask](./docs/07-feature-workshoptask.md) | API de órdenes de trabajo |
| [08 - Reportes](./docs/08-reportes.md) | API de informes y estadísticas |
| [09 - Tests](./docs/09-tests.md) | Estrategia de testing |
| [10 - Docker y despliegue](./docs/10-docker-despliegue.md) | Contenedorización y puesta en producción |
