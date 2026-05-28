package com.hotguy.workshopmanagement.mechanic.model;

import com.hotguy.workshopmanagement.auth.model.User;
import com.hotguy.workshopmanagement.common.model.Person;
import com.hotguy.workshopmanagement.task.model.WorkshopTask;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa a un mecánico del taller.
 *
 * <p>
 * Hereda los campos de persona de {@link Person} y añade
 * la fecha de alta, la especialidad y la lista de tareas asignadas.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "mechanics")
@SQLRestriction("deleted_at IS NULL")
public class Mechanic extends Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Fecha de incorporación del mecánico al taller.
     * Usamos {@code LocalDate} porque no necesitamos hora, solo fecha.
     */
    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    /** Especialidad principal del mecánico (ej. "Electricidad", "Chapa"). */
    @Column(name = "specialty", nullable = false, length = 100)
    private String specialty;

    /**
     * Tareas de taller asignadas a este mecánico.
     * {@code fetch = LAZY}: las tareas NO se cargan de la BD hasta que
     * se accede explícitamente a la lista. Mejora el rendimiento evitando
     * cargas innecesarias de datos.
     */
    @OneToMany(mappedBy = "mechanic", fetch = FetchType.LAZY)
    private List<WorkshopTask> workshopTasks = new ArrayList<>();

    /**
     * Usuario del sistema vinculado a este mecánico.
     * Puede ser {@code null} si el mecánico no tiene acceso al sistema.
     */
    @OneToOne(mappedBy = "mechanic", fetch = FetchType.LAZY)
    private User user;

    /**
     * Devuelve el número de tareas actualmente asignadas.
     *
     * @return número de tareas
     */
    public int getWorkshopTasksSize() {
        return workshopTasks.size();
    }
}
