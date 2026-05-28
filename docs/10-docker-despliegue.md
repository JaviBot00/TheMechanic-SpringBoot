# 10 — Docker y despliegue

## ¿Por qué Docker?

Sin Docker, desplegar una aplicación Java implica instalar Java en el servidor, configurar variables de entorno, gestionar el proceso manualmente, etc. Con Docker, la aplicación y todo su entorno viajan juntos en un contenedor reproducible.

**"Funciona en mi máquina"** → con Docker también funciona en producción, porque el entorno es idéntico.

---

## Conceptos clave

### Imagen vs Contenedor

- **Imagen**: plantilla inmutable (como una clase Java)
- **Contenedor**: instancia en ejecución de una imagen (como un objeto Java)

### Volumen

Almacenamiento persistente fuera del contenedor. Sin volumen, los datos se pierden al reiniciar el contenedor.

### Red (network)

Permite que los contenedores se comuniquen entre sí por nombre. `workshop-api` puede conectarse a `workshop-db` usando `db` como hostname.

---

## Comandos esenciales

```bash
# Arrancar todos los servicios en background
docker-compose up -d

# Ver logs en tiempo real
docker-compose logs -f api
docker-compose logs -f db

# Parar sin borrar datos
docker-compose down

# Parar Y borrar volúmenes (⚠ borra la BD)
docker-compose down -v

# Reconstruir la imagen de la API tras cambios en el código
docker-compose up -d --build api

# Ejecutar comandos dentro del contenedor
docker-compose exec api sh
docker-compose exec db psql -U workshop_user workshopdb

# Ver estado de los contenedores
docker-compose ps
```

---

## Arranque de producción paso a paso

```bash
# 1. Clonar el repo en el servidor
git clone https://github.com/tu-usuario/workshop-management-api.git
cd workshop-management-api

# 2. Configurar variables de entorno
cp .env.example .env
nano .env  # Rellenar con valores reales

# Contenido mínimo del .env en producción:
# POSTGRES_DB=workshopdb
# POSTGRES_USER=workshop_user
# POSTGRES_PASSWORD=contraseña-segura-aqui
# SECURITY_JWT_SECRET_KEY=$(openssl rand -base64 64)

# 3. Arrancar
docker-compose up -d

# 4. Verificar que arrancó correctamente
docker-compose ps
curl http://localhost:8080/actuator/health

# 5. Crear usuario admin inicial (solo la primera vez)
# La BD se inicializa con Flyway automáticamente.
# Si quieres añadir el usuario admin en producción, crear un script
# V2__insert_admin.sql en db/migration/ con el hash BCrypt de la contraseña.
```

---

## Generar contraseña BCrypt para producción

```bash
# Opción 1: con htpasswd (si está instalado)
htpasswd -bnBC 10 "" "miContraseña" | tr -d ':\n'

# Opción 2: desde la consola H2 en dev o con una clase Java temporal
# El hash de "password123" con cost 10 es:
# $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

---

## Actualizar la aplicación

```bash
# 1. Obtener el nuevo código
git pull

# 2. Reconstruir y reiniciar solo la API (sin tocar la BD)
docker-compose up -d --build api

# Flyway aplicará automáticamente las nuevas migraciones al arrancar
```

---

## Estructura de la red Docker

```cmd
Internet
    │
    ▼
:8080 (expuesto)
    │
    ▼
┌────────────────────────────────────┐
│       workshop-network             │
│                                    │
│  workshop-api ──────► workshop-db  │
│  Spring Boot        PostgreSQL     │
│     :8080               :5432      │
└────────────────────────────────────┘
                              ↑
                    :5432 expuesto SOLO en localhost
                    (para DBeaver/pgAdmin en desarrollo)
                    En producción real: eliminar esta exposición
```
