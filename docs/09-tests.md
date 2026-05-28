# 09 — Tests: estrategia y guía de escritura

## Estructura de los tests

```
src/test/java/com/workshopmanagement/
├── auth/
│   ├── JwtServiceTest.java                  ← Unitario: generación/validación JWT
│   └── AuthControllerIntegrationTest.java   ← Integración: flujo login completo
├── client/
│   ├── ClientServiceTest.java               ← Unitario: lógica de negocio
│   └── ClientControllerIntegrationTest.java ← Integración: endpoints HTTP + seguridad
├── task/
│   └── WorkshopTaskModelTest.java           ← Unitario: modelo y lógica de tarea
└── vehicle/
    └── VehicleTypeTest.java                 ← Unitario: cálculo de tarifas (parametrizado)
```

---

## Tipos de tests y cuándo usar cada uno

### Tests unitarios
- **Velocidad**: milisegundos
- **Aislamiento**: completo (no Spring, no BD)
- **Uso**: lógica de negocio, cálculos, reglas de dominio
- **Anotación clave**: `@ExtendWith(MockitoExtension.class)`

### Tests de integración
- **Velocidad**: segundos (levanta Spring + H2)
- **Aislamiento**: ninguno (prueba el sistema completo)
- **Uso**: endpoints HTTP, seguridad JWT, flujos completos
- **Anotación clave**: `@SpringBootTest` + `@AutoConfigureMockMvc`

### Regla general
Escribe más tests unitarios que de integración (pirámide de tests).
Los unitarios son baratos; los de integración son caros pero necesarios.

---

## Cómo ejecutar los tests

```bash
# Todos los tests
mvn test

# Solo tests unitarios (excluir los de integración por convención)
mvn test -Dtest="*Test"

# Un test específico
mvn test -Dtest="ClientServiceTest"

# Con reporte de cobertura JaCoCo
mvn verify
open target/site/jacoco/index.html
```

---

## Cómo escribir un test nuevo

### Test unitario (patrón Given-When-Then)

```java
@Test
@DisplayName("Descripción clara de lo que se prueba")
void methodName_Condition_ExpectedResult() {
    // GIVEN: estado inicial
    given(repository.findById(1L)).willReturn(Optional.of(entity));

    // WHEN: ejecutar la acción
    ResponseDto result = service.getById(1L);

    // THEN: verificar el resultado
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(1L);
}
```

### Test de integración

```java
@Test
@DisplayName("Descripción")
void endpointTest() throws Exception {
    mockMvc.perform(get("/api/v1/clients/1")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
}
```

---

## Mockito: principales métodos

```java
// Simular comportamiento
given(mock.metodo(arg)).willReturn(valor);
given(mock.metodo(any())).willThrow(new RuntimeException());

// Verificar que se llamó
then(mock).should().metodo(arg);
then(mock).should(never()).metodo(any());
then(mock).should(times(2)).metodo(any());

// Matchers para argumentos
any()                    // cualquier valor
any(ClaseEspecifica.class)
eq("valorExacto")
anyString()
```

---

## AssertJ: principales aserciones

```java
// Valores básicos
assertThat(valor).isEqualTo(esperado);
assertThat(valor).isNotNull();
assertThat(valor).isNull();
assertThat(valor).isTrue();
assertThat(numero).isZero();
assertThat(numero).isPositive();

// Colecciones
assertThat(lista).hasSize(3);
assertThat(lista).isEmpty();
assertThat(lista).contains("elemento");
assertThat(lista).extracting("campo").containsExactly("a", "b");

// Excepciones
assertThatThrownBy(() -> servicio.metodo())
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("texto esperado");

// Strings
assertThat(texto).startsWith("prefijo");
assertThat(texto).isNotBlank();
```
