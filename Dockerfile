# =============================================================================
# Dockerfile - Workshop Management API
# =============================================================================
# Build multi-stage: usa dos imágenes separadas para construir y ejecutar.
# Ventaja: la imagen final es mucho más pequeña porque no incluye Maven,
# el JDK completo ni el código fuente, solo el JAR compilado.
#
# STAGE 1: Build
#   - Imagen con JDK + Maven
#   - Compila el proyecto y genera el JAR
#   - Resultado: target/workshop-management-api-1.0.0-SNAPSHOT.jar
#
# STAGE 2: Runtime
#   - Imagen solo con JRE (sin herramientas de compilación)
#   - Copia el JAR del stage anterior
#   - Resultado: imagen ligera (~200MB vs ~600MB)
# =============================================================================

# ── STAGE 1: Build ────────────────────────────────────────────────────────────
# eclipse-temurin es la distribución OpenJDK más usada en contenedores.
# 21-jdk-alpine: Java 21 + Alpine Linux (distribución mínima, ~5MB base).
FROM eclipse-temurin:21-jdk-alpine AS builder

# Directorio de trabajo dentro del contenedor
WORKDIR /build

# Copiar primero solo los ficheros de dependencias.
# Docker cachea cada capa: si pom.xml no cambia, no re-descarga dependencias.
# Esto acelera mucho las builds cuando solo cambia el código fuente.
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Descargar dependencias (cacheado si pom.xml no cambia)
RUN ./mvnw dependency:go-offline -q

# Copiar el código fuente
COPY src ./src

# Compilar y empaquetar (-DskipTests para acelerar la imagen de producción)
# Los tests se ejecutan en CI/CD, no en el build de Docker.
RUN ./mvnw package -DskipTests -q

# ── STAGE 2: Runtime ──────────────────────────────────────────────────────────
# Solo JRE (sin compilador), Alpine para tamaño mínimo.
FROM eclipse-temurin:21-jre-alpine AS runtime

# Usuario no-root por seguridad.
# Ejecutar la aplicación como root es un riesgo de seguridad.
RUN addgroup -S workshopgroup && adduser -S workshopuser -G workshopgroup

# Directorio de la aplicación
WORKDIR /app

# Crear directorio de logs con permisos correctos
RUN mkdir -p /var/log/workshop-management && \
    chown -R workshopuser:workshopgroup /var/log/workshop-management

# Copiar el JAR desde el stage de build
COPY --from=builder /build/target/workshop-management-api-*.jar app.jar

# Cambiar al usuario no-root
USER workshopuser

# Puerto que expone la aplicación (documentativo, no abre el puerto)
EXPOSE 8080

# Variables de entorno con valores por defecto.
# Sobreescritas en docker-compose.yml para producción.
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Opciones JVM optimizadas para contenedores:
# -XX:+UseContainerSupport: detecta los límites de CPU/memoria del contenedor
# -XX:MaxRAMPercentage=75.0: usa máximo 75% de la RAM asignada al contenedor
# -Djava.security.egd=...: acelera el arranque en Linux (generador de números aleatorios)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
