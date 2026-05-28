package com.hotguy.workshopmanagement.report.dto;

/**
 * DTO de respuesta con el resumen general del taller.
 * Usado en el endpoint de estadísticas generales.
 *
 * @param totalClients   número total de clientes activos
 * @param totalMechanics número total de mecánicos activos
 * @param totalVehicles  número total de vehículos registrados
 * @param totalTasks     número total de órdenes de trabajo
 * @param pendingTasks   tareas en curso o pendientes
 * @param totalRevenue   facturación total acumulada (tareas pagadas)
 */
public record SummaryReportResponse(
    long totalClients,
    long totalMechanics,
    long totalVehicles,
    long totalTasks,
    long pendingTasks,
    double totalRevenue) {
}
