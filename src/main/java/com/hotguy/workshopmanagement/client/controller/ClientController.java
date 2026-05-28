package com.hotguy.workshopmanagement.client.controller;

import com.hotguy.workshopmanagement.client.dto.ClientRequest;
import com.hotguy.workshopmanagement.client.dto.ClientResponse;
import com.hotguy.workshopmanagement.client.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de clientes.
 *
 * <p>
 * Sigue las convenciones REST:
 * <ul>
 * <li>GET para consultas (sin efectos secundarios)</li>
 * <li>POST para crear nuevos recursos</li>
 * <li>PUT para actualizar un recurso completo</li>
 * <li>DELETE para eliminar (lógicamente en este caso)</li>
 * </ul>
 *
 * <p>
 * La autorización se delega al {@link ClientService} mediante
 * {@code @PreAuthorize}.
 * El Controller solo gestiona HTTP: recibe peticiones, valida, delega, devuelve
 * respuesta.
 */
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestión de clientes del taller")
public class ClientController {

    private final ClientService clientService;

    /**
     * Crea un nuevo cliente.
     * Roles: ADMIN, MECHANIC.
     *
     * @param request datos del nuevo cliente (validados automáticamente)
     * @return 201 Created con el cliente creado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear cliente")
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(request));
    }

    /**
     * Obtiene un cliente por su ID.
     * Roles: ADMIN, MECHANIC (cualquier cliente), CLIENT (solo el suyo).
     *
     * @param id identificador del cliente
     * @return 200 OK con el cliente
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener cliente por ID")
    public ResponseEntity<ClientResponse> getClientById(
        @Parameter(description = "ID del cliente") @PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    /**
     * Lista todos los clientes con paginación.
     * Roles: ADMIN, MECHANIC.
     *
     * <p>
     * {@code @PageableDefault}: valores por defecto de paginación si el cliente
     * no especifica parámetros. Equivale a ?page=0&size=20&sort=surname1,asc.
     * El cliente puede sobreescribirlos con query params:
     * ?page=1&size=10&sort=name,desc
     *
     * @param pageable configuración de paginación (inyectada por Spring)
     * @return 200 OK con página de clientes
     */
    @GetMapping
    @Operation(summary = "Listar clientes")
    public ResponseEntity<Page<ClientResponse>> listClients(
        @PageableDefault(size = 20, sort = "surname1") Pageable pageable) {
        return ResponseEntity.ok(clientService.listClients(pageable));
    }

    /**
     * Busca clientes por primer apellido.
     * Roles: ADMIN, MECHANIC.
     *
     * @param surname1 texto a buscar en el primer apellido
     * @param pageable paginación
     * @return página de clientes que coinciden
     */
    @GetMapping("/search")
    @Operation(summary = "Buscar clientes por apellido")
    public ResponseEntity<Page<ClientResponse>> findBySurname(
        @Parameter(description = "Primer apellido a buscar") @RequestParam String surname1,
        @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(clientService.findClientsBySurname(surname1, pageable));
    }

    /**
     * Busca un cliente por NIF.
     * Roles: ADMIN, MECHANIC.
     *
     * @param nif el NIF del cliente
     * @return el cliente encontrado
     */
    @GetMapping("/by-nif/{nif}")
    @Operation(summary = "Buscar cliente por NIF")
    public ResponseEntity<ClientResponse> findByNif(@PathVariable String nif) {
        return ResponseEntity.ok(clientService.findClientByNif(nif));
    }

    /**
     * Actualiza los datos de un cliente.
     * Roles: ADMIN, MECHANIC.
     *
     * @param id      ID del cliente a actualizar
     * @param request nuevos datos
     * @return el cliente actualizado
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cliente")
    public ResponseEntity<ClientResponse> updateClient(
        @PathVariable Long id,
        @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.ok(clientService.updateClient(id, request));
    }

    /**
     * Elimina lógicamente un cliente (soft delete).
     * Roles: ADMIN.
     *
     * @param id ID del cliente a eliminar
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cliente (soft delete)")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
