# 03 — Seguridad JWT

## ¿Qué contiene este fascículo?

Todo el sistema de autenticación y autorización: el filtro JWT, la configuración de Spring Security, los endpoints de login/registro/refresh/logout, y el manejo centralizado de excepciones.

---

## Ficheros entregados

```cmd
fasciculo-3/
└── src/main/java/com/hotguy/workshopmanagement/
    ├── config/
    │   ├── JwtProperties.java          ← Lee propiedades JWT del YAML
    │   └── SecurityConfig.java         ← Configuración central de seguridad
    ├── auth/
    │   ├── controller/
    │   │   └── AuthController.java     ← Endpoints /auth/*
    │   ├── service/
    │   │   ├── AuthService.java        ← Lógica de login, registro, refresh, logout
    │   │   ├── JwtService.java         ← Generación y validación de JWT
    │   │   └── UserDetailsServiceImpl.java ← Carga usuarios de la BD
    │   ├── filter/
    │   │   └── JwtAuthenticationFilter.java ← Intercepta cada petición
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   └── RefreshTokenRepository.java
    │   └── dto/
    │       ├── LoginRequest.java
    │       ├── RegisterRequest.java
    │       ├── RefreshRequest.java
    │       └── AuthResponse.java
    └── common/exception/
        ├── ResourceNotFoundException.java
        └── GlobalExceptionHandler.java
```

---

## ¿Qué es JWT?

JWT (JSON Web Token) es un estándar para transmitir información de forma segura entre dos partes. Un JWT tiene tres partes separadas por puntos:

```cmd
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJST0xFX0FETUlOIn0.xK9z...
      HEADER                          PAYLOAD                        SIGNATURE
```

- **Header**: algoritmo de firma (`HS256`) y tipo (`JWT`)
- **Payload**: los datos (claims): username, rol, expiración
- **Signature**: HMAC-SHA256 del header+payload usando la clave secreta

La clave de seguridad es la **firma**: si alguien modifica el payload, la firma deja de ser válida. El servidor solo necesita su clave secreta para verificarlo, sin consultar la BD en cada petición.

### Access Token vs Refresh Token

| | Access Token | Refresh Token |
|---|---|---|
| Formato | JWT (firmado, con datos) | UUID aleatorio (opaco) |
| Duración | 1 hora | 7 días |
| Almacenamiento servidor | No (stateless) | Sí (tabla refresh_tokens) |
| Uso | Autenticar cada petición | Obtener nuevo access token |
| Revocable | No directamente | Sí (marcar como revocado en BD) |

---

## Flujo completo de autenticación

```cmd
┌─────────┐                              ┌─────────┐          ┌────────┐
│ Cliente │                              │   API   │          │   BD   │
└────┬────┘                              └────┬────┘          └───┬────┘
     │                                        │                   │
     │  POST /auth/login {user, pass}         │                   │
     │───────────────────────────────────────>│                   │
     │                                        │  SELECT user      │
     │                                        │──────────────────>│
     │                                        │  user encontrado  │
     │                                        │<──────────────────│
     │                                        │  bcrypt.verify()  │
     │                                        │  INSERT refresh   │
     │                                        │──────────────────>│
     │  200 {accessToken, refreshToken}       │                   │
     │<───────────────────────────────────────│                   │
     │                                        │                   │
     │  GET /api/v1/clients                   │                   │
     │  Authorization: Bearer <accessToken>   │                   │
     │───────────────────────────────────────>│                   │
     │                                        │  JwtFilter valida │
     │                                        │  (sin BD)         │
     │  200 [lista de clientes]               │                   │
     │<───────────────────────────────────────│                   │
     │                                        │                   │
     │  (1 hora después, access token expira) │                   │
     │                                        │                   │
     │  POST /auth/refresh {refreshToken}     │                   │
     │───────────────────────────────────────>│                   │
     │                                        │  SELECT refresh   │
     │                                        │──────────────────>│
     │                                        │  token válido     │
     │                                        │<──────────────────│
     │                                        │  UPDATE revoked   │
     │                                        │  INSERT nuevo     │
     │  200 {nuevo accessToken, refreshToken} │                   │
     │<───────────────────────────────────────│                   │
```

---

## Spring Security: cómo funciona

Spring Security es una cadena de filtros que intercepta cada petición HTTP antes de que llegue al Controller. El orden importa:

```cmd
Petición HTTP
    │
    ▼
┌─────────────────────────────────┐
│  JwtAuthenticationFilter        │  ← Nuestro filtro personalizado
│  Lee el JWT, valida la firma,   │
│  establece el SecurityContext   │
└─────────────┬───────────────────┘
              │
              ▼
┌─────────────────────────────────┐
│  UsernamePasswordAuthFilter     │  ← Filtro estándar de Spring (ya no hace nada
│  (desplazado por el nuestro)    │    porque el SecurityContext ya está relleno)
└─────────────┬───────────────────┘
              │
              ▼
┌─────────────────────────────────┐
│  AuthorizationFilter            │  ← Comprueba si el usuario tiene permiso
│  ¿Tiene el rol necesario?       │    para acceder al endpoint solicitado
└─────────────┬───────────────────┘
              │
              ▼
         Controller
```

### SecurityContext

El `SecurityContextHolder` es un almacenamiento por hilo (ThreadLocal) que Spring Security usa para saber quién es el usuario durante el procesamiento de la petición. Una vez que nuestro filtro JWT establece la autenticación:

```java
SecurityContextHolder.getContext().setAuthentication(authToken);
```

Cualquier componente en la misma petición puede acceder al usuario:

```java
// En un Controller o Service:
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();

// O más cómodo con la anotación:
@AuthenticationPrincipal UserDetails userDetails
```

---

## Configuración de seguridad por URL

En `SecurityConfig.java` definimos qué endpoints son públicos y cuáles requieren roles:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
    .requestMatchers("/api/v1/auth/register").hasRole("ADMIN")
    .requestMatchers("/api/v1/reports/**").hasAnyRole("ADMIN", "MECHANIC")
    .requestMatchers(HttpMethod.PATCH, "/api/v1/tasks/*/pay").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

Para control más granular dentro de un endpoint (ej. un cliente solo puede ver sus propios datos), usamos `@PreAuthorize` en los Controllers, que se activa con `@EnableMethodSecurity`.

---

## BCrypt: por qué hasheamos contraseñas

Nunca se almacena la contraseña en texto plano. BCrypt aplica:

1. **Salt aleatorio**: cada hash es único aunque la contraseña sea la misma
2. **Factor de coste** (por defecto 10): hace el algoritmo lento adrede para dificultar ataques de fuerza bruta
3. **Irreversible**: no se puede recuperar la contraseña original del hash

```cmd
"password123"  →  BCrypt  →  "$2a$10$N9qo8uLOick..."
"password123"  →  BCrypt  →  "$2a$10$Kx7pL3mRick..."  (distinto, mismo input)
```

Para verificar: `passwordEncoder.matches("password123", hashAlmacenado)`

---

## Manejo de excepciones con ProblemDetail

`GlobalExceptionHandler` captura todas las excepciones de los Controllers y devuelve respuestas estructuradas según RFC 9457 (estándar de Spring Boot 3+):

```json
{
  "type": "https://workshopmanagement.com/errors/not-found",
  "title": "Recurso no encontrado",
  "status": 404,
  "detail": "Cliente no encontrado: 42",
  "timestamp": "2026-01-15T10:30:00Z"
}
```

Sin este handler, una excepción no capturada devolvería un error 500 genérico con el stack trace (peligroso en producción, molesto en desarrollo).

### Mapa de excepciones a HTTP

| Excepción | Código HTTP | Cuándo ocurre |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entidad no encontrada por ID |
| `IllegalArgumentException` | 400 | Datos incorrectos (username duplicado...) |
| `IllegalStateException` | 409 | Operación no permitida en el estado actual |
| `MethodArgumentNotValidException` | 400 | Validación Bean Validation fallida |
| `AuthenticationException` | 401 | Credenciales incorrectas |
| `AccessDeniedException` | 403 | Sin permisos suficientes |
| `Exception` (catch-all) | 500 | Error inesperado |

---

## Tokens con `record` de Java

Los DTOs de autenticación usan `record`, una característica de Java 16+:

```java
public record LoginRequest(String username, String password) {}
```

Equivale a una clase con:

- Constructor con todos los campos
- Getters (método con el nombre del campo, sin `get`)
- `equals()`, `hashCode()`, `toString()` generados

Los records son inmutables (los campos son `final`), perfectos para DTOs que solo transportan datos.

---

## Próximo fascículo

El **Fascículo 4** implementa la feature completa de `Client`: Repository, Service, Controller, DTOs y MapStruct mapper, aplicando todos los conceptos de seguridad de este fascículo con `@PreAuthorize`.
