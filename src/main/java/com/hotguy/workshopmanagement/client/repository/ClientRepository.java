package com.hotguy.workshopmanagement.client.repository;

import com.hotguy.workshopmanagement.client.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link Client}.
 *
 * <p>
 * Spring Data genera automáticamente las implementaciones SQL.
 * El filtro {@code deleted_at IS NULL} lo aplica Hibernate automáticamente
 * gracias a {@code @SQLRestriction} en la entidad, por lo que no necesitamos
 * añadirlo en ninguna query.
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    /**
     * Busca un cliente por su NIF. Usado en operaciones de búsqueda y validación.
     *
     * @param nif el NIF del cliente
     * @return el cliente si existe y está activo
     */
    Optional<Client> findByNif(String nif);

    /**
     * Comprueba si existe un cliente con el NIF dado.
     * Más eficiente que {@code findByNif} cuando solo necesitamos saber si existe.
     *
     * @param nif el NIF a comprobar
     * @return {@code true} si existe un cliente activo con ese NIF
     */
    boolean existsByNif(String nif);

    /**
     * Busca clientes cuyo primer apellido contenga el texto dado (insensible a
     * mayúsculas).
     * Devuelve resultados paginados.
     *
     * <p>
     * La paginación evita cargar miles de registros en memoria de golpe.
     * El caller decide cuántos resultados por página y en qué orden.
     *
     * @param surname1 texto a buscar en el primer apellido
     * @param pageable configuración de paginación y ordenación
     * @return página de clientes que coinciden
     */
    Page<Client> findBySurname1ContainingIgnoreCase(String surname1, Pageable pageable);

    /**
     * Lista todos los clientes activos con paginación.
     * La anotación {@code @SQLRestriction} filtra los borrados automáticamente.
     *
     * @param pageable configuración de paginación
     * @return página de clientes activos
     */
    Page<Client> findAll(Pageable pageable);

    /**
     * Busca un cliente por su código de cliente (el código de negocio, no el ID
     * técnico).
     *
     * @param clientCode el código de cliente
     * @return el cliente si existe
     */
    Optional<Client> findByClientCode(Integer clientCode);

    /**
     * Comprueba si ya existe un cliente con el código dado.
     * Usado al crear para evitar duplicados.
     */
    boolean existsByClientCode(Integer clientCode);

    /**
     * Cuenta los clientes activos. Útil para estadísticas y reportes.
     * Hibernate aplica el filtro de soft delete automáticamente.
     */
    @Query("SELECT COUNT(c) FROM Client c")
    long countActiveClients();
}
