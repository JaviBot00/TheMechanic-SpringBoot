package com.workshopmanagement.auth.repository;

import com.workshopmanagement.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link User}.
 *
 * <p>Spring Data genera automáticamente la implementación SQL de cada método
 * basándose en su nombre. No es necesario escribir ninguna query.
 *
 * <p>Ejemplos de derivación de queries por nombre:
 * <ul>
 *   <li>{@code findByUsername} → {@code SELECT * FROM users WHERE username = ?}</li>
 *   <li>{@code existsByUsername} → {@code SELECT COUNT(*) > 0 FROM users WHERE username = ?}</li>
 * </ul>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su nombre de usuario.
     * Usado por Spring Security en el proceso de autenticación.
     *
     * @param username el nombre de usuario
     * @return el usuario envuelto en Optional, vacío si no existe
     */
    Optional<User> findByUsername(String username);

    /**
     * Comprueba si ya existe un usuario con el username dado.
     * Usado al crear usuarios para evitar duplicados.
     *
     * @param username el nombre de usuario a comprobar
     * @return {@code true} si el username ya está registrado
     */
    boolean existsByUsername(String username);
}
