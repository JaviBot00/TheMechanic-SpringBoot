package com.hotguy.workshopmanagement.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotguy.workshopmanagement.auth.dto.LoginRequest;
import com.hotguy.workshopmanagement.auth.model.Role;
import com.hotguy.workshopmanagement.auth.model.User;
import com.hotguy.workshopmanagement.auth.repository.UserRepository;
import com.hotguy.workshopmanagement.client.dto.ClientRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para el flujo completo de gestión de clientes.
 *
 * <p>
 * Verifica la cadena completa: seguridad JWT → Controller → Service →
 * Repository → H2.
 * Cada test obtiene primero un token JWT y lo usa en las peticiones.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@DisplayName("ClientController - Tests de integración")
class ClientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String clientToken;

    @BeforeEach
    void setUp() throws Exception {
        // Crear usuario ADMIN
        userRepository.save(User.builder()
            .username("admin_test")
            .password(passwordEncoder.encode("admin123"))
            .role(Role.ADMIN)
            .enabled(true)
            .build());

        // Crear usuario CLIENT
        userRepository.save(User.builder()
            .username("client_test")
            .password(passwordEncoder.encode("client123"))
            .role(Role.CLIENT)
            .enabled(true)
            .build());

        adminToken = obtenerToken("admin_test", "admin123");
        clientToken = obtenerToken("client_test", "client123");
    }

    @Test
    @DisplayName("ADMIN puede crear un cliente")
    void adminCanCreateClient() throws Exception {
        ClientRequest request = new ClientRequest(
            999, "Pedro", "Martínez", "Ruiz",
            "99999999Z", "pedro@test.com", "611111111");

        mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nif").value("99999999Z"))
            .andExpect(jsonPath("$.name").value("Pedro"));
    }

    @Test
    @DisplayName("CLIENT no puede crear clientes")
    void clientCannotCreateClient() throws Exception {
        ClientRequest request = new ClientRequest(
            998, "Ana", "López", "García",
            "88888888Y", "ana@test.com", "622222222");

        mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Crear cliente con NIF inválido debe devolver 400")
    void createClientWithInvalidNif_shouldReturn400() throws Exception {
        ClientRequest request = new ClientRequest(
            997, "Test", "Test", null,
            "nif-invalido", "test@test.com", null);

        mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.nif").exists());
    }

    @Test
    @DisplayName("Listar clientes requiere autenticación")
    void listClients_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN puede listar todos los clientes con paginación")
    void adminCanListClients() throws Exception {
        mockMvc.perform(get("/api/v1/clients?page=0&size=10")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("Obtener cliente inexistente debe devolver 404")
    void getNonExistentClient_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/clients/99999")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    // =========================================================================
    // MÉTODO AUXILIAR
    // =========================================================================

    /**
     * Obtiene un token JWT para el usuario dado realizando una petición de login.
     * Reutilizado en todos los tests que necesitan autenticación.
     */
    private String obtenerToken(String username, String password) throws Exception {
        LoginRequest loginReq = new LoginRequest(username, password);
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
