package com.hotguy.workshopmanagement.vehicle.service;

import com.hotguy.workshopmanagement.client.model.Client;
import com.hotguy.workshopmanagement.client.repository.ClientRepository;
import com.hotguy.workshopmanagement.common.exception.ResourceNotFoundException;
import com.hotguy.workshopmanagement.vehicle.dto.VehicleRequest;
import com.hotguy.workshopmanagement.vehicle.dto.VehicleResponse;
import com.hotguy.workshopmanagement.vehicle.mapper.VehicleMapper;
import com.hotguy.workshopmanagement.vehicle.model.Vehicle;
import com.hotguy.workshopmanagement.vehicle.model.VehicleType;
import com.hotguy.workshopmanagement.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio que gestiona la lógica de negocio de los vehículos.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final ClientRepository clientRepository;
    private final VehicleMapper vehicleMapper;

    /**
     * Registra un vehículo y lo vincula al cliente propietario.
     */
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public VehicleResponse createVehicle(VehicleRequest request) {
        if (vehicleRepository.existsByRegistrationCode(request.registrationCode())) {
            throw new IllegalArgumentException("Ya existe un vehículo con la matrícula: " + request.registrationCode());
        }
        Client client = clientRepository.findById(request.clientId())
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + request.clientId()));

        Vehicle vehicle = vehicleMapper.toEntity(request);
        client.addVehicle(vehicle);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC') or " +
        "(hasRole('CLIENT') and @vehicleSecurityService.isOwner(authentication, #id))")
    public VehicleResponse getVehicleById(Long id) {
        return vehicleMapper.toResponse(findVehicleOrThrow(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public Page<VehicleResponse> listVehicles(Pageable pageable) {
        return vehicleRepository.findAll(pageable).map(vehicleMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC') or hasRole('CLIENT')")
    public Page<VehicleResponse> listVehiclesByClient(Long clientId, Pageable pageable) {
        return vehicleRepository.findByProprietaryId(clientId, pageable).map(vehicleMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public Page<VehicleResponse> listVehiclesByType(VehicleType type, Pageable pageable) {
        return vehicleRepository.findByType(type, pageable).map(vehicleMapper::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public VehicleResponse updateVehicle(Long id, VehicleRequest request) {
        Vehicle vehicle = findVehicleOrThrow(id);
        if (!vehicle.getRegistrationCode().equals(request.registrationCode())
            && vehicleRepository.existsByRegistrationCode(request.registrationCode())) {
            throw new IllegalArgumentException("La matrícula ya está en uso: " + request.registrationCode());
        }
        // Cambiar propietario si ha cambiado el clientId
        if (!vehicle.getProprietary().getId().equals(request.clientId())) {
            Client newOwner = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + request.clientId()));
            vehicle.getProprietary().removeVehicle(vehicle);
            newOwner.addVehicle(vehicle);
        }
        vehicleMapper.updateEntityFromRequest(request, vehicle);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteVehicle(Long id) {
        Vehicle vehicle = findVehicleOrThrow(id);
        long activeTasks = vehicle.getWorkshopTasks().stream().filter(t -> !t.isFinished()).count();
        if (activeTasks > 0) {
            throw new IllegalStateException(
                "No se puede eliminar: el vehículo tiene " + activeTasks + " tarea(s) activa(s)");
        }
        vehicle.softDelete();
        vehicleRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public long countVehicles() {
        return vehicleRepository.countActiveVehicles();
    }

    private Vehicle findVehicleOrThrow(Long id) {
        return vehicleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado: " + id));
    }
}
