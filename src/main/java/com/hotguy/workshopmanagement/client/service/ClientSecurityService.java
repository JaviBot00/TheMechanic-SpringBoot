package com.hotguy.workshopmanagement.client.service;

import com.hotguy.workshopmanagement.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio auxiliar para evaluar permisos de acceso específicos del dominio
 * cliente.
 *
 * <p>
 * Se usa en expresiones {@code @PreAuthorize} para comprobar si el usuario
 * autenticado es el propietario del recurso solicitado:
 *
 * <pre>
 * {@code @PreAuthorize("hasRole('ADMIN') or @clientSecurityService.isOwner(authentication, #id)")}
 * </pre>
 *
 * <p>
 * Spring Security evalúa la expresión SpEL (Spring Expression Language) antes
 * de ejecutar el método. El {@code @} en {@code @clientSecurityService}
 * referencia
 * el bean de Spring con ese nombre.
 */
@Service("clientSecurityService")
@RequiredArgsConstructor
public class ClientSecurityService {

    private final ClientRepository clientRepository;

    /**
     * Comprueba si el usuario autenticado es el cliente con el ID dado.
     *
     * <p>
     * Un cliente solo puede acceder a sus propios datos.
     *
     * @param authentication el contexto de autenticación de Spring Security
     * @param clientId       el ID del cliente al que se intenta acceder
     * @return {@code true} si el usuario autenticado es ese cliente
     */
    @Transactional(readOnly = true)
    public boolean isOwner(Authentication authentication, Long clientId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        String username = authentication.getName();
        return clientRepository.existsByIdAndUserUsername(clientId, username);
    }
}
