package com.hotguy.workshopmanagement.client;

import com.hotguy.workshopmanagement.client.dto.ClientRequest;
import com.hotguy.workshopmanagement.client.dto.ClientResponse;
import com.hotguy.workshopmanagement.client.mapper.ClientMapper;
import com.hotguy.workshopmanagement.client.model.Client;
import com.hotguy.workshopmanagement.client.repository.ClientRepository;
import com.hotguy.workshopmanagement.client.service.ClientService;
import com.hotguy.workshopmanagement.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

/**
 * Tests unitarios para {@link ClientService}.
 *
 * <p>
 * Usamos AssertJ (incluido en spring-boot-starter-test) por su API fluida
 * y sus mensajes de error más claros que los de JUnit estándar.
 *
 * <p>
 * Patrón BDD (Given-When-Then):
 * <ul>
 * <li>Given: configuramos el estado inicial y los mocks</li>
 * <li>When: ejecutamos la acción bajo test</li>
 * <li>Then: verificamos el resultado</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService - Tests unitarios")
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientService clientService;

    private Client testClient;
    private ClientRequest testRequest;
    private ClientResponse testResponse;

    @BeforeEach
    void setUp() {
        testClient = Client.builder()
            .id(1L)
            .clientCode(100)
            .name("Juan")
            .surname1("García")
            .surname2("López")
            .nif("12345678A")
            .email("juan@test.com")
            .telephone("600000000")
            .build();

        testRequest = new ClientRequest(
            100, "Juan", "García", "López",
            "12345678A", "juan@test.com", "600000000");

        testResponse = new ClientResponse(
            1L, 100, "Juan", "García", "López",
            "12345678A", "juan@test.com", "600000000",
            0, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("Debe crear un cliente cuando el NIF y código son únicos")
    void shouldCreateClientWhenNifAndCodeAreUnique() {
        // Given
        given(clientRepository.existsByNif("12345678A")).willReturn(false);
        given(clientRepository.existsByClientCode(100)).willReturn(false);
        given(clientMapper.toEntity(testRequest)).willReturn(testClient);
        given(clientRepository.save(testClient)).willReturn(testClient);
        given(clientMapper.toResponse(testClient)).willReturn(testResponse);

        // When
        ClientResponse result = clientService.createClient(testRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.nif()).isEqualTo("12345678A");
        then(clientRepository).should().save(testClient);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el NIF ya existe")
    void shouldThrowExceptionWhenNifAlreadyExists() {
        // Given
        given(clientRepository.existsByNif("12345678A")).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> clientService.createClient(testRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("12345678A");

        then(clientRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el código de cliente ya existe")
    void shouldThrowExceptionWhenClientCodeAlreadyExists() {
        // Given
        given(clientRepository.existsByNif("12345678A")).willReturn(false);
        given(clientRepository.existsByClientCode(100)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> clientService.createClient(testRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("100");
    }

    @Test
    @DisplayName("Debe devolver un cliente por ID si existe")
    void shouldReturnClientById() {
        // Given
        given(clientRepository.findById(1L)).willReturn(Optional.of(testClient));
        given(clientMapper.toResponse(testClient)).willReturn(testResponse);

        // When
        ClientResponse result = clientService.getClientById(1L);

        // Then
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el cliente no existe")
    void shouldThrowResourceNotFoundWhenClientDoesNotExist() {
        // Given
        given(clientRepository.findById(99L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> clientService.getClientById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Debe listar clientes con paginación")
    void shouldListClientsWithPagination() {
        // Given
        Page<Client> clientPage = new PageImpl<>(List.of(testClient));
        given(clientRepository.findAll(any(Pageable.class))).willReturn(clientPage);
        given(clientMapper.toResponse(testClient)).willReturn(testResponse);

        // When
        Page<ClientResponse> result = clientService.listClients(Pageable.ofSize(20));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).nif()).isEqualTo("12345678A");
    }

    @Test
    @DisplayName("Debe realizar soft delete del cliente")
    void shouldSoftDeleteClient() {
        // Given
        given(clientRepository.findById(1L)).willReturn(Optional.of(testClient));
        given(clientRepository.save(testClient)).willReturn(testClient);

        // When
        clientService.deleteClient(1L);

        // Then: el cliente debe tener deletedAt relleno
        assertThat(testClient.getDeletedAt()).isNotNull();
        then(clientRepository).should().save(testClient);
    }

    @Test
    @DisplayName("Debe actualizar los datos del cliente")
    void shouldUpdateClientData() {
        // Given
        given(clientRepository.findById(1L)).willReturn(Optional.of(testClient));
//        given(clientRepository.existsByNif("12345678A")).willReturn(false);
        given(clientRepository.save(testClient)).willReturn(testClient);
        given(clientMapper.toResponse(testClient)).willReturn(testResponse);
        willDoNothing().given(clientMapper).updateEntityFromRequest(testRequest, testClient);

        // When
        ClientResponse result = clientService.updateClient(1L, testRequest);

        // Then
        assertThat(result).isNotNull();
        then(clientMapper).should().updateEntityFromRequest(testRequest, testClient);
    }
}
