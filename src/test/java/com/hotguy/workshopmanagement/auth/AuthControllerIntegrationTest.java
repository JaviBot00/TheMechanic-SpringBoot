package com.hotguy.workshopmanagement.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotguy.workshopmanagement.auth.dto.LoginRequest;
import com.hotguy.workshopmanagement.auth.model.Role;
import com.hotguy.workshopmanagement.auth.model.User;
import com.hotguy.workshopmanagement.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para el flujo de autenticación JWT.
 *
 * <p>
 * {@code @SpringBootTest}: levanta el contexto completo de Spring Boot
 * (incluyendo BD H2, seguridad, etc.). Es más lento que los tests unitarios
 * pero verifica que todos los componentes funcionan juntos.
 *
 * <p>
 * {@code @AutoConfigureMockMvc}: configura {@link MockMvc} para simular
 * peticiones HTTP sin levantar un servidor real. Más rápido que un servidor
 * real, pero verifica filtros, serialización JSON y validaciones.
 *
 * <p>
 * {@code @ActiveProfiles("dev")}: usa la configuración del perfil dev
 * (H2 en memoria), evitando dependencias de PostgreSQL en los tests.
 *
 * <p>
 * {@code @Transactional}: cada test se ejecuta en una transacción que
 * se revierte al finalizar, dejando la BD limpia para el siguiente test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@DisplayName("AuthController - Tests de integración")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Crear un usuario de prueba antes de cada test
        User user = User.builder()
                .username("testuser")
                .password(passwordEncoder.encode("password123"))
                .role(Role.CLIENT)
                .enabled(true)
                .build();
        userRepository.save(user);
    }

    @Test
    @DisplayName("Login con credenciales correctas debe devolver 200 con tokens")
    void loginWithValidCredentials_shouldReturn200WithTokens() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("ROLE_CLIENT"));
    }

    @Test
    @DisplayName("Login con contraseña incorrecta debe devolver 401")
    void loginWithWrongPassword_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login con usuario inexistente debe devolver 401")
    void loginWithUnknownUser_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("nobody", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login con username vacío debe devolver 400")
    void loginWithBlankUsername_shouldReturn400() throws Exception {
        LoginRequest request = new LoginRequest("", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    @DisplayName("Acceder a endpoint protegido sin token debe devolver 401")
    void accessProtectedEndpointWithoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Registro de usuario sin rol ADMIN debe devolver 403")
    void registerWithoutAdminRole_shouldReturn403() throws Exception {
        // Obtener token de usuario CLIENT
        LoginRequest loginReq = new LoginRequest("testuser", "password123");
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(response).get("accessToken").asText();

        // Intentar registrar con token de CLIENT
        mockMvc.perform(post("/api/v1/auth/register")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nuevo\",\"password\":\"pass1234\",\"role\":\"CLIENT\"}"))
                .andExpect(status().isForbidden());
    }
}
