package com.hotguy.workshopmanagement.vehicle.service;

import com.hotguy.workshopmanagement.auth.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Servicio auxiliar para evaluar permisos de acceso sobre vehículos.
 * Un cliente solo puede ver sus propios vehículos.
 */
@Service("vehicleSecurityService")
public class VehicleSecurityService {

    /**
     * Comprueba si el usuario autenticado es el propietario del vehículo dado.
     *
     * @param authentication contexto de autenticación
     * @param vehicleId      ID del vehículo al que se accede
     * @return {@code true} si el usuario es el propietario
     */
    public boolean isOwner(Authentication authentication, Long vehicleId) {
        if (authentication == null || !authentication.isAuthenticated())
            return false;
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user && user.getClient() != null) {
            return user.getClient().getVehicles().stream()
                .anyMatch(v -> v.getId().equals(vehicleId));
        }
        return false;
    }
}
