# 08 — Feature: Reportes y estadísticas

## ¿Qué contiene esta feature?

El módulo de reportes agrega datos de múltiples repositorios para generar estadísticas del estado del taller. Es la feature más sencilla en términos de código pero ilustra bien el patrón de queries de agregación con JPQL.

---

## Ficheros de esta feature

```cmd
report/
├── controller/
│   └── ReportController.java
├── service/
│   └── ReportService.java
└── dto/
    └── SummaryReportResponse.java
```

---

## Endpoints disponibles

| Método | URL | Roles | Descripción |
|---|---|---|---|
| `GET` | `/api/v1/reports/summary` | ADMIN, MECHANIC | Resumen general del taller |

---

## El endpoint de resumen

Devuelve una visión global del estado del taller en una sola petición:

```json
{
  "totalClients": 10,
  "totalMechanics": 9,
  "totalVehicles": 8,
  "totalTasks": 8,
  "pendingTasks": 4,
  "totalRevenue": 530.0
}
```

El `ReportService` delega en los repositorios especializados de cada feature:

```java
@Service
@Transactional(readOnly = true)
public class ReportService {

    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public SummaryReportResponse getSummary() {
        return new SummaryReportResponse(
            clientRepository.countActiveClients(),
            mechanicRepository.countActiveMechanics(),
            vehicleRepository.countActiveVehicles(),
            taskRepository.count(),
            taskRepository.countPendingTasks(),
            Optional.ofNullable(taskRepository.sumTotalRevenue()).orElse(0.0)
        );
    }
}
```

El `@Transactional(readOnly = true)` a nivel de clase garantiza que todas las queries del servicio son de solo lectura, permitiendo optimizaciones en la BD.

---

## Queries de agregación en los repositorios

Cada repositorio expone los contadores que necesita el servicio de reportes:

```java
// En ClientRepository:
@Query("SELECT COUNT(c) FROM Client c")
long countActiveClients();
// Hibernate aplica WHERE deleted_at IS NULL automáticamente (@SQLRestriction)

// En WorkshopTaskRepository:
@Query("SELECT COUNT(t) FROM WorkshopTask t WHERE t.finished = false")
long countPendingTasks();

// Suma de facturación: accede a propiedades del enum directamente en JPQL
@Query("""
    SELECT SUM(t.realHours * v.type.hourlyRate + v.type.fixedFee)
    FROM WorkshopTask t
    JOIN t.vehicle v
    WHERE t.paid = true
    """)
Double sumTotalRevenue();
```

La query `sumTotalRevenue` devuelve `Double` (no `double`) porque puede ser `null` si no hay tareas pagadas. El service lo gestiona con `Optional.ofNullable(...).orElse(0.0)`.

---

## Por qué @Transactional(readOnly = true)

Cuando una transacción se marca como `readOnly`:

1. La BD puede usar réplicas de lectura (en arquitecturas con read replicas)
2. Hibernate no hace flush del contexto de persistencia antes de las queries (no comprueba si hay cambios pendientes que escribir)
3. El pool de conexiones puede optimizar la conexión

Es una buena práctica marcar siempre `readOnly = true` en métodos o clases que solo lean datos.

---

## Cómo añadir un nuevo informe

El patrón para añadir un nuevo endpoint de reporte es siempre el mismo:

**1. Añadir la query al repositorio correspondiente:**

```java
// En WorkshopTaskRepository:
@Query("SELECT t.mechanic.id, COUNT(t), SUM(t.realHours) " +
       "FROM WorkshopTask t GROUP BY t.mechanic.id")
List<Object[]> getMechanicWorkloadRaw();
```

**2. Crear el DTO de respuesta:**

```java
public record MechanicWorkloadResponse(
    Long mechanicId,
    String mechanicName,
    long taskCount,
    float totalHours
) {}
```

**3. Añadir el método al servicio:**

```java
@PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
public List<MechanicWorkloadResponse> getMechanicWorkload() {
    // procesar y mapear los resultados
}
```

**4. Exponer el endpoint en el controller:**

```java
@GetMapping("/mechanic-workload")
public ResponseEntity<List<MechanicWorkloadResponse>> getMechanicWorkload() {
    return ResponseEntity.ok(reportService.getMechanicWorkload());
}
```

---

## Ideas para reportes adicionales

Con la infraestructura actual, estos reportes son fáciles de añadir:

| Reporte | Query base |
|---|---|
| Carga de trabajo por mecánico | `GROUP BY mechanic_id` con COUNT y SUM de horas |
| Vehículos más problemáticos | `GROUP BY vehicle_id` con COUNT de tareas |
| Ingresos por mes | `GROUP BY MONTH(init_date)` con SUM de costes |
| Tareas por tipo de vehículo | `GROUP BY vehicle.type` |
| Tiempo medio por especialidad | AVG de realHours agrupado por `mechanic.specialty` |
| Clientes con más vehículos | JOIN y `GROUP BY client_id` con COUNT |

---

## Seguridad del módulo de reportes

La URL `/api/v1/reports/**` está protegida en `SecurityConfig` con `hasAnyRole('ADMIN','MECHANIC')`. Además, cada método del service tiene `@PreAuthorize` por doble seguridad (defensa en profundidad).

Los `CLIENT` no tienen acceso a los reportes porque contienen información agregada de todos los clientes, mecánicos y vehículos del taller.

---

## SummaryReportResponse como record

```java
public record SummaryReportResponse(
    long totalClients,
    long totalMechanics,
    long totalVehicles,
    long totalTasks,
    long pendingTasks,
    double totalRevenue
) {}
```

Un `record` es perfecto aquí: es inmutable, no tiene lógica, solo transporta datos de la capa de servicio a la capa HTTP. Jackson lo serializa automáticamente a JSON sin ninguna configuración adicional.
