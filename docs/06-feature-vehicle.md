# 06 — Feature: Vehicle

## ¿Qué contiene esta feature?

Gestión de vehículos registrados en el taller, incluyendo la lógica de tarifas por tipo, cambio de propietario, y el control de acceso de clientes a sus propios vehículos.

---

## Ficheros de esta feature

```cmd
vehicle/
├── controller/
│   └── VehicleController.java
├── service/
│   ├── VehicleService.java
│   └── VehicleSecurityService.java   ← Control de acceso por propietario
├── repository/
│   └── VehicleRepository.java
├── mapper/
│   └── VehicleMapper.java
└── dto/
    ├── VehicleRequest.java
    └── VehicleResponse.java
```

---

## Endpoints disponibles

| Método | URL | Roles | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/vehicles` | ADMIN, MECHANIC | Registrar vehículo |
| `GET` | `/api/v1/vehicles` | ADMIN, MECHANIC | Listar todos (paginado) |
| `GET` | `/api/v1/vehicles/{id}` | ADMIN, MECHANIC, CLIENT* | Obtener por ID |
| `GET` | `/api/v1/vehicles/by-client/{clientId}` | ADMIN, MECHANIC, CLIENT | Ver vehículos de un cliente |
| `GET` | `/api/v1/vehicles/by-type?type=CAR` | ADMIN, MECHANIC | Filtrar por tipo |
| `PUT` | `/api/v1/vehicles/{id}` | ADMIN, MECHANIC | Actualizar (incluye cambio de propietario) |
| `DELETE` | `/api/v1/vehicles/{id}` | ADMIN | Eliminar (soft delete) |

*CLIENT solo puede ver sus propios vehículos.

---

## El enum VehicleType como tabla de tarifas

En el proyecto original había cuatro subclases de `Vehicle` (Car, Van, Truck, Motorcycle). En Spring Boot lo simplificamos con un enum que lleva la lógica de precios integrada:

```java
public enum VehicleType {
    MOTORCYCLE(20f, 0f,  "Motocicleta"),
    CAR       (25f, 0f,  "Coche"),
    VAN       (30f, 30f, "Furgoneta"),
    TRUCK     (40f, 50f, "Camión");

    private final float hourlyRate;
    private final float fixedFee;

    public float calculatePrice(float hours) {
        if (hours < 0) throw new IllegalArgumentException("...");
        return (hours * hourlyRate) + fixedFee;
    }
}
```

El `Vehicle` delega el cálculo al enum:

```java
public float calculatePrice(float hours) {
    return type.calculatePrice(hours);  // Delega al enum
}
```

Esto elimina la necesidad de una jerarquía de herencia en la BD (que en JPA siempre es complicada) manteniendo la misma lógica de negocio.

---

## Registro de vehículo: vinculación con el propietario

Al crear un vehículo, se vincula inmediatamente al cliente propietario:

```java
public VehicleResponse createVehicle(VehicleRequest request) {
    // 1. Comprobar matrícula única
    if (vehicleRepository.existsByRegistrationCode(request.registrationCode())) {
        throw new IllegalArgumentException("Ya existe un vehículo con la matrícula: ...");
    }

    // 2. Obtener el cliente propietario
    Client client = clientRepository.findById(request.clientId())
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: ..."));

    // 3. Crear el vehículo y vincularlo
    Vehicle vehicle = vehicleMapper.toEntity(request);
    client.addVehicle(vehicle);  // Establece la FK en ambos lados de la relación

    return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
}
```

El método `client.addVehicle(vehicle)` mantiene la consistencia bidireccional: añade el vehículo a la lista del cliente Y establece `vehicle.proprietary = client`.

---

## Cambio de propietario en la actualización

Si el `clientId` del request es diferente al propietario actual, el servicio gestiona el traspaso:

```java
if (!vehicle.getProprietary().getId().equals(request.clientId())) {
    Client newOwner = clientRepository.findById(request.clientId())
            .orElseThrow(...);

    // Desvincular del propietario anterior
    vehicle.getProprietary().removeVehicle(vehicle);

    // Vincular al nuevo propietario
    newOwner.addVehicle(vehicle);
}
```

---

## VehicleResponse: datos enriquecidos

La respuesta incluye campos calculados para que el frontend no necesite hacer cálculos ni peticiones adicionales:

```java
@Mapping(target = "hourlyRate",    expression = "java(vehicle.getType().getHourlyRate())")
@Mapping(target = "fixedFee",      expression = "java(vehicle.getType().getFixedFee())")
@Mapping(target = "clientId",      expression = "java(vehicle.getProprietary().getId())")
@Mapping(target = "clientName",    expression = "java(vehicle.getProprietary().getName() + ' ' + vehicle.getProprietary().getSurname1())")
@Mapping(target = "taskCount",     expression = "java(vehicle.getWorkshopTasks().size())")
@Mapping(target = "completionPct", expression = "java(vehicle.getCompletionPercentage())")
@Mapping(target = "totalRevenue",  expression = "java(vehicle.getTotalRevenue())")
VehicleResponse toResponse(Vehicle vehicle);
```

Así la respuesta JSON incluye directamente la tarifa, el nombre del propietario, cuántas tareas tiene y cuánto ha facturado el vehículo históricamente.

---

## VehicleSecurityService: el cliente ve solo sus vehículos

Un `CLIENT` puede ver un vehículo solo si le pertenece:

```java
@Service("vehicleSecurityService")
public class VehicleSecurityService {

    public boolean isOwner(Authentication authentication, Long vehicleId) {
        if (principal instanceof User user && user.getClient() != null) {
            return user.getClient().getVehicles().stream()
                    .anyMatch(v -> v.getId().equals(vehicleId));
        }
        return false;
    }
}
```

Usado en `@PreAuthorize`:

```java
@PreAuthorize(
    "hasAnyRole('ADMIN','MECHANIC') or " +
    "(hasRole('CLIENT') and @vehicleSecurityService.isOwner(authentication, #id))"
)
public VehicleResponse getVehicleById(Long id) { ... }
```

---

## Restricción de eliminación

Un vehículo no puede eliminarse si tiene tareas activas (igual que el mecánico):

```java
public void deleteVehicle(Long id) {
    Vehicle vehicle = findVehicleOrThrow(id);

    long activeTasks = vehicle.getWorkshopTasks().stream()
            .filter(t -> !t.isFinished())
            .count();

    if (activeTasks > 0) {
        throw new IllegalStateException(
            "No se puede eliminar: el vehículo tiene " + activeTasks + " tarea(s) activa(s)"
        );
    }

    vehicle.softDelete();
    vehicleRepository.save(vehicle);
}
```

---

## Almacenamiento del enum en BD

```java
@Enumerated(EnumType.STRING)
@Column(name = "type", nullable = false, length = 20)
private VehicleType type;
```

`EnumType.STRING` almacena el nombre del enum (`"CAR"`, `"VAN"`) en lugar del ordinal numérico (`0`, `1`). Ventajas:

- La BD es legible directamente
- Reordenar el enum no rompe los datos existentes
- Los checks de la BD (`CHECK (type IN ('CAR','VAN',...))`) son legibles

---

## Errores específicos de esta feature

| Situación | Código | Detalle |
|---|---|---|
| Matrícula duplicada | 400 | "Ya existe un vehículo con la matrícula: ..." |
| Cliente propietario no existe | 404 | "Cliente no encontrado: {id}" |
| Vehículo no encontrado | 404 | "Vehículo no encontrado: {id}" |
| Eliminar con tareas activas | 409 | "No se puede eliminar: el vehículo tiene N tarea(s) activa(s)" |

---

## Próximo fascículo

El **Fascículo 07** documenta la feature `WorkshopTask`, la más compleja del sistema: gestiona el ciclo de vida completo de una reparación, desde el diagnóstico hasta el cobro.
