package com.hotguy.workshopmanagement.mechanic.repository;

import com.hotguy.workshopmanagement.mechanic.model.Mechanic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link Mechanic}.
 */
@Repository
public interface MechanicRepository extends JpaRepository<Mechanic, Long> {

    Optional<Mechanic> findByNif(String nif);

    boolean existsByNif(String nif);

    Page<Mechanic> findBySpecialtyContainingIgnoreCase(String specialty, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Mechanic m")
    long countActiveMechanics();
}
