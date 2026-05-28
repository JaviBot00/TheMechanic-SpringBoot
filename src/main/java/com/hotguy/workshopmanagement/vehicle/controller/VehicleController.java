package com.hotguy.workshopmanagement.vehicle.controller;

import com.hotguy.workshopmanagement.vehicle.dto.VehicleRequest;
import com.hotguy.workshopmanagement.vehicle.dto.VehicleResponse;
import com.hotguy.workshopmanagement.vehicle.model.VehicleType;
import com.hotguy.workshopmanagement.vehicle.service.VehicleService;
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
 * Controlador REST para la gestión de vehículos.
 */
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehículos", description = "Gestión de vehículos del taller")
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @Operation(summary = "Registrar vehículo")
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.createVehicle(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener vehículo por ID")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    @GetMapping
    @Operation(summary = "Listar vehículos")
    public ResponseEntity<Page<VehicleResponse>> listVehicles(
            @PageableDefault(size = 20, sort = "registrationCode") Pageable pageable) {
        return ResponseEntity.ok(vehicleService.listVehicles(pageable));
    }

    @GetMapping("/by-client/{clientId}")
    @Operation(summary = "Listar vehículos de un cliente")
    public ResponseEntity<Page<VehicleResponse>> listByClient(
            @PathVariable Long clientId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(vehicleService.listVehiclesByClient(clientId, pageable));
    }

    @GetMapping("/by-type")
    @Operation(summary = "Listar vehículos por tipo")
    public ResponseEntity<Page<VehicleResponse>> listByType(
            @RequestParam VehicleType type,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(vehicleService.listVehiclesByType(type, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar vehículo")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar vehículo (soft delete)")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}
