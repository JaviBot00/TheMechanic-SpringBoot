package com.workshopmanagement.report.service;

import com.workshopmanagement.client.repository.ClientRepository;
import com.workshopmanagement.mechanic.repository.MechanicRepository;
import com.workshopmanagement.report.dto.SummaryReportResponse;
import com.workshopmanagement.task.repository.WorkshopTaskRepository;
import com.workshopmanagement.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de reportes y estadísticas del taller.
 * Agrega datos de múltiples repositorios para generar informes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ClientRepository clientRepository;
    private final MechanicRepository mechanicRepository;
    private final VehicleRepository vehicleRepository;
    private final WorkshopTaskRepository taskRepository;

    /**
     * Genera un resumen general del estado del taller.
     * Agrega contadores de todas las entidades y la facturación total.
     *
     * @return DTO con las estadísticas generales
     */
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")
    public SummaryReportResponse getSummary() {
        long totalClients   = clientRepository.countActiveClients();
        long totalMechanics = mechanicRepository.countActiveMechanics();
        long totalVehicles  = vehicleRepository.countActiveVehicles();
        long totalTasks     = taskRepository.count();
        long pendingTasks   = taskRepository.countPendingTasks();
        Double revenue      = taskRepository.sumTotalRevenue();

        return new SummaryReportResponse(
                totalClients,
                totalMechanics,
                totalVehicles,
                totalTasks,
                pendingTasks,
                revenue != null ? revenue : 0.0
        );
    }
}
