package com.hotguy.workshopmanagement.common.model;

import com.hotguy.workshopmanagement.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Superclase JPA que representa los atributos comunes de una persona física
 * dentro del sistema (clientes y mecánicos).
 *
 * <p>
 * No genera tabla propia. Sus columnas se incluyen en las tablas
 * de {@code Client} y {@code Mechanic} respectivamente.
 *
 * <p>
 * Extiende {@link AuditableEntity} para heredar los campos de auditoría
 * ({@code createdAt}, {@code updatedAt}, {@code deletedAt}).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@MappedSuperclass
public abstract class Person extends AuditableEntity {

    /** Nombre de pila. */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Primer apellido. */
    @Column(name = "surname1", nullable = false, length = 100)
    private String surname1;

    /** Segundo apellido (opcional). */
    @Column(name = "surname2", length = 100)
    private String surname2;

    /**
     * NIF (Número de Identificación Fiscal).
     * Único en su tabla correspondiente.
     */
    @Column(name = "nif", nullable = false, unique = true, length = 20)
    private String nif;

    /** Correo electrónico de contacto. */
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    /** Número de teléfono de contacto. */
    @Column(name = "telephone", length = 20)
    private String telephone;
}
