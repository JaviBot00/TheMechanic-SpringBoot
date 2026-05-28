# 01 — Base del proyecto

## ¿Qué contiene esta feature?

Este es el punto de partida del proyecto. No hay lógica de negocio todavía, pero sí toda la **infraestructura** sobre la que construiremos el resto: el fichero de dependencias, la clase de arranque, y la configuración por entornos.

Piénsalo como los cimientos de un edificio: si están bien hechos, todo lo que viene después es más fácil.

---

## Ficheros de esta feature

```cmd
fasciculo-1/
├── pom.xml
├── README.md
├── .gitignore
├── .env.example
└── src/
    └── main/
        ├── java/com/hotguy/workshopmanagement/
        │   └── WorkshopManagementApplication.java
        └── resources/
            ├── application.yml
            ├── application-dev.yml
            └── application-prod.yml
```

---

## Conceptos clave

### ¿Qué es Spring Boot?

Spring Boot es un framework Java que te permite crear aplicaciones web listas para producción con muy poca configuración manual. La idea central es **convención sobre configuración**: si sigues las convenciones del framework, él configura automáticamente todo lo que necesitas (servidor web, conexión a BD, seguridad, etc.).

El flujo básico es:

1. Defines tus dependencias en `pom.xml`
2. Spring Boot detecta qué tienes en el classpath y se auto-configura
3. Tú solo escribes la lógica de negocio

### ¿Qué es Maven?

Maven es la herramienta que gestiona:

- **Dependencias**: descarga automáticamente las librerías que necesitas desde Maven Central (el repositorio público de librerías Java)
- **Build**: compila el código, ejecuta los tests, y empaqueta la aplicación en un `.jar`
- **Plugins**: ejecuta tareas adicionales como generar reportes de cobertura

El fichero `pom.xml` (Project Object Model) es el corazón de Maven. Define quién eres (las coordenadas del proyecto) y qué necesitas.

---

## pom.xml explicado

### Parent BOM

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.6</version>
</parent>
```

El **BOM** (Bill of Materials) es como una hoja de precios que Spring Boot mantiene para todas sus dependencias. Al declarar este `parent`, heredamos la gestión automática de versiones: no necesitamos especificar versiones para `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, etc. Spring Boot garantiza que todas son compatibles entre sí.

### Starters

Los **starters** son dependencias especiales de Spring Boot que agrupan todo lo necesario para una funcionalidad. Por ejemplo:

- `spring-boot-starter-web` trae Spring MVC + Tomcat embebido + Jackson
- `spring-boot-starter-data-jpa` trae Spring Data JPA + Hibernate + gestión de transacciones
- `spring-boot-starter-security` trae Spring Security con toda su infraestructura

El patrón `spring-boot-starter-*` indica que es un starter oficial.

### Scopes de dependencias

| Scope | ¿Cuándo se usa? | ¿Va en el JAR final? |
|---|---|---|
| `compile` (default) | Necesaria para compilar y ejecutar | ✅ Sí |
| `runtime` | Solo necesaria en ejecución (no para compilar) | ✅ Sí |
| `test` | Solo para tests | ❌ No |
| `provided` | La proporciona el contenedor | ❌ No |
| `optional` | No se hereda como transitiva | Depende |

Por eso `postgresql` y `h2` van en `runtime`: el código Java no importa clases directamente del driver, solo usa las interfaces JDBC estándar.

### MapStruct + Lombok: el orden importa

```xml
<annotationProcessorPaths>
    <!-- Lombok PRIMERO -->
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </path>
    <!-- MapStruct DESPUÉS -->
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
    </path>
</annotationProcessorPaths>
```

Ambas librerías son **procesadores de anotaciones**: generan código Java en tiempo de compilación. El problema es que MapStruct necesita los getters y setters de Lombok para generar el código de mapeo, y Lombok los genera antes de que MapStruct entre en acción. Si el orden se invierte, MapStruct no encuentra los accessors y la compilación falla.

---

## WorkshopManagementApplication.java explicado

```java
@SpringBootApplication
@EnableJpaAuditing
public class WorkshopManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkshopManagementApplication.class, args);
    }
}
```

### `@SpringBootApplication`

Es un atajo para tres anotaciones:

1. **`@SpringBootConfiguration`**: dice "esta clase puede definir beans de Spring" (equivalente a `@Configuration`)
2. **`@EnableAutoConfiguration`**: activa la magia de Spring Boot. Analiza el classpath y configura automáticamente lo que encuentra. Si ve `spring-boot-starter-web`, configura Tomcat y Spring MVC. Si ve `spring-boot-starter-data-jpa`, configura Hibernate y Spring Data.
3. **`@ComponentScan`**: escanea el paquete actual y todos los subpaquetes en busca de clases anotadas con `@Component`, `@Service`, `@Repository`, `@Controller`, etc., y las registra como beans en el contenedor de Spring.

### `@EnableJpaAuditing`

Activa el sistema de auditoría automática de Spring Data JPA. Cuando una entidad tiene campos como `createdAt` o `updatedAt` con la anotación `@CreatedDate` o `@LastModifiedDate`, Spring los rellena automáticamente sin que el desarrollador tenga que escribir código para ello.

---

## Los ficheros de configuración YAML

### ¿Por qué YAML en lugar de properties?

Spring Boot admite tanto `application.properties` como `application.yml`. YAML (Yet Another Markup Language) es más legible cuando hay configuración anidada:

```yaml
# YAML - más legible
spring:
  jpa:
    hibernate:
      ddl-auto: validate

# Properties - más verboso
spring.jpa.hibernate.ddl-auto=validate
```

### El sistema de perfiles

Spring Boot permite tener configuraciones distintas para cada entorno mediante **perfiles**:

```cmd
application.yml          ← Base común (siempre se carga)
application-dev.yml      ← Sobreescribe/añade propiedades para dev
application-prod.yml     ← Sobreescribe/añade propiedades para prod
```

El perfil activo se indica con:

```bash
# Variable de entorno
SPRING_PROFILES_ACTIVE=dev

# Argumento JVM
-Dspring.profiles.active=dev

# Argumento de la aplicación
--spring.profiles.active=dev
```

Spring carga el `application.yml` base y **encima** carga el fichero del perfil activo. Las propiedades del perfil sobreescriben las del base si están duplicadas.

### Propiedades personalizadas con `${VARIABLE:valor_default}`

En el `application-prod.yml` encontrarás esta sintaxis:

```yaml
datasource:
  url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/workshopdb}
```

Esto significa: "usa el valor de la variable de entorno `SPRING_DATASOURCE_URL`, y si no está definida, usa `jdbc:postgresql://localhost:5432/workshopdb` como valor por defecto". Así el fichero funciona tanto en local como en un servidor donde las variables están definidas.

---

## Propiedades importantes explicadas

### `spring.jpa.hibernate.ddl-auto: validate`

Hibernate puede gestionar el esquema de la BD de varias formas:

| Valor | Comportamiento |
|---|---|
| `none` | No hace nada con el esquema |
| `validate` | Comprueba que el esquema coincide con las entidades, falla si no |
| `update` | Modifica el esquema para que coincida (peligroso en prod) |
| `create` | Crea el esquema desde cero al arrancar |
| `create-drop` | Crea al arrancar y borra al parar |

Usamos `validate` porque **Flyway es el responsable del esquema**. Hibernate solo comprueba que lo que Flyway ha creado coincide con nuestras entidades Java. Si hay discrepancia, la aplicación no arranca y nos avisa del problema.

### `spring.jackson.default-property-inclusion: non_null`

Jackson es la librería que convierte objetos Java a JSON. Con `non_null`, los campos que son `null` no aparecen en el JSON de respuesta. Esto reduce el tamaño de las respuestas y hace la API más limpia.

### `spring.flyway.locations`

Define dónde Flyway busca los scripts de migración:

- En `dev`: `classpath:db/migration` + `classpath:db/seed` (incluye datos de prueba)
- En `prod`: solo `classpath:db/migration` (solo el esquema)

### HikariCP

HikariCP es el **pool de conexiones** de Spring Boot (incluido por defecto). Un pool de conexiones mantiene un conjunto de conexiones abiertas a la BD, reutilizándolas en lugar de abrir y cerrar una conexión por cada request. Esto mejora enormemente el rendimiento porque abrir una conexión de BD es una operación costosa.

---

## El fichero .env.example

Este fichero cumple dos propósitos:

1. **Documentación**: muestra qué variables de entorno necesita la aplicación
2. **Plantilla**: los desarrolladores hacen `cp .env.example .env` y rellenan sus valores

El fichero `.env` real nunca se sube al repositorio (está en `.gitignore`) porque puede contener contraseñas y claves secretas.

---

## Descripción del repositorio (para GitHub)

```cmd
API REST para gestión de taller mecánico | Spring Boot 4 · Java 21 · JWT · PostgreSQL · Docker
```

---

## Próximo fascículo

El **Fascículo 2** construirá sobre esta base para definir el modelo de datos: las entidades JPA (las clases Java que representan tablas en la BD) y los scripts Flyway que crearán esas tablas.
