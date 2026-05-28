package com.workshopmanagement.mechanic.controller;

import com.workshopmanagement.mechanic.dto.MechanicRequest;
import com.workshopmanagement.mechanic.dto.MechanicResponse;
import com.workshopmanagement.mechanic.service.MechanicService;
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
 * Controlador REST para la gestión de mecánicos.
 */
@RestController
@RequestMapping("/api/v1/mechanics")
@RequiredArgsConstructor
@Tag(name = "Mecánicos", description = "Gestión de mecánicos del taller")
public class MechanicController {

    private final MechanicService mechanicService;

    @PostMapping
    @Operation(summary = "Registrar mecánico")
    public ResponseEntity<MechanicResponse> createMechanic(@Valid @RequestBody MechanicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mechanicService.createMechanic(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener mecánico por ID")
    public ResponseEntity<MechanicResponse> getMechanicById(@PathVariable Long id) {
        return ResponseEntity.ok(mechanicService.getMechanicById(id));
    }

    @GetMapping
    @Operation(summary = "Listar mecánicos")
    public ResponseEntity<Page<MechanicResponse>> listMechanics(
            @PageableDefault(size = 20, sort = "surname1") Pageable pageable) {
        return ResponseEntity.ok(mechanicService.listMechanics(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar mecánicos por especialidad")
    public ResponseEntity<Page<MechanicResponse>> findBySpecialty(
            @RequestParam String specialty,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(mechanicService.findBySpecialty(specialty, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar mecánico")
    public ResponseEntity<MechanicResponse> updateMechanic(
            @PathVariable Long id, @Valid @RequestBody MechanicRequest request) {
        return ResponseEntity.ok(mechanicService.updateMechanic(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar mecánico (soft delete)")
    public ResponseEntity<Void> deleteMechanic(@PathVariable Long id) {
        mechanicService.deleteMechanic(id);
        return ResponseEntity.noContent().build();
    }
}
