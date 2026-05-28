# 07 — Feature: WorkshopTask

## ¿Qué contiene esta feature?

La orden de trabajo es la entidad central del sistema. Gestiona el ciclo de vida completo de una reparación: creación con diagnóstico, acumulación de horas trabajadas, finalización con solución, y cobro al cliente. Es la feature con más lógica de negocio y más endpoints.

---

## Ficheros de esta feature

```cmd
task/
├── controller/
│   └── WorkshopTaskController.java
├── service/
│   └── WorkshopTaskService.java
├── repository/
│   └── WorkshopTaskRepository.java
├── mapper/
│   └── WorkshopTaskMapper.java
└── dto/
    ├── WorkshopTaskRequest.java    ← Crear/actualizar tarea
    ├── WorkshopTaskResponse.java   ← Respuesta completa
    └── AddHoursRequest.java        ← Solo para añadir horas
```

---

## Endpoints disponibles

| Método | URL | Roles | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/tasks` | ADMIN, MECHANIC | Crear tarea |
| `GET` | `/api/v1/tasks` | ADMIN, MECHANIC | Listar todas (paginado) |
| `GET` | `/api/v1/tasks/{id}` | ADMIN, MECHANIC, CLIENT | Ver tarea |
| `GET` | `/api/v1/tasks/by-client/{id}` | ADMIN, MECHANIC, CLIENT | Tareas de un cliente |
| `GET` | `/api/v1/tasks/by-vehicle/{id}` | ADMIN, MECHANIC, CLIENT | Tareas de un vehículo |
| `GET` | `/api/v1/tasks/by-mechanic/{id}` | ADMIN, MECHANIC | Tareas de un mecánico |
| `GET` | `/api/v1/tasks/pending` | ADMIN, MECHANIC | Tareas no finalizadas |
| `GET` | `/api/v1/tasks/unpaid` | ADMIN, MECHANIC | Finalizadas sin pagar |
| `PUT` | `/api/v1/tasks/{id}` | ADMIN, MECHANIC | Actualizar diagnóstico/notas |
| `PATCH` | `/api/v1/tasks/{id}/hours` | ADMIN, MECHANIC | Añadir horas trabajadas |
| `PATCH` | `/api/v1/tasks/{id}/finish` | ADMIN, MECHANIC | Marcar como finalizada |
| `PATCH` | `/api/v1/tasks/{id}/pay` | ADMIN | Marcar como pagada |
| `DELETE` | `/api/v1/tasks/{id}` | ADMIN | Eliminar (solo si no pagada) |

---

## El ciclo de vida de una tarea

```cmd
CREACIÓN
    │
    ▼
┌─────────────────────────────────────────────┐
│  Estado: "Pendiente"                        │
│  realHours = 0                              │
│  finished = false, paid = false             │
│  totalCost = 0 (no finalizada)              │
└─────────────────────────────────────────────┘
    │  PATCH /tasks/{id}/hours
    ▼
┌─────────────────────────────────────────────┐
│  Estado: "En progreso"                      │
│  realHours > 0                              │
│  Se puede llamar N veces (acumula horas)    │
│  progress = (realHours / previewHours) × 100│
└─────────────────────────────────────────────┘
    │  PATCH /tasks/{id}/finish
    ▼
┌─────────────────────────────────────────────┐
│  Estado: "Finalizada"                       │
│  finished = true                            │
│  totalCost = vehicle.calculatePrice(realHours)│
│  Ya NO se pueden añadir más horas           │
└─────────────────────────────────────────────┘
    │  PATCH /tasks/{id}/pay  [solo ADMIN]
    ▼
┌─────────────────────────────────────────────┐
│  Estado: "Pagada"                           │
│  paid = true                                │
│  Estado final: no se puede modificar más    │
└─────────────────────────────────────────────┘
```

Las transiciones son irreversibles (excepto `markAsUnpaid` para corregir errores bancarios). Si se intenta una transición inválida, el modelo lanza una excepción.

---

## Creación de una tarea

Al crear una tarea, el servicio:

1. Obtiene el vehículo por ID → obtiene el cliente propietario automáticamente
2. Obtiene el mecánico por ID
3. Construye la entidad con las referencias correctas

```java
public WorkshopTaskResponse createTask(WorkshopTaskRequest request) {
    Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado: ..."));
    Mechanic mechanic = mechanicRepository.findById(request.mechanicId())
            .orElseThrow(() -> new ResourceNotFoundException("Mecánico no encontrado: ..."));

    // El cliente se obtiene del vehículo, no hace falta enviarlo en el request
    Client client = vehicle.getProprietary();

    WorkshopTask task = taskMapper.toEntity(request);
    task.setVehicle(vehicle);
    task.setMechanic(mechanic);
    task.setClient(client);      // Desnormalizado para facilitar búsquedas
    task.setRealHours(0f);
    task.setFinished(false);
    task.setPaid(false);

    return taskMapper.toResponse(taskRepository.save(task));
}
```

El `client` se almacena directamente en la tarea (desnormalización) para permitir búsquedas eficientes por cliente sin hacer joins adicionales a través de `Vehicle`.

---

## Cálculo de costes

El coste se calcula dinámicamente, nunca se almacena en BD. Así siempre es coherente aunque cambien las tarifas:

```java
// En WorkshopTask:
public float getTotalCost() {
    if (!finished) return 0f;              // Gratis si no está finalizada
    return vehicle.calculatePrice(realHours);  // Delega al vehículo → al enum
}

public float getEstimatedCost() {
    return vehicle.calculatePrice(previewHours);  // Presupuesto inicial
}

public float getProgress() {
    if (previewHours <= 0) return 0f;
    return Math.min((realHours / previewHours) * 100f, 100f);  // Máximo 100%
}
```

Ejemplo para un `VAN` con 3 horas reales:

```cmd
totalCost = 3h × 30€/h + 30€ fijo = 120€
```

---

## El mapper: campos calculados en la respuesta

```java
@Mapping(target = "progress",      expression = "java(task.getProgress())")
@Mapping(target = "status",        expression = "java(task.getStatus())")
@Mapping(target = "estimatedCost", expression = "java(task.getEstimatedCost())")
@Mapping(target = "totalCost",     expression = "java(task.getTotalCost())")
@Mapping(target = "clientId",      expression = "java(task.getClient().getId())")
@Mapping(target = "clientName",    expression = "java(task.getClient().getName() + ' ' + task.getClient().getSurname1())")
@Mapping(target = "vehicleId",     expression = "java(task.getVehicle().getId())")
@Mapping(target = "vehicleReg",    expression = "java(task.getVehicle().getRegistrationCode())")
@Mapping(target = "mechanicId",    expression = "java(task.getMechanic().getId())")
@Mapping(target = "mechanicName",  expression = "java(task.getMechanic().getName() + ' ' + task.getMechanic().getSurname1())")
WorkshopTaskResponse toResponse(WorkshopTask task);
```

La respuesta es completa: incluye IDs y nombres de cliente, vehículo y mecánico, más todos los campos calculados (progreso, estado, costes). El frontend no necesita hacer joins ni peticiones adicionales.

---

## Endpoints PATCH vs PUT

Usamos `PATCH` para operaciones de **cambio de estado parcial** y `PUT` para **actualización de datos**:

- `PATCH /tasks/{id}/hours` → solo añade horas (acción de negocio específica)
- `PATCH /tasks/{id}/finish` → solo finaliza (transición de estado)
- `PATCH /tasks/{id}/pay` → solo marca como pagada (transición de estado)
- `PUT /tasks/{id}` → actualiza diagnóstico, notas, horas estimadas

Esta distinción sigue las convenciones REST: `PUT` reemplaza representación, `PATCH` aplica cambio parcial.

---

## Queries especializadas del repositorio

```java
// Tareas pendientes de finalizar (para la cola de trabajo)
Page<WorkshopTask> findByFinishedFalse(Pageable pageable);

// Tareas finalizadas pendientes de cobro (para facturación)
Page<WorkshopTask> findByFinishedTrueAndPaidFalse(Pageable pageable);

// Contar tareas pendientes (para el dashboard)
@Query("SELECT COUNT(t) FROM WorkshopTask t WHERE t.finished = false")
long countPendingTasks();

// Calcular facturación total (para reportes)
@Query("SELECT SUM(t.realHours * v.type.hourlyRate + v.type.fixedFee) " +
       "FROM WorkshopTask t JOIN t.vehicle v WHERE t.paid = true")
Double sumTotalRevenue();
```

La query `sumTotalRevenue` usa JPQL para acceder a la propiedad del enum (`v.type.hourlyRate`) directamente en la query. Hibernate la traduce al SQL correspondiente con los valores del enum.

---

## Restricción de eliminación

Solo se puede eliminar una tarea que no haya sido pagada:

```java
public void deleteTask(Long id) {
    WorkshopTask task = findTaskOrThrow(id);
    if (task.isPaid()) {
        throw new IllegalStateException(
            "No se puede eliminar una tarea ya pagada"
        );
    }
    taskRepository.delete(task);  // Hard delete (no soft delete en tareas)
}
```

Las tareas pagadas son registros contables y no deben borrarse nunca. Las no pagadas pueden eliminarse por errores de entrada o cancelaciones.

---

## Errores específicos de esta feature

| Situación | Código | Detalle |
|---|---|---|
| Vehículo no encontrado | 404 | "Vehículo no encontrado: {id}" |
| Mecánico no encontrado | 404 | "Mecánico no encontrado: {id}" |
| Tarea no encontrada | 404 | "Tarea no encontrada: {id}" |
| Añadir horas a tarea finalizada | 409 | "No se pueden añadir horas a una tarea finalizada" |
| Añadir horas negativas o cero | 400 | "Las horas deben ser mayores que cero" |
| Pagar tarea no finalizada | 409 | "No se puede cobrar una tarea no finalizada" |
| Eliminar tarea pagada | 409 | "No se puede eliminar una tarea ya pagada" |

---

## Próximo fascículo

El **Fascículo 08** documenta la feature `Reports`, que agrega datos de múltiples repositorios para generar estadísticas e informes del estado del taller.
