package com.hotguy.workshopmanagement.auth.repository;

import com.hotguy.workshopmanagement.auth.model.RefreshToken;
import com.hotguy.workshopmanagement.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link RefreshToken}.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Busca un refresh token por su valor de cadena.
     * Usado al validar el token enviado por el cliente.
     *
     * @param token el valor del token
     * @return el refresh token si existe
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Revoca todos los refresh tokens activos de un usuario.
     * Se llama al hacer logout para invalidar todas las sesiones activas.
     *
     * <p>
     * {@code @Modifying} indica que esta query modifica datos (no es SELECT).
     * Requiere estar dentro de una transacción ({@code @Transactional} en el
     * Service).
     * {@code clearAutomatically = true} limpia el contexto de persistencia de JPA
     * después de la actualización para evitar que entidades en memoria queden
     * obsoletas.
     *
     * @param user el usuario cuyas sesiones se van a cerrar
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    void revokeAllUserTokens(User user);

    /**
     * Elimina todos los tokens ya revocados o expirados de un usuario.
     * Limpieza periódica para no acumular tokens inútiles en la BD.
     *
     * @param user el usuario
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user AND (rt.revoked = true OR rt.expiresAt < CURRENT_TIMESTAMP)")
    void deleteExpiredAndRevokedTokens(User user);
}
