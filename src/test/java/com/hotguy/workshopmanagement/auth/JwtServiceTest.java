package com.hotguy.workshopmanagement.auth;

import com.hotguy.workshopmanagement.auth.model.Role;
import com.hotguy.workshopmanagement.auth.model.User;
import com.hotguy.workshopmanagement.auth.service.JwtService;
import com.hotguy.workshopmanagement.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests unitarios para {@link JwtService}.
 *
 * <p>
 * {@code @ExtendWith(MockitoExtension.class)}: activa la integración de Mockito
 * con JUnit 5. Mockito crea automáticamente los mocks anotados con
 * {@code @Mock}
 * e inyecta los necesarios en la clase bajo test ({@code @InjectMocks}).
 *
 * <p>
 * Estos tests son "unitarios puros": no levantan el contexto de Spring,
 * no usan base de datos. Son rápidos y aislados.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService - Tests unitarios")
class JwtServiceTest {

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Configurar las propiedades JWT para los tests
        given(jwtProperties.getSecretKey())
                .willReturn("test-secret-key-that-is-long-enough-for-hs256-algorithm");
        given(jwtProperties.getAccessTokenExpiration())
                .willReturn(3600000L); // 1 hora

        // Crear un usuario de prueba
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("hashedpassword")
                .role(Role.CLIENT)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Debe generar un token JWT no vacío")
    void shouldGenerateNonEmptyToken() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Debe extraer el username correcto del token")
    void shouldExtractCorrectUsername() {
        String token = jwtService.generateAccessToken(testUser);
        String extracted = jwtService.extractUsername(token);
        assertThat(extracted).isEqualTo("testuser");
    }

    @Test
    @DisplayName("El token debe ser válido para el usuario que lo generó")
    void shouldBeValidForCorrectUser() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("El token no debe ser válido para un usuario diferente")
    void shouldNotBeValidForDifferentUser() {
        String token = jwtService.generateAccessToken(testUser);

        User otherUser = User.builder()
                .username("otheruser")
                .role(Role.MECHANIC)
                .enabled(true)
                .build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("Un token expirado debe detectarse como expirado")
    void shouldDetectExpiredToken() {
        // Generar un token con expiración en el pasado (-1 ms)
        given(jwtProperties.getAccessTokenExpiration()).willReturn(-1L);
        String expiredToken = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.isTokenExpired(expiredToken)).isTrue();
    }

    @Test
    @DisplayName("Un token válido no debe detectarse como expirado")
    void shouldNotDetectValidTokenAsExpired() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }
}
