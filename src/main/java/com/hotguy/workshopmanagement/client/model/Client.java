package com.hotguy.workshopmanagement.client.model;

import com.hotguy.workshopmanagement.auth.model.User;
import com.hotguy.workshopmanagement.common.model.Person;
import com.hotguy.workshopmanagement.vehicle.model.Vehicle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa a un cliente del taller.
 *
 * <p>
 * Hereda los campos de persona ({@code name}, {@code surname1}, {@code nif},
 * etc.)
 * de {@link Person} y añade el código de cliente y la lista de vehículos
 * asociados.
 *
 * <p>
 * {@code @SQLRestriction("deleted_at IS NULL")} hace que Hibernate aplique
 * automáticamente este filtro a todas las queries sobre esta entidad,
 * implementando
 * el soft delete de forma transparente sin necesidad de añadirlo manualmente.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "clients")
@SQLRestriction("deleted_at IS NULL")
public class Client extends Person {

    /** Identificador interno autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código identificador de negocio del cliente.
     * Distinto del {@code id} técnico: es el código que el taller asigna
     * internamente y puede tener significado para el usuario.
     */
    @Column(name = "client_code", nullable = false, unique = true)
    private Integer clientCode;

    /**
     * Vehículos del cliente.
     * Relación bidireccional: el lado propietario está en {@code Vehicle}.
     * {@code cascade = ALL} significa que si se persiste/borra un cliente,
     * la operación se propaga a sus vehículos.
     * {@code orphanRemoval = true} elimina los vehículos huérfanos si se
     * desvinculan de la lista.
     */
    @OneToMany(mappedBy = "proprietary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vehicle> vehicles = new ArrayList<>();

    /**
     * Usuario del sistema vinculado a este cliente.
     * Puede ser {@code null} si el cliente no tiene acceso al sistema.
     * {@code @OneToOne(mappedBy = "client")} indica que la FK está en la tabla
     * users.
     */
    @OneToOne(mappedBy = "client", fetch = FetchType.LAZY)
    private User user;

    /**
     * Añade un vehículo a la lista del cliente y establece la referencia inversa.
     *
     * @param vehicle el vehículo a añadir
     */
    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
        vehicle.setProprietary(this);
    }

    /**
     * Elimina un vehículo de la lista del cliente y rompe la referencia inversa.
     *
     * @param vehicle el vehículo a eliminar
     */
    public void removeVehicle(Vehicle vehicle) {
        vehicles.remove(vehicle);
        vehicle.setProprietary(null);
    }
}
