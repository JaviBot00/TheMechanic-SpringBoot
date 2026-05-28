package com.hotguy.workshopmanagement.vehicle.model;

import com.hotguy.workshopmanagement.client.model.Client;
import com.hotguy.workshopmanagement.common.audit.AuditableEntity;
import com.hotguy.workshopmanagement.task.model.WorkshopTask;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa un vehículo registrado en el taller.
 *
 * <p>
 * La lógica de tarifas se ha movido al enum {@link VehicleType},
 * que actúa como tabla de configuración de precios. Esto elimina la
 * jerarquía de subclases (Car, Van, Truck...) del proyecto original,
 * simplificando el modelo sin perder funcionalidad.
 *
 * <p>
 * Un vehículo pertenece siempre a un {@link Client} y puede tener
 * múltiples {@link WorkshopTask} a lo largo de su vida.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "vehicles")
@SQLRestriction("deleted_at IS NULL")
public class Vehicle extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Matrícula del vehículo. Identificador único de negocio.
     * Se almacena en mayúsculas por convención.
     */
    @Column(name = "registration_code", nullable = false, unique = true, length = 20)
    private String registrationCode;

    /** Marca y modelo del vehículo (ej. "Toyota Corolla"). */
    @Column(name = "model", nullable = false, length = 150)
    private String model;

    /**
     * Tipo de vehículo. Determina las tarifas de facturación.
     * Se persiste como texto (EnumType.STRING) en lugar de número ordinal
     * para que la BD sea legible y no se rompa al reordenar el enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private VehicleType type;

    /**
     * Cliente propietario del vehículo.
     * Lado propietario de la relación (contiene la FK {@code client_id}).
     * {@code LAZY}: no se carga el cliente de la BD hasta que se accede.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client proprietary;

    /**
     * Historial de tareas de taller realizadas sobre este vehículo.
     */
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkshopTask> workshopTasks = new ArrayList<>();

    /**
     * Calcula el precio de una reparación en base al tipo de vehículo.
     * Delega en {@link VehicleType#calculatePrice(float)}.
     *
     * @param hours horas trabajadas
     * @return precio total en euros
     */
    public float calculatePrice(float hours) {
        return type.calculatePrice(hours);
    }

    /**
     * Devuelve el porcentaje de tareas completadas sobre el total.
     *
     * @return porcentaje entre 0 y 100, o 0 si no hay tareas
     */
    public float getCompletionPercentage() {
        if (workshopTasks.isEmpty())
            return 0f;
        long finished = workshopTasks.stream()
                .filter(WorkshopTask::isFinished)
                .count();
        return (finished * 100f) / workshopTasks.size();
    }

    /**
     * Devuelve la facturación total generada por este vehículo
     * (suma de tareas pagadas).
     *
     * @return importe total en euros
     */
    public float getTotalRevenue() {
        return workshopTasks.stream()
                .filter(WorkshopTask::isPaid)
                .map(WorkshopTask::getTotalCost)
                .reduce(0f, Float::sum);
    }
}
