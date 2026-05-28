# 02 — Modelo de datos y Flyway

## ¿Qué contiene este fascículo?

Las entidades JPA (las clases Java que se mapean a tablas de la BD), los scripts SQL de Flyway que crean esas tablas, y los datos de prueba para el entorno de desarrollo.

---

## Ficheros entregados

```
fasciculo-2/
├── src/main/java/com/workshopmanagement/
│   ├── common/
│   │   ├── audit/
│   │   │   └── AuditableEntity.java       ← Campos de auditoría compartidos
│   │   └── model/
│   │       └── Person.java                ← Superclase de Client y Mechanic
│   ├── client/model/
│   │   └── Client.java
│   ├── mechanic/model/
│   │   └── Mechanic.java
│   ├── vehicle/model/
│   │   ├── Vehicle.java
│   │   └── VehicleType.java               ← Enum con tarifas
│   ├── task/model/
│   │   └── WorkshopTask.java
│   └── auth/model/
│       ├── User.java                      ← Implementa UserDetails
│       ├── Role.java                      ← Enum de roles
│       └── RefreshToken.java
└── src/main/resources/
    ├── application.yml                    ← Corregido con spring.profiles.active: dev
    └── db/
        ├── migration/
        │   └── V1__init_schema.sql        ← Esquema completo
        └── seed/
            └── R__dev_seed_data.sql       ← Datos de prueba (solo dev)
```

---

## Conceptos clave

### ¿Qué es JPA e Hibernate?

**JPA** (Jakarta Persistence API) es una especificación estándar de Java que define cómo mapear objetos Java a tablas de bases de datos relacionales. Es solo una interfaz, no una implementación.

**Hibernate** es la implementación más popular de JPA. Spring Boot lo incluye automáticamente con `spring-boot-starter-data-jpa`. Cuando escribes una clase Java con anotaciones JPA (`@Entity`, `@Table`, `@Column`...), Hibernate traduce esas anotaciones a SQL.

El flujo es:
```
Clase Java con @Entity → Hibernate → SQL → Base de datos
```

### Anotaciones JPA fundamentales

| Anotación | Significado |
|-----------|-------------|
| `@Entity` | Esta clase representa una tabla en la BD |
| `@Table(name = "clients")` | Nombre de la tabla (por defecto usa el nombre de la clase) |
| `@Id` | Este campo es la clave primaria |
| `@GeneratedValue(strategy = IDENTITY)` | El valor lo genera la BD (autoincremental) |
| `@Column(name = "nif", nullable = false)` | Configuración de la columna |
| `@MappedSuperclass` | Clase padre cuyos campos se heredan, sin tabla propia |

### Relaciones entre entidades

JPA permite modelar las relaciones de la BD en Java:

**@OneToMany / @ManyToOne** (uno a muchos)
```
Un Client tiene muchos Vehicle
Un Vehicle tiene un solo Client (ManyToOne, lado propietario de la FK)
```

```java
// En Client (lado "uno"):
@OneToMany(mappedBy = "proprietary")
private List<Vehicle> vehicles;

// En Vehicle (lado "muchos", tiene la FK client_id):
@ManyToOne
@JoinColumn(name = "client_id")
private Client proprietary;
```

La FK (`client_id`) siempre está en la tabla del lado "muchos" (`vehicles`). El `mappedBy` en `@OneToMany` apunta al nombre del campo en la otra entidad que tiene la FK.

**@OneToOne** (uno a uno)
```
Un User tiene un Client (opcional)
Un Client tiene un User (opcional, mappedBy en este lado)
```

### LAZY vs EAGER loading

Al cargar una entidad de la BD, JPA tiene que decidir si también carga las entidades relacionadas inmediatamente o solo cuando se accede a ellas.

- **`FetchType.EAGER`**: carga la relación inmediatamente (JOIN en el SQL). Puede cargar datos innecesarios.
- **`FetchType.LAZY`** (recomendado): la relación se carga solo cuando el código accede a ella. Mejor rendimiento.

```java
// Al hacer clientRepository.findById(1), NO se hace JOIN con vehicles
@OneToMany(mappedBy = "proprietary", fetch = FetchType.LAZY)
private List<Vehicle> vehicles;

// Solo cuando llamas a client.getVehicles(), Hibernate hace el SELECT
```

### CascadeType

Controla qué operaciones JPA se propagan de la entidad padre a las hijas:

```java
@OneToMany(mappedBy = "proprietary", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Vehicle> vehicles;
```

- `CascadeType.ALL`: si persistes/borras un `Client`, se persisten/borran sus `Vehicle`
- `orphanRemoval = true`: si quitas un `Vehicle` de la lista del cliente, se borra de la BD

### `@SQLRestriction` — Soft delete automático

Esta anotación de Hibernate 6 aplica un filtro SQL a todas las queries de la entidad:

```java
@SQLRestriction("deleted_at IS NULL")
public class Client extends Person { ... }
```

Sin necesitar tocar ninguna query, todas las búsquedas de clientes excluyen automáticamente los borrados. Es como si los registros con `deleted_at != NULL` no existieran para Hibernate.

---

## Decisión: subclases vs enum para vehículos

El proyecto original tenía cuatro subclases de `Vehicle` (`Car`, `Van`, `Truck`, `Motorcycle`). En la versión Spring Boot lo hemos simplificado:

```
Antes:                          Ahora:
Vehicle (abstract)              Vehicle (@Entity)
  ├── Car                         └── VehicleType (enum)
  ├── Van                               MOTORCYCLE(20€/h, 0€)
  ├── Truck                             CAR(25€/h, 0€)
  └── Motorcycle                        VAN(30€/h, 30€)
                                        TRUCK(40€/h, 50€)
```

**Por qué**: en una BD relacional, las jerarquías de herencia son complicadas. JPA ofrece tres estrategias (`SINGLE_TABLE`, `TABLE_PER_CLASS`, `JOINED`) y todas tienen inconvenientes. Como las subclases originales solo diferían en dos números (tarifa y cargo fijo), moverlos a un enum es más limpio, más simple y más mantenible.

---

## Flyway en detalle

### ¿Cómo funciona?

Al arrancar la aplicación, Flyway:
1. Comprueba si existe la tabla `flyway_schema_history` (la crea si no existe)
2. Lee los scripts de `db/migration/` y `db/seed/` (según el perfil)
3. Compara la lista con los registros en `flyway_schema_history`
4. Ejecuta solo los scripts nuevos (los que no están registrados)

### Tipos de scripts

| Prefijo | Tipo | Comportamiento |
|---------|------|----------------|
| `V` | Versioned | Se ejecuta una sola vez. Si se modifica después, Flyway lanza error. |
| `R` | Repeatable | Se re-ejecuta cada vez que cambia el contenido del fichero. |
| `U` | Undo | Revierte una migración V (requiere Flyway Teams). |

Usamos `V` para el esquema (nunca cambia) y `R` para los datos de prueba (podemos actualizarlos libremente en dev).

### Convención de nombres

```
V1__init_schema.sql
│ │  └── Descripción (palabras con guión bajo)
│ └──── Doble guión bajo (obligatorio)
└────── Versión (número o decimal: 1, 1.1, 2...)

R__dev_seed_data.sql
│  └── Descripción
└───── Prefijo R (Repeatable)
```

### Por qué no usar `ddl-auto: create`

Hibernate puede crear las tablas automáticamente con `ddl-auto: create` o `update`. No lo usamos porque:

1. **Sin control**: Hibernate decide el esquema. Con Flyway, nosotros decidimos.
2. **Sin historial**: no hay registro de qué cambios se han hecho y cuándo.
3. **Peligroso en prod**: `ddl-auto: create` borra y recrea las tablas. `update` puede no aplicar todos los cambios necesarios.
4. **No colaborativo**: si dos desarrolladores modifican el mismo modelo, los cambios pueden entrar en conflicto. Con Flyway, los scripts son ficheros en Git.

---

## Auditoría automática

Todas las entidades heredan de `AuditableEntity`, que tiene tres campos gestionados automáticamente:

```java
@CreatedDate
private Instant createdAt;   // Spring lo rellena al hacer save()

@LastModifiedDate
private Instant updatedAt;   // Spring lo actualiza en cada merge()

private Instant deletedAt;   // Lo rellenamos nosotros en el Service (soft delete)
```

Esto funciona gracias a `@EnableJpaAuditing` en la clase principal y `@EntityListeners(AuditingEntityListener.class)` en `AuditableEntity`.

Usamos `Instant` (momento absoluto en UTC) en lugar de `LocalDateTime` (sin zona horaria) para evitar problemas cuando la BD y el servidor están en zonas horarias distintas.

---

## Datos de prueba en producción

Cuando el sistema pase a producción y necesites cargar datos reales iniciales (ej. el usuario admin), crea un script `V2__insert_admin_user.sql` en `db/migration/`. A diferencia del `R__dev_seed_data.sql`, este script se ejecutará una sola vez y queda registrado en el historial de Flyway.

Nunca uses el script seed de dev en producción.

---

## Próximo fascículo

El **Fascículo 3** implementa toda la seguridad: el filtro JWT, los endpoints de login/refresh/logout, y la configuración de Spring Security que protege los endpoints por rol.
