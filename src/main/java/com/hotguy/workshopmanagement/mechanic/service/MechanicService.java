package com.hotguy.workshopmanagement.mechanic.service;

import com.hotguy.workshopmanagement.common.exception.ResourceNotFoundException;
import com.hotguy.workshopmanagement.mechanic.dto.MechanicRequest;
import com.hotguy.workshopmanagement.mechanic.dto.MechanicResponse;
import com.hotguy.workshopmanagement.mechanic.mapper.MechanicMapper;
import com.hotguy.workshopmanagement.mechanic.model.Mechanic;
import com.hotguy.workshopmanagement.mechanic.repository.MechanicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio que gestiona la lógica de negocio de los mecánicos.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MechanicService {

    private final MechanicRepository mechanicRepository;
    private final MechanicMapper mechanicMapper;

    /**
     * Crea un nuevo mecánico. Solo ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public MechanicResponse createMechanic(MechanicRequest request) {
        if (mechanicRepository.existsByNif(request.nif())) {
            throw new IllegalArgumentException("Ya existe un mecánico con el NIF: " + request.nif());
        }
        return mechanicMapper.toResponse(mechanicRepository.save(mechanicMapper.toEntity(request)));
    }

    /**
     * Obtiene un mecánico por ID. ADMIN y MECHANIC (el propio) pueden acceder.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public MechanicResponse getMechanicById(Long id) {
        return mechanicMapper.toResponse(findMechanicOrThrow(id));
    }

    /**
     * Lista todos los mecánicos activos con paginación.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public Page<MechanicResponse> listMechanics(Pageable pageable) {
        return mechanicRepository.findAll(pageable).map(mechanicMapper::toResponse);
    }

    /**
     * Busca mecánicos por especialidad.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public Page<MechanicResponse> findBySpecialty(String specialty, Pageable pageable) {
        return mechanicRepository.findBySpecialtyContainingIgnoreCase(specialty, pageable)
            .map(mechanicMapper::toResponse);
    }

    /**
     * Actualiza datos de un mecánico. Solo ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public MechanicResponse updateMechanic(Long id, MechanicRequest request) {
        Mechanic mechanic = findMechanicOrThrow(id);
        if (!mechanic.getNif().equals(request.nif()) && mechanicRepository.existsByNif(request.nif())) {
            throw new IllegalArgumentException("El NIF ya está en uso: " + request.nif());
        }
        mechanicMapper.updateEntityFromRequest(request, mechanic);
        return mechanicMapper.toResponse(mechanicRepository.save(mechanic));
    }

    /**
     * Elimina lógicamente un mecánico.
     * No se puede eliminar si tiene tareas activas asignadas.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMechanic(Long id) {
        Mechanic mechanic = findMechanicOrThrow(id);
        long activeTasks = mechanic.getWorkshopTasks().stream()
            .filter(t -> !t.isFinished()).count();
        if (activeTasks > 0) {
            throw new IllegalStateException(
                "No se puede eliminar el mecánico: tiene " + activeTasks + " tarea(s) activa(s) asignada(s)");
        }
        mechanic.softDelete();
        mechanicRepository.save(mechanic);
    }

    /**
     * Devuelve el número total de mecánicos activos.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public long countMechanics() {
        return mechanicRepository.countActiveMechanics();
    }

    private Mechanic findMechanicOrThrow(Long id) {
        return mechanicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Mecánico no encontrado: " + id));
    }
}
