# 05 — Feature: Mechanic

## ¿Qué contiene esta feature?

Gestión completa de mecánicos del taller: repositorio, servicio con lógica de negocio, controlador HTTP, DTOs y mapper. Sigue el mismo patrón que la feature `Client` con una diferencia importante: **un mecánico no puede eliminarse si tiene tareas activas asignadas**.

---

## Ficheros de esta feature

```cmd
mechanic/
├── controller/
│   └── MechanicController.java
├── service/
│   └── MechanicService.java
├── repository/
│   └── MechanicRepository.java
├── mapper/
│   └── MechanicMapper.java
└── dto/
    ├── MechanicRequest.java
    └── MechanicResponse.java
```

---

## Endpoints disponibles

| Método | URL | Roles | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/mechanics` | ADMIN | Registrar mecánico |
| `GET` | `/api/v1/mechanics` | ADMIN, MECHANIC | Listar todos (paginado) |
| `GET` | `/api/v1/mechanics/{id}` | ADMIN, MECHANIC | Obtener por ID |
| `GET` | `/api/v1/mechanics/search?specialty=` | ADMIN, MECHANIC | Buscar por especialidad |
| `PUT` | `/api/v1/mechanics/{id}` | ADMIN | Actualizar |
| `DELETE` | `/api/v1/mechanics/{id}` | ADMIN | Eliminar (soft delete) |

---

## Diferencias respecto a Client

### 1. Restricción de eliminación

Un mecánico no puede eliminarse si tiene tareas activas (no finalizadas). Esto protege la integridad referencial de negocio: no tiene sentido borrar a alguien que está trabajando en reparaciones.

```java
public void deleteMechanic(Long id) {
    Mechanic mechanic = findMechanicOrThrow(id);

    // Contar tareas activas (no finalizadas)
    long activeTasks = mechanic.getWorkshopTasks().stream()
            .filter(t -> !t.isFinished())
            .count();

    if (activeTasks > 0) {
        throw new IllegalStateException(
            "No se puede eliminar el mecánico: tiene "
            + activeTasks + " tarea(s) activa(s) asignada(s)"
        );
    }

    mechanic.softDelete();
    mechanicRepository.save(mechanic);
}
```

`IllegalStateException` → `GlobalExceptionHandler` → **409 Conflict**.

### 2. Registro por fecha

El campo `registrationDate` es un `LocalDate` (solo fecha, sin hora) validado con `@PastOrPresent`. Un mecánico no puede tener fecha de alta en el futuro.

```java
@PastOrPresent(message = "La fecha de registro no puede ser futura")
LocalDate registrationDate
```

### 3. Roles más restrictivos

Crear y eliminar mecánicos es solo para `ADMIN`. Un `MECHANIC` puede consultar y buscar, pero no crear ni eliminar compañeros.

```java
@PreAuthorize("hasRole('ADMIN')")
public MechanicResponse createMechanic(MechanicRequest request) { ... }

@PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
public Page<MechanicResponse> listMechanics(Pageable pageable) { ... }
```

---

## MechanicResponse: el campo taskCount

La respuesta incluye el número de tareas asignadas al mecánico. Es útil para el frontend mostrar la carga de trabajo sin hacer una petición adicional.

```java
public record MechanicResponse(
    Long id,
    String name,
    String surname1,
    String surname2,
    String nif,
    String email,
    String telephone,
    LocalDate registrationDate,
    String specialty,
    int taskCount,         // Número de tareas asignadas actualmente
    Instant createdAt,
    Instant updatedAt
) {}
```

El mapper lo calcula con una expresión:

```java
@Mapping(
    target = "taskCount",
    expression = "java(mechanic.getWorkshopTasks().size())"
)
MechanicResponse toResponse(Mechanic mechanic);
```

---

## Búsqueda por especialidad

El endpoint `GET /api/v1/mechanics/search?specialty=Electricidad` usa búsqueda parcial insensible a mayúsculas:

```java
// Spring genera:
// SELECT * FROM mechanics WHERE LOWER(specialty) LIKE LOWER('%electricidad%')
// AND deleted_at IS NULL
Page<Mechanic> findBySpecialtyContainingIgnoreCase(String specialty, Pageable pageable);
```

Así `?specialty=elec` devuelve mecánicos con especialidad "Electricidad", "Electrónica", etc.

---

## Errores específicos de esta feature

| Situación | Código | Detalle |
|---|---|---|
| NIF duplicado | 400 | "Ya existe un mecánico con el NIF: ..." |
| Mecánico no encontrado | 404 | "Mecánico no encontrado: {id}" |
| Eliminar con tareas activas | 409 | "No se puede eliminar el mecánico: tiene N tarea(s) activa(s)" |
| Fecha de registro futura | 400 | Error de validación en `registrationDate` |

---

## Próximo fascículo

El **Fascículo 06** documenta la feature `Vehicle`, que introduce la lógica de tipos de vehículo con tarifas diferenciadas y la gestión de propietarios.
