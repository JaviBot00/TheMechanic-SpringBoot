package com.hotguy.workshopmanagement.vehicle.service;

import com.hotguy.workshopmanagement.auth.model.User;
import com.hotguy.workshopmanagement.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio auxiliar para evaluar permisos de acceso sobre vehículos.
 * Un cliente solo puede ver sus propios vehículos.
 */
@Service("vehicleSecurityService")
@RequiredArgsConstructor
public class VehicleSecurityService {

    private final VehicleRepository vehicleRepository;

    /**
     * Comprueba si el usuario autenticado es el propietario del vehículo dado.
     *
     * @param authentication contexto de autenticación
     * @param vehicleId      ID del vehículo al que se accede
     * @return {@code true} si el usuario es el propietario
     */
    @Transactional(readOnly = true)
    public boolean isOwner(Authentication authentication, Long vehicleId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        String username = authentication.getName(); // viene del JWT, siempre disponible
        return vehicleRepository.existsByIdAndProprietaryUserUsername(vehicleId, username);
    }
}
