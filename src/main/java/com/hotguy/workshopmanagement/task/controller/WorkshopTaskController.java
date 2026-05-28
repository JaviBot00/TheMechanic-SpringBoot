package com.hotguy.workshopmanagement.task.controller;

import com.hotguy.workshopmanagement.task.dto.AddHoursRequest;
import com.hotguy.workshopmanagement.task.dto.WorkshopTaskRequest;
import com.hotguy.workshopmanagement.task.dto.WorkshopTaskResponse;
import com.hotguy.workshopmanagement.task.service.WorkshopTaskService;
import io.swagger.v3.oas.annotations.Operation;
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
 * Controlador REST para la gestión de órdenes de trabajo del taller.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tareas de Taller", description = "Gestión de órdenes de trabajo")
public class WorkshopTaskController {

    private final WorkshopTaskService taskService;

    @PostMapping
    @Operation(summary = "Crear tarea de taller")
    public ResponseEntity<WorkshopTaskResponse> createTask(@Valid @RequestBody WorkshopTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener tarea por ID")
    public ResponseEntity<WorkshopTaskResponse> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las tareas")
    public ResponseEntity<Page<WorkshopTaskResponse>> listTasks(
            @PageableDefault(size = 20, sort = "initDate") Pageable pageable) {
        return ResponseEntity.ok(taskService.listTasks(pageable));
    }

    @GetMapping("/by-client/{clientId}")
    @Operation(summary = "Listar tareas de un cliente")
    public ResponseEntity<Page<WorkshopTaskResponse>> listByClient(
            @PathVariable Long clientId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.listByClient(clientId, pageable));
    }

    @GetMapping("/by-vehicle/{vehicleId}")
    @Operation(summary = "Listar tareas de un vehículo")
    public ResponseEntity<Page<WorkshopTaskResponse>> listByVehicle(
            @PathVariable Long vehicleId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.listByVehicle(vehicleId, pageable));
    }

    @GetMapping("/by-mechanic/{mechanicId}")
    @Operation(summary = "Listar tareas de un mecánico")
    public ResponseEntity<Page<WorkshopTaskResponse>> listByMechanic(
            @PathVariable Long mechanicId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.listByMechanic(mechanicId, pageable));
    }

    @GetMapping("/pending")
    @Operation(summary = "Listar tareas pendientes")
    public ResponseEntity<Page<WorkshopTaskResponse>> listPending(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.listPendingTasks(pageable));
    }

    @GetMapping("/unpaid")
    @Operation(summary = "Listar tareas finalizadas sin pagar")
    public ResponseEntity<Page<WorkshopTaskResponse>> listUnpaid(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.listUnpaidFinishedTasks(pageable));
    }

    @PatchMapping("/{id}/hours")
    @Operation(summary = "Añadir horas de trabajo a una tarea")
    public ResponseEntity<WorkshopTaskResponse> addHours(
            @PathVariable Long id, @Valid @RequestBody AddHoursRequest request) {
        return ResponseEntity.ok(taskService.addHours(id, request.hours()));
    }

    @PatchMapping("/{id}/finish")
    @Operation(summary = "Marcar tarea como finalizada")
    public ResponseEntity<WorkshopTaskResponse> finishTask(
            @PathVariable Long id,
            @RequestParam(required = false) String solution) {
        return ResponseEntity.ok(taskService.finishTask(id, solution));
    }

    @PatchMapping("/{id}/pay")
    @Operation(summary = "Marcar tarea como pagada (solo ADMIN)")
    public ResponseEntity<WorkshopTaskResponse> markAsPaid(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.markAsPaid(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos de una tarea")
    public ResponseEntity<WorkshopTaskResponse> updateTask(
            @PathVariable Long id, @Valid @RequestBody WorkshopTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar tarea (solo si no está pagada)")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
