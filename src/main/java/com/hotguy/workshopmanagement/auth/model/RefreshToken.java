package com.hotguy.workshopmanagement.auth.model;

import com.hotguy.workshopmanagement.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Entidad JPA que representa un refresh token almacenado en base de datos.
 *
 * <p>
 * Los refresh tokens permiten renovar el access token JWT sin que el usuario
 * tenga que volver a introducir sus credenciales. Al guardarlos en BD podemos:
 * <ul>
 * <li>Invalidarlos al hacer logout (revocación)</li>
 * <li>Detectar reutilización maliciosa (token rotation)</li>
 * <li>Establecer expiración independiente del access token</li>
 * </ul>
 *
 * <p>
 * Flujo completo:
 *
 * <pre>
 *   login → access token (1h) + refresh token (7 días) → guardado en BD
 *   access token expira → cliente envía refresh token → nuevo access token
 *   logout → refresh token marcado como revocado en BD
 * </pre>
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * El token en sí: una cadena UUID aleatoria.
     * Se almacena tal cual (no es un JWT, no lleva información embebida).
     */
    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    /**
     * Fecha y hora de expiración del token.
     * Una vez superada esta fecha, el token no puede usarse aunque no esté
     * revocado.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * {@code true} si el token ha sido revocado (logout o token rotation).
     * Un token revocado no puede usarse aunque no haya expirado.
     */
    @Builder.Default
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    /**
     * Usuario al que pertenece este refresh token.
     * Un usuario puede tener múltiples refresh tokens activos
     * (ej. sesión en móvil y en PC a la vez).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Comprueba si el token sigue siendo válido (no expirado y no revocado).
     *
     * @return {@code true} si el token puede usarse para generar un nuevo access
     * token
     */
    public boolean isValid() {
        return !revoked && Instant.now().isBefore(expiresAt);
    }

    /**
     * Revoca el token, impidiendo su uso futuro.
     */
    public void revoke() {
        this.revoked = true;
    }
}
