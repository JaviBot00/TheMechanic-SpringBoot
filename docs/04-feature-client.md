# 04 — Feature: Client

## ¿Qué contiene esta feature?

Todo lo necesario para gestionar clientes del taller a través de la API REST: repositorio, servicio con lógica de negocio, controlador HTTP, DTOs de entrada/salida, mapper MapStruct y control de acceso por rol.

---

## Ficheros de esta feature

```cmd
client/
├── controller/
│   └── ClientController.java       ← Endpoints REST /api/v1/clients
├── service/
│   ├── ClientService.java           ← Lógica de negocio + @PreAuthorize
│   └── ClientSecurityService.java   ← Evaluación de permisos por propietario
├── repository/
│   └── ClientRepository.java        ← Acceso a BD (Spring Data JPA)
├── mapper/
│   └── ClientMapper.java            ← Conversión entidad ↔ DTO (MapStruct)
└── dto/
    ├── ClientRequest.java           ← DTO de entrada (crear/actualizar)
    └── ClientResponse.java          ← DTO de salida (respuesta JSON)
```

---

## Endpoints disponibles

| Método | URL | Roles | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/clients` | ADMIN, MECHANIC | Crear cliente |
| `GET` | `/api/v1/clients` | ADMIN, MECHANIC | Listar todos (paginado) |
| `GET` | `/api/v1/clients/{id}` | ADMIN, MECHANIC, CLIENT* | Obtener por ID |
| `GET` | `/api/v1/clients/search?surname1=` | ADMIN, MECHANIC | Buscar por apellido |
| `GET` | `/api/v1/clients/by-nif/{nif}` | ADMIN, MECHANIC | Buscar por NIF |
| `PUT` | `/api/v1/clients/{id}` | ADMIN, MECHANIC | Actualizar |
| `DELETE` | `/api/v1/clients/{id}` | ADMIN | Eliminar (soft delete) |

*Un CLIENT solo puede acceder a sus propios datos.

---

## El flujo completo de una petición

Tomamos como ejemplo `POST /api/v1/clients`:

```cmd
1. Petición HTTP llega con JSON + Bearer Token

2. JwtAuthenticationFilter
   → Extrae el token del header "Authorization: Bearer xxx"
   → Valida la firma con la clave secreta
   → Establece el usuario en el SecurityContext

3. SecurityConfig
   → Comprueba que el endpoint permite el método POST
   → El endpoint requiere autenticación (anyRequest().authenticated())

4. ClientController.createClient()
   → @Valid valida el ClientRequest (NIF formato correcto, campos no vacíos...)
   → Si falla la validación → 400 Bad Request automático
   → Delega en ClientService

5. ClientService.createClient()
   → @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')") evalúa el rol
   → Si no tiene rol → 403 Forbidden
   → Comprueba que el NIF no existe (clientRepository.existsByNif)
   → Si existe → IllegalArgumentException → GlobalExceptionHandler → 400
   → ClientMapper.toEntity(request) convierte DTO a entidad
   → clientRepository.save(entity) persiste en BD
   → ClientMapper.toResponse(entity) convierte entidad a DTO de respuesta

6. ClientController devuelve ResponseEntity.status(201).body(clientResponse)

7. Jackson serializa ClientResponse a JSON
   → campos null se omiten (default-property-inclusion: non_null)

8. Respuesta 201 Created con el cliente creado en el body
```

---

## ClientRequest: validaciones Bean Validation

```java
public record ClientRequest(
    @NotNull @Positive
    Integer clientCode,          // Obligatorio, debe ser positivo

    @NotBlank @Size(max = 100)
    String name,                 // Obligatorio, máximo 100 caracteres

    @NotBlank @Size(max = 100)
    String surname1,

    @Size(max = 100)
    String surname2,             // Opcional (sin @NotBlank)

    @NotBlank
    @Pattern(regexp = "^[0-9]{8}[A-Z]$")
    String nif,                  // Formato NIF español: 8 dígitos + letra

    @NotBlank @Email
    String email,

    @Size(max = 20)
    String telephone             // Opcional
) {}
```

Si alguna validación falla, Spring devuelve automáticamente:

```json
{
  "title": "Error de validación",
  "status": 400,
  "errors": {
    "nif": "El NIF debe tener 8 dígitos y una letra mayúscula",
    "email": "El formato del email no es válido"
  }
}
```

Sin necesidad de escribir ningún código de validación manual.

---

## ClientResponse: qué se expone y qué no

La entidad `Client` tiene campos internos que no deben salir en la respuesta JSON: `deletedAt`, la colección completa de vehículos (causaría referencias circulares), o la relación con `User` (que tiene la contraseña hasheada).

El `ClientResponse` expone solo lo necesario:

```java
public record ClientResponse(
    Long id,
    Integer clientCode,
    String name,
    String surname1,
    String surname2,
    String nif,
    String email,
    String telephone,
    int vehicleCount,      // Solo el número, no la lista completa
    Instant createdAt,
    Instant updatedAt
    // deletedAt: NO expuesto intencionalmente
    // user: NO expuesto (contiene credenciales)
    // vehicles: NO expuesto (sería una lista enorme con referencias circulares)
) {}
```

---

## ClientMapper: MapStruct en detalle

### Conversión DTO → Entidad (para crear)

```java
@Mapping(target = "id", ignore = true)          // Lo genera la BD
@Mapping(target = "vehicles", ignore = true)     // Lista vacía por defecto
@Mapping(target = "user", ignore = true)         // Se vincula aparte
@Mapping(target = "createdAt", ignore = true)    // Lo gestiona @CreatedDate
@Mapping(target = "updatedAt", ignore = true)    // Lo gestiona @LastModifiedDate
@Mapping(target = "deletedAt", ignore = true)    // Null hasta que se borre
Client toEntity(ClientRequest request);
```

### Conversión Entidad → DTO (para responder)

```java
@Mapping(
    target = "vehicleCount",
    expression = "java(client.getVehicles().size())"
)
ClientResponse toResponse(Client client);
```

La directiva `expression` permite ejecutar código Java arbitrario cuando el campo no tiene un mapeo directo.

### Actualización parcial (para PUT)

```java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void updateEntityFromRequest(ClientRequest request, @MappingTarget Client client);
```

Con `IGNORE`: los campos `null` en el request no sobreescriben los valores actuales de la entidad. Si el cliente envía solo `{"email": "nuevo@email.com"}` y el resto `null`, solo se actualiza el email.

`@MappingTarget` indica que `client` es el objeto que se modifica, no se crea uno nuevo.

---

## ClientRepository: Spring Data en detalle

```java
// Spring genera: SELECT * FROM clients WHERE nif = ? AND deleted_at IS NULL
Optional<Client> findByNif(String nif);

// SELECT COUNT(*) > 0 FROM clients WHERE nif = ? AND deleted_at IS NULL
boolean existsByNif(String nif);

// SELECT * FROM clients WHERE LOWER(surname1) LIKE LOWER('%?%') AND deleted_at IS NULL
// LIMIT ? OFFSET ?
Page<Client> findBySurname1ContainingIgnoreCase(String surname1, Pageable pageable);
```

El filtro `AND deleted_at IS NULL` lo añade Hibernate automáticamente gracias a `@SQLRestriction` en la entidad. No hay que escribirlo en ninguna query.

---

## ClientSecurityService: control de acceso por propietario

Un `CLIENT` solo puede ver sus propios datos. Esto no se puede expresar solo con roles, necesitamos lógica adicional:

```java
@Service("clientSecurityService")
public class ClientSecurityService {

    public boolean isOwner(Authentication authentication, Long clientId) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getClient() != null
                && user.getClient().getId().equals(clientId);
        }
        return false;
    }
}
```

Se usa en `@PreAuthorize` del service:

```java
@PreAuthorize(
    "hasAnyRole('ADMIN','MECHANIC') or " +
    "(hasRole('CLIENT') and @clientSecurityService.isOwner(authentication, #id))"
)
public ClientResponse getClientById(Long id) { ... }
```

Spring evalúa la expresión **antes** de entrar al método. Si es `false`, lanza `AccessDeniedException` → `GlobalExceptionHandler` → 403 Forbidden.

El `#id` en la expresión SpEL referencia el parámetro `id` del método anotado.

---

## Soft delete: cómo se implementa

```java
// En ClientService:
public void deleteClient(Long id) {
    Client client = findClientOrThrow(id);  // 404 si no existe
    client.softDelete();                     // rellena deletedAt = Instant.now()
    clientRepository.save(client);           // persiste el cambio en BD
}
```

```java
// En AuditableEntity (heredado por Client):
public void softDelete() {
    this.deletedAt = Instant.now();
}
```

Después del soft delete, cualquier `findById`, `findByNif`, `findAll`... excluye automáticamente ese cliente porque Hibernate aplica `WHERE deleted_at IS NULL` en todas las queries.

---

## Paginación: cómo funciona

El endpoint `GET /api/v1/clients` acepta query params opcionales:

```cmd
/api/v1/clients                          → página 0, 20 por página, orden por surname1
/api/v1/clients?page=1&size=10           → página 1, 10 por página
/api/v1/clients?sort=name,asc            → ordenado por nombre ascendente
/api/v1/clients?page=0&size=5&sort=clientCode,desc
```

La respuesta incluye metadatos:

```json
{
  "content": [
    { "id": 1, "name": "Ana", ... },
    { "id": 2, "name": "Carlos", ... }
  ],
  "totalElements": 47,
  "totalPages": 10,
  "size": 5,
  "number": 0,
  "first": true,
  "last": false,
  "sort": { "sorted": true, "direction": "DESC" }
}
```

El `@PageableDefault(size = 20, sort = "surname1")` en el Controller define los valores por defecto cuando el cliente no especifica parámetros.

---

## Errores posibles y sus códigos HTTP

| Situación | Código | Respuesta |
|---|---|---|
| NIF ya existe | 400 | `{"title": "Petición incorrecta", "detail": "Ya existe un cliente con el NIF: 12345678A"}` |
| Código de cliente duplicado | 400 | Ídem |
| NIF con formato inválido | 400 | `{"title": "Error de validación", "errors": {"nif": "..."}}` |
| Cliente no encontrado | 404 | `{"title": "Recurso no encontrado", "detail": "Cliente no encontrado: 99"}` |
| Sin token JWT | 401 | `{"title": "No autenticado"}` |
| Rol insuficiente | 403 | `{"title": "Acceso denegado"}` |

---

## Próximo fascículo

El **Fascículo 05** documenta la feature `Mechanic`, que sigue exactamente el mismo patrón pero con lógica adicional: no se puede eliminar un mecánico si tiene tareas activas asignadas.
