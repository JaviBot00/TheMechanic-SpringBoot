package com.hotguy.workshopmanagement.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Clase base de auditoría para todas las entidades JPA del sistema.
 *
 * <p>
 * Al extender esta clase, una entidad hereda automáticamente cuatro campos:
 * <ul>
 * <li>{@code createdAt}: momento exacto en que se creó el registro</li>
 * <li>{@code updatedAt}: momento de la última modificación</li>
 * <li>{@code deletedAt}: momento del borrado lógico (null si está activo)</li>
 * </ul>
 *
 * <p>
 * Los campos {@code createdAt} y {@code updatedAt} se gestionan automáticamente
 * por Spring Data JPA gracias a
 * {@code @EntityListeners(AuditingEntityListener.class)},
 * que requiere que {@code @EnableJpaAuditing} esté activo en la clase
 * principal.
 *
 * <p>
 * {@code @MappedSuperclass} indica a JPA que esta clase no es una entidad por
 * sí
 * misma (no tiene tabla propia), sino una superclase cuyos campos se heredan en
 * las tablas de las entidades hijas.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    /**
     * Fecha y hora de creación del registro.
     * Se rellena automáticamente al persistir por primera vez.
     * {@code updatable = false} impide que se modifique después.
     * Usamos {@code Instant} (UTC) en lugar de {@code LocalDateTime}
     * para evitar ambigüedades con zonas horarias.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Fecha y hora de la última modificación del registro.
     * Se actualiza automáticamente en cada {@code merge} de Hibernate.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Fecha y hora del borrado lógico (soft delete).
     * Cuando es {@code null}, el registro está activo.
     * Cuando tiene valor, el registro está "borrado" pero sigue en la BD.
     * Las queries deben filtrar por {@code deleted_at IS NULL}.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Marca el registro como borrado lógicamente.
     * Establece {@code deletedAt} al momento actual en UTC.
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    /**
     * Comprueba si el registro está activo (no borrado).
     *
     * @return {@code true} si el registro no ha sido borrado lógicamente
     */
    public boolean isActive() {
        return this.deletedAt == null;
    }
}
