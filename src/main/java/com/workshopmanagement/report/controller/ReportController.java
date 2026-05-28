package com.workshopmanagement.report.controller;

import com.workshopmanagement.report.dto.SummaryReportResponse;
import com.workshopmanagement.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para reportes y estadísticas del taller.
 * Accesible por ADMIN y MECHANIC (definido en SecurityConfig y @PreAuthorize del Service).
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Estadísticas e informes del taller")
public class ReportController {

    private final ReportService reportService;

    /**
     * Devuelve un resumen general del estado del taller:
     * número de clientes, mecánicos, vehículos, tareas y facturación total.
     *
     * @return 200 OK con el resumen estadístico
     */
    @GetMapping("/summary")
    @Operation(summary = "Resumen general del taller")
    public ResponseEntity<SummaryReportResponse> getSummary() {
        return ResponseEntity.ok(reportService.getSummary());
    }
}
