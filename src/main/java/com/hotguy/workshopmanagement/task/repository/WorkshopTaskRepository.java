package com.hotguy.workshopmanagement.task.repository;

import com.hotguy.workshopmanagement.task.model.WorkshopTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la entidad {@link WorkshopTask}.
 */
@Repository
public interface WorkshopTaskRepository extends JpaRepository<WorkshopTask, Long> {

    Page<WorkshopTask> findByClientId(Long clientId, Pageable pageable);

    Page<WorkshopTask> findByVehicleId(Long vehicleId, Pageable pageable);

    Page<WorkshopTask> findByMechanicId(Long mechanicId, Pageable pageable);

    Page<WorkshopTask> findByFinishedFalse(Pageable pageable);

    Page<WorkshopTask> findByFinishedTrueAndPaidFalse(Pageable pageable);

    @Query("SELECT COUNT(t) FROM WorkshopTask t WHERE t.finished = false")
    long countPendingTasks();

    List<WorkshopTask> findByPaidTrue();

    @Query("SELECT t FROM WorkshopTask t WHERE t.mechanic.id = :mechanicId AND t.finished = false")
    List<WorkshopTask> findActiveTasksByMechanic(Long mechanicId);
}
