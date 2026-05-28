package com.hotguy.workshopmanagement.task.model;

import com.hotguy.workshopmanagement.client.model.Client;
import com.hotguy.workshopmanagement.common.audit.AuditableEntity;
import com.hotguy.workshopmanagement.mechanic.model.Mechanic;
import com.hotguy.workshopmanagement.vehicle.model.Vehicle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Entidad JPA que representa una orden de trabajo en el taller.
 *
 * <p>
 * Una tarea de taller vincula un {@link Vehicle}, un {@link Mechanic}
 * y un {@link Client}, y gestiona el ciclo de vida completo de una reparación:
 * diagnóstico → trabajo en progreso → finalización → pago.
 *
 * <p>
 * El coste se calcula dinámicamente a partir de las horas reales trabajadas
 * y las tarifas del tipo de vehículo (delegando en
 * {@link Vehicle#calculatePrice}).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "workshop_tasks")
public class WorkshopTask extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Descripción del problema detectado. Obligatorio al crear la tarea.
     */
    @Column(name = "diagnostic", nullable = false, length = 500)
    private String diagnostic;

    /**
     * Descripción de la solución aplicada.
     * Se rellena cuando la tarea está en progreso o al finalizarla.
     */
    @Column(name = "solution", length = 500)
    private String solution;

    /**
     * Horas estimadas para completar la tarea (presupuesto).
     */
    @Column(name = "preview_hours", nullable = false)
    private float previewHours;

    /**
     * Horas reales trabajadas. Se acumula mediante {@link #addHours(float)}.
     * Inicia en 0 y solo puede crecer mientras la tarea no esté finalizada.
     */
    @Column(name = "real_hours", nullable = false)
    private float realHours;

    /**
     * {@code true} cuando la tarea ha sido marcada como completada.
     */
    @Column(name = "is_finished", nullable = false)
    private boolean finished;

    /**
     * {@code true} cuando el cliente ha abonado la factura.
     * Solo puede ser {@code true} si {@code finished} también lo es.
     */
    @Column(name = "is_paid", nullable = false)
    private boolean paid;

    /**
     * Fecha de inicio de la tarea.
     */
    @Column(name = "init_date", nullable = false)
    private LocalDate initDate;

    /**
     * Notas adicionales del mecánico sobre la tarea.
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Cliente propietario del vehículo en reparación.
     * Se almacena por comodidad de consulta (evita el join a través de Vehicle).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /**
     * Vehículo objeto de la reparación.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /**
     * Mecánico responsable de la tarea.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private Mechanic mechanic;

    // =========================================================================
    // LÓGICA DE NEGOCIO
    // =========================================================================

    /**
     * Acumula horas de trabajo a la tarea.
     *
     * @param hours horas a añadir (deben ser positivas)
     * @throws IllegalStateException    si la tarea ya está finalizada
     * @throws IllegalArgumentException si las horas no son positivas
     */
    public void addHours(float hours) {
        if (finished) {
            throw new IllegalStateException("No se pueden añadir horas a una tarea finalizada");
        }
        if (hours <= 0) {
            throw new IllegalArgumentException("Las horas deben ser mayores que cero");
        }
        this.realHours += hours;
    }

    /**
     * Marca la tarea como finalizada. Operación idempotente.
     */
    public void finish() {
        this.finished = true;
    }

    /**
     * Marca la tarea como pagada.
     *
     * @throws IllegalStateException si la tarea no está finalizada aún
     */
    public void markAsPaid() {
        if (!finished) {
            throw new IllegalStateException("No se puede cobrar una tarea no finalizada");
        }
        this.paid = true;
    }

    /**
     * Revierte el estado de pago (por ejemplo, ante un error bancario).
     */
    public void markAsUnpaid() {
        this.paid = false;
    }

    /**
     * Calcula el coste total de la tarea en base a las horas reales trabajadas
     * y las tarifas del tipo de vehículo. Devuelve 0 si la tarea no está
     * finalizada.
     *
     * @return coste total en euros
     */
    public float getTotalCost() {
        if (!finished)
            return 0f;
        return vehicle.calculatePrice(realHours);
    }

    /**
     * Calcula el coste estimado según las horas presupuestadas.
     * Útil para mostrar el presupuesto antes de iniciar la tarea.
     *
     * @return coste estimado en euros
     */
    public float getEstimatedCost() {
        return vehicle.calculatePrice(previewHours);
    }

    /**
     * Calcula el porcentaje de progreso de la tarea.
     *
     * @return porcentaje entre 0 y 100 (tope máximo aunque se superen las horas)
     */
    public float getProgress() {
        if (previewHours <= 0)
            return 0f;
        return Math.min((realHours / previewHours) * 100f, 100f);
    }

    /**
     * Devuelve el estado de la tarea como cadena de texto legible.
     *
     * @return "Pendiente", "En progreso", "Finalizada" o "Pagada"
     */
    public String getStatus() {
        if (paid)
            return "Pagada";
        if (finished)
            return "Finalizada";
        if (realHours > 0)
            return "En progreso";
        return "Pendiente";
    }
}
