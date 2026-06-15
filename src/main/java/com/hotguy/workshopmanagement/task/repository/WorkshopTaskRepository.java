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

//    @Query("""
//    SELECT COALESCE(SUM(t.realHours * v.type.hourlyRate + v.type.fixedFee), 0.0)
//    FROM WorkshopTask t
//    JOIN t.vehicle v
//    WHERE t.paid = true
//    """)
    @Query(value = """
    SELECT COALESCE(SUM(
        t.real_hours * CASE v.type
            WHEN 'MOTORCYCLE' THEN 20
            WHEN 'CAR'        THEN 25
            WHEN 'VAN'        THEN 30
            WHEN 'TRUCK'      THEN 40
            ELSE 0 END
        +
        CASE v.type
            WHEN 'VAN'   THEN 30
            WHEN 'TRUCK' THEN 50
            ELSE 0 END
    ), 0.0)
    FROM workshop_tasks t
    JOIN vehicles v ON t.vehicle_id = v.id
    WHERE t.is_paid = true
    """, nativeQuery = true)
    double sumTotalRevenue();

    @Query("SELECT t FROM WorkshopTask t WHERE t.mechanic.id = :mechanicId AND t.finished = false")
    List<WorkshopTask> findActiveTasksByMechanic(Long mechanicId);
}
