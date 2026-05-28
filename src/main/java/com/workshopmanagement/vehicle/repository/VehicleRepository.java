package com.workshopmanagement.vehicle.repository;

import com.workshopmanagement.vehicle.model.Vehicle;
import com.workshopmanagement.vehicle.model.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link Vehicle}.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByRegistrationCode(String registrationCode);
    boolean existsByRegistrationCode(String registrationCode);
    Page<Vehicle> findByProprietaryId(Long clientId, Pageable pageable);
    Page<Vehicle> findByType(VehicleType type, Pageable pageable);

    @Query("SELECT COUNT(v) FROM Vehicle v")
    long countActiveVehicles();
}
