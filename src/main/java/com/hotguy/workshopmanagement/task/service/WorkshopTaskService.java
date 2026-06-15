package com.hotguy.workshopmanagement.task.service;

import com.hotguy.workshopmanagement.client.model.Client;
import com.hotguy.workshopmanagement.common.exception.ResourceNotFoundException;
import com.hotguy.workshopmanagement.mechanic.model.Mechanic;
import com.hotguy.workshopmanagement.mechanic.repository.MechanicRepository;
import com.hotguy.workshopmanagement.task.dto.WorkshopTaskRequest;
import com.hotguy.workshopmanagement.task.dto.WorkshopTaskResponse;
import com.hotguy.workshopmanagement.task.mapper.WorkshopTaskMapper;
import com.hotguy.workshopmanagement.task.model.WorkshopTask;
import com.hotguy.workshopmanagement.task.repository.WorkshopTaskRepository;
import com.hotguy.workshopmanagement.vehicle.model.Vehicle;
import com.hotguy.workshopmanagement.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio que gestiona la lógica de negocio de las órdenes de trabajo.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkshopTaskService {

    private final WorkshopTaskRepository taskRepository;
    private final VehicleRepository vehicleRepository;
    private final MechanicRepository mechanicRepository;
    private final WorkshopTaskMapper taskMapper;

    /**
     * Crea una nueva orden de trabajo.
     */
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public WorkshopTaskResponse createTask(WorkshopTaskRequest request) {
        if (request.vehicleId() == null) {
            throw new IllegalArgumentException("El ID del vehículo es obligatorio para crear una tarea");
        }
        if (request.mechanicId() == null) {
            throw new IllegalArgumentException("El ID del mecánico es obligatorio para crear una tarea");
        }
        if (request.previewHours() == null) {
            throw new IllegalArgumentException("Las horas estimadas son obligatorias para crear una tarea");
        }
        if (request.initDate() == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria para crear una tarea");
        }
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado: " + request.vehicleId()));
        Mechanic mechanic = mechanicRepository.findById(request.mechanicId())
            .orElseThrow(() -> new ResourceNotFoundException("Mecánico no encontrado: " + request.mechanicId()));
        Client client = vehicle.getProprietary();

        WorkshopTask task = taskMapper.toEntity(request);
        task.setVehicle(vehicle);
        task.setMechanic(mechanic);
        task.setClient(client);
        task.setRealHours(0f);
        task.setFinished(false);
        task.setPaid(false);

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC') or hasRole('CLIENT')")
    public WorkshopTaskResponse getTaskById(Long id) {
        return taskMapper.toResponse(findTaskOrThrow(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public Page<WorkshopTaskResponse> listTasks(Pageable pageable) {
        return taskRepository.findAll(pageable).map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC') or hasRole('CLIENT')")
    public Page<WorkshopTaskResponse> listByClient(Long clientId, Pageable pageable) {
        return taskRepository.findByClientId(clientId, pageable).map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC') or hasRole('CLIENT')")
    public Page<WorkshopTaskResponse> listByVehicle(Long vehicleId, Pageable pageable) {
        return taskRepository.findByVehicleId(vehicleId, pageable).map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public Page<WorkshopTaskResponse> listByMechanic(Long mechanicId, Pageable pageable) {
        return taskRepository.findByMechanicId(mechanicId, pageable).map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public Page<WorkshopTaskResponse> listPendingTasks(Pageable pageable) {
        return taskRepository.findByFinishedFalse(pageable).map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public Page<WorkshopTaskResponse> listUnpaidFinishedTasks(Pageable pageable) {
        return taskRepository.findByFinishedTrueAndPaidFalse(pageable).map(taskMapper::toResponse);
    }

    /**
     * Añade horas trabajadas a una tarea.
     */
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public WorkshopTaskResponse addHours(Long id, float hours) {
        WorkshopTask task = findTaskOrThrow(id);
        task.addHours(hours);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    /**
     * Actualiza diagnóstico y/o solución de la tarea.
     */
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public WorkshopTaskResponse updateTask(Long id, WorkshopTaskRequest request) {
        WorkshopTask task = findTaskOrThrow(id);
        if (task.isPaid()) {
            throw new IllegalStateException("No se puede modificar una tarea ya pagada");
        }
        if (request.diagnostic() != null && !request.diagnostic().isBlank()) {
            task.setDiagnostic(request.diagnostic());
        }
        if (request.previewHours() != null && request.previewHours() > 0) {
            task.setPreviewHours(request.previewHours());
        }
        if (request.notes() != null) {
            task.setNotes(request.notes());
        }
        return taskMapper.toResponse(taskRepository.save(task));
    }

    /**
     * Marca la tarea como finalizada.
     */
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public WorkshopTaskResponse finishTask(Long id, String solution) {
        WorkshopTask task = findTaskOrThrow(id);
        task.finish();
        if (solution != null && !solution.isBlank()) {
            task.setSolution(solution);
        }
        return taskMapper.toResponse(taskRepository.save(task));
    }

    /**
     * Marca la tarea como pagada. Solo ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public WorkshopTaskResponse markAsPaid(Long id) {
        WorkshopTask task = findTaskOrThrow(id);
        task.markAsPaid();
        return taskMapper.toResponse(taskRepository.save(task));
    }

    /**
     * Elimina una tarea. Solo si no está pagada.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTask(Long id) {
        WorkshopTask task = findTaskOrThrow(id);
        if (task.isPaid()) {
            throw new IllegalStateException("No se puede eliminar una tarea ya pagada");
        }
        taskRepository.delete(task);
    }

    private WorkshopTask findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada: " + id));
    }
}
