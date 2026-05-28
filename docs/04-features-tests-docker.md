# 04 — Features: Client, Mechanic, Vehicle, Task y Reportes

## ¿Qué contiene este fascículo?

La implementación completa de todas las features de negocio, los tests (unitarios e integración), el Dockerfile, docker-compose y la configuración de OpenAPI.

---

## Ficheros entregados

```
fasciculo-4/
├── Dockerfile
├── docker-compose.yml
└── src/
    ├── main/java/com/workshopmanagement/
    │   ├── client/
    │   │   ├── controller/ClientController.java
    │   │   ├── service/ClientService.java
    │   │   ├── service/ClientSecurityService.java
    │   │   ├── repository/ClientRepository.java
    │   │   ├── mapper/ClientMapper.java
    │   │   └── dto/{ClientRequest, ClientResponse}.java
    │   ├── mechanic/          (misma estructura que client/)
    │   ├── vehicle/           (misma estructura + VehicleSecurityService)
    │   ├── task/              (misma estructura + AddHoursRequest)
    │   ├── report/
    │   │   ├── controller/ReportController.java
    │   │   ├── service/ReportService.java
    │   │   └── dto/SummaryReportResponse.java
    │   └── config/OpenApiConfig.java
    └── test/java/com/workshopmanagement/
        ├── auth/JwtServiceTest.java
        ├── auth/AuthControllerIntegrationTest.java
        ├── client/ClientServiceTest.java
        ├── client/ClientControllerIntegrationTest.java
        ├── task/WorkshopTaskModelTest.java
        └── vehicle/VehicleTypeTest.java
```

---

## La arquitectura por feature en la práctica

Cada feature sigue exactamente la misma estructura de cuatro capas:

```
HTTP Request
     │
     ▼
Controller          ← Recibe HTTP, valida con @Valid, devuelve ResponseEntity
     │
     ▼
Service             ← Lógica de negocio, @Transactional, @PreAuthorize
     │
     ▼
Repository          ← Acceso a BD, Spring Data genera el SQL
     │
     ▼
  Base de datos
```

El **Mapper** (MapStruct) traduce entre las capas:
```
Request DTO ──MapStruct──→ Entity ──→ BD
BD ──→ Entity ──MapStruct──→ Response DTO ──→ JSON
```

---

## Spring Data JPA: queries por nombre de método

Spring Data genera automáticamente el SQL mirando el nombre del método:

```java
// Spring Data genera: SELECT * FROM clients WHERE nif = ?
Optional<Client> findByNif(String nif);

// SELECT * FROM clients WHERE surname1 ILIKE '%?%'
Page<Client> findBySurname1ContainingIgnoreCase(String surname1, Pageable pageable);

// SELECT COUNT(*) > 0 FROM clients WHERE nif = ?
boolean existsByNif(String nif);

// SELECT * FROM workshop_tasks WHERE finished = false
Page<WorkshopTask> findByFinishedFalse(Pageable pageable);
```

La convención de nombres es: `find|exists|count` + `By` + `campo` + `Condición`.

Para queries más complejas, usamos `@Query` con JPQL (lenguaje de queries orientado a objetos):

```java
@Query("SELECT COUNT(t) FROM WorkshopTask t WHERE t.finished = false")
long countPendingTasks();
```

JPQL usa nombres de clases y campos Java, no nombres de tablas y columnas SQL. Hibernate lo traduce al SQL específico de cada BD.

---

## Paginación con Spring Data

Todos los endpoints de listado devuelven `Page<T>` en lugar de `List<T>`. Esto es fundamental para aplicaciones reales donde puede haber miles de registros.

```java
// En el Controller:
@GetMapping
public ResponseEntity<Page<ClientResponse>> listClients(
        @PageableDefault(size = 20, sort = "surname1") Pageable pageable) {
    return ResponseEntity.ok(clientService.listClients(pageable));
}
```

El cliente controla la paginación con query params:
```
GET /api/v1/clients?page=0&size=10&sort=surname1,asc
GET /api/v1/clients?page=1&size=5&sort=name,desc
```

La respuesta incluye metadatos de paginación:
```json
{
  "content": [...],
  "totalElements": 47,
  "totalPages": 5,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false
}
```

---

## Control de acceso granular con @PreAuthorize

Combinamos dos niveles de seguridad:

**Nivel 1 - URL** (en `SecurityConfig`): reglas globales por ruta.
```java
.requestMatchers("/api/v1/auth/register").hasRole("ADMIN")
.requestMatchers(HttpMethod.PATCH, "/api/v1/tasks/*/pay").hasRole("ADMIN")
```

**Nivel 2 - Método** (con `@PreAuthorize` en Services): reglas específicas por operación.
```java
// Un CLIENT solo puede ver sus propios datos
@PreAuthorize("hasAnyRole('ADMIN','MECHANIC') or " +
    "(hasRole('CLIENT') and @clientSecurityService.isOwner(authentication, #id))")
public ClientResponse getClientById(Long id) { ... }
```

La expresión SpEL `@clientSecurityService.isOwner(authentication, #id)` llama a un bean de Spring para evaluar si el usuario autenticado es el propietario del recurso.

`#id` referencia el parámetro `id` del método anotado.

---

## MapStruct en detalle

MapStruct genera el código de conversión en tiempo de compilación. Para ver el código generado, busca en `target/generated-sources/annotations/`.

```java
// Lo que escribimos:
@Mapper(componentModel = "spring")
public interface ClientMapper {
    @Mapping(target = "vehicleCount", expression = "java(client.getVehicles().size())")
    ClientResponse toResponse(Client client);
}

// Lo que MapStruct genera (aproximado):
@Component
public class ClientMapperImpl implements ClientMapper {
    @Override
    public ClientResponse toResponse(Client client) {
        if (client == null) return null;
        return new ClientResponse(
            client.getId(),
            client.getClientCode(),
            client.getName(),
            // ... todos los campos mapeados
            client.getVehicles().size(),  // expresión Java
            client.getCreatedAt(),
            client.getUpdatedAt()
        );
    }
}
```

### `updateEntityFromRequest` con `@MappingTarget`

Para actualizaciones parciales (no crear entidad nueva, sino modificar la existente):

```java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void updateEntityFromRequest(ClientRequest request, @MappingTarget Client client);
```

Con `IGNORE`: si un campo del request es `null`, el campo de la entidad no se toca. Útil cuando el cliente solo quiere actualizar algunos campos.

---

## Soft delete: cómo funciona en la práctica

Cuando se llama a `deleteClient(id)`:

```java
public void deleteClient(Long id) {
    Client client = findClientOrThrow(id);
    client.softDelete();          // rellena deletedAt = Instant.now()
    clientRepository.save(client); // persiste el cambio
}
```

A partir de ese momento, `@SQLRestriction("deleted_at IS NULL")` hace que Hibernate excluya automáticamente ese registro de todas las queries:

```sql
-- Lo que escribe el desarrollador:
SELECT * FROM clients WHERE id = 42

-- Lo que Hibernate ejecuta realmente (aplica el filtro automáticamente):
SELECT * FROM clients WHERE id = 42 AND deleted_at IS NULL
```

El registro sigue existiendo en la BD para auditoría histórica.

---

## Tests: estrategia de dos niveles

### Tests unitarios (`@ExtendWith(MockitoExtension.class)`)

- No levantan Spring
- No usan BD
- Son muy rápidos (milisegundos)
- Usan `@Mock` para simular dependencias
- Prueban la lógica de negocio en aislamiento

```java
// Mockito simula el comportamiento del repositorio:
given(clientRepository.existsByNif("12345678A")).willReturn(false);
// El service no sabe que es un mock; se comporta como si fuera real
```

### Tests de integración (`@SpringBootTest`)

- Levantan el contexto completo de Spring
- Usan H2 en memoria (perfil `dev`)
- Son más lentos (segundos)
- Usan `MockMvc` para simular peticiones HTTP completas
- Prueban que todos los componentes funcionan juntos

```java
mockMvc.perform(post("/api/v1/clients")
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.nif").value("99999999Z"));
```

La combinación de ambos tipos da la máxima confianza: los unitarios son rápidos para desarrollo continuo, los de integración verifican el sistema completo.

---

## Docker: por qué multi-stage

```
Stage 1 (builder):           Stage 2 (runtime):
────────────────────         ──────────────────────
eclipse-temurin:21-jdk       eclipse-temurin:21-jre
+ Maven (~350MB)             Solo el JAR (~200MB total)
+ código fuente
= ~600MB                     = ~200MB ✅
```

El Dockerfile copia solo el JAR compilado al stage final, descartando todo lo demás. Resultado: imagen 3x más pequeña.

### Caching de capas Docker

```dockerfile
COPY pom.xml .              # ← Capa 1: solo el pom
RUN ./mvnw dependency:go-offline  # ← Capa 2: descarga dependencias
COPY src ./src              # ← Capa 3: código fuente
RUN ./mvnw package          # ← Capa 4: compilación
```

Si solo cambia el código fuente (la mayoría de los cambios), Docker reutiliza las capas 1 y 2 (descarga de dependencias ya cacheada). Solo recompila la capa 4. Esto puede ahorrar 2-3 minutos en cada build.

---

## docker-compose: servicios y redes

```
┌─────────────────────────────────────────┐
│          workshop-network               │
│  ┌──────────────┐   ┌────────────────┐  │
│  │   workshop-db│   │  workshop-api  │  │
│  │  PostgreSQL  │◄──│  Spring Boot   │  │
│  │   :5432      │   │    :8080       │  │
│  └──────────────┘   └────────────────┘  │
└─────────────────────────────────────────┘
       ↑                      ↑
  Acceso local           Acceso público
  (herramientas BD)      (navegador/frontend)
```

Los servicios se comunican por nombre (`db`, `api`) dentro de la red interna Docker. Solo el puerto 8080 de la API se expone al exterior.

`depends_on` + `healthcheck` garantiza que la API no intenta conectarse a la BD hasta que PostgreSQL esté completamente listo y aceptando conexiones.

---

## Referencia rápida de endpoints

### Autenticación
```
POST /api/v1/auth/login      → {accessToken, refreshToken, role}
POST /api/v1/auth/refresh    → {accessToken, refreshToken, role}
POST /api/v1/auth/logout     → 204
POST /api/v1/auth/register   → {accessToken, refreshToken, role} [ADMIN]
```

### Clientes
```
POST   /api/v1/clients           [ADMIN, MECHANIC]
GET    /api/v1/clients           [ADMIN, MECHANIC]
GET    /api/v1/clients/{id}      [ADMIN, MECHANIC, CLIENT*]
GET    /api/v1/clients/search?surname1=García  [ADMIN, MECHANIC]
GET    /api/v1/clients/by-nif/{nif}  [ADMIN, MECHANIC]
PUT    /api/v1/clients/{id}      [ADMIN, MECHANIC]
DELETE /api/v1/clients/{id}      [ADMIN]
```

### Mecánicos
```
POST   /api/v1/mechanics         [ADMIN]
GET    /api/v1/mechanics         [ADMIN, MECHANIC]
GET    /api/v1/mechanics/{id}    [ADMIN, MECHANIC]
GET    /api/v1/mechanics/search?specialty=Electricidad  [ADMIN, MECHANIC]
PUT    /api/v1/mechanics/{id}    [ADMIN]
DELETE /api/v1/mechanics/{id}    [ADMIN]
```

### Vehículos
```
POST   /api/v1/vehicles                    [ADMIN, MECHANIC]
GET    /api/v1/vehicles                    [ADMIN, MECHANIC]
GET    /api/v1/vehicles/{id}               [ADMIN, MECHANIC, CLIENT*]
GET    /api/v1/vehicles/by-client/{id}     [ADMIN, MECHANIC, CLIENT]
GET    /api/v1/vehicles/by-type?type=CAR   [ADMIN, MECHANIC]
PUT    /api/v1/vehicles/{id}               [ADMIN, MECHANIC]
DELETE /api/v1/vehicles/{id}               [ADMIN]
```

### Tareas de taller
```
POST   /api/v1/tasks                       [ADMIN, MECHANIC]
GET    /api/v1/tasks                       [ADMIN, MECHANIC]
GET    /api/v1/tasks/{id}                  [ADMIN, MECHANIC, CLIENT]
GET    /api/v1/tasks/by-client/{id}        [ADMIN, MECHANIC, CLIENT]
GET    /api/v1/tasks/by-vehicle/{id}       [ADMIN, MECHANIC, CLIENT]
GET    /api/v1/tasks/by-mechanic/{id}      [ADMIN, MECHANIC]
GET    /api/v1/tasks/pending               [ADMIN, MECHANIC]
GET    /api/v1/tasks/unpaid                [ADMIN, MECHANIC]
PUT    /api/v1/tasks/{id}                  [ADMIN, MECHANIC]
PATCH  /api/v1/tasks/{id}/hours            [ADMIN, MECHANIC]
PATCH  /api/v1/tasks/{id}/finish           [ADMIN, MECHANIC]
PATCH  /api/v1/tasks/{id}/pay             [ADMIN]
DELETE /api/v1/tasks/{id}                  [ADMIN]
```

### Reportes
```
GET    /api/v1/reports/summary             [ADMIN, MECHANIC]
```

*CLIENT solo puede acceder a sus propios recursos.

---

## Cómo ejecutar en desarrollo

```bash
# 1. Compilar
mvn clean package -DskipTests

# 2. Arrancar (perfil dev activo por defecto en application.yml)
mvn spring-boot:run

# 3. Acceder a Swagger UI
open http://localhost:8080/swagger-ui.html

# 4. Login en Swagger:
#    POST /api/v1/auth/login con {"username":"admin","password":"password123"}
#    Copiar el accessToken y pegarlo en el botón "Authorize"
```

## Cómo ejecutar en producción

```bash
# 1. Configurar variables de entorno
cp .env.example .env
# Editar .env con valores reales

# 2. Arrancar
docker-compose up -d

# 3. Ver logs
docker-compose logs -f api

# 4. Comprobar estado
curl http://localhost:8080/actuator/health
```

---

## Guía de aprendizaje recomendada

Para sacar el máximo partido a este proyecto, te recomiendo seguir este orden:

1. **Leer** `00-arquitectura-general.md` para tener la visión global
2. **Arrancar** el proyecto en dev y explorar Swagger UI
3. **Hacer login** con el usuario `admin` / `password123` y explorar la API
4. **Leer** el código de `ClientService` y `ClientController` como referencia
5. **Ejecutar** los tests con `mvn test` y ver los reportes de cobertura
6. **Añadir** un nuevo endpoint o funcionalidad siguiendo el mismo patrón
7. **Escribir** un test para tu nueva funcionalidad
8. **Arrancar** con Docker y verificar que funciona en "producción"
