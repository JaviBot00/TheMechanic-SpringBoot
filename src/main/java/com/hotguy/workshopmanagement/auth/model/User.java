package com.hotguy.workshopmanagement.auth.model;

import com.hotguy.workshopmanagement.client.model.Client;
import com.hotguy.workshopmanagement.common.audit.AuditableEntity;
import com.hotguy.workshopmanagement.mechanic.model.Mechanic;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Entidad JPA que representa una cuenta de usuario del sistema.
 *
 * <p>
 * Implementa {@link UserDetails}, que es la interfaz que Spring Security
 * utiliza para obtener la información de autenticación de un usuario.
 * Al implementarla, esta clase se convierte en la fuente de verdad para
 * el sistema de seguridad.
 *
 * <p>
 * La entidad {@code User} está separada del modelo de negocio ({@link Client},
 * {@link Mechanic}) para respetar el principio de responsabilidad única:
 * las credenciales son una responsabilidad de seguridad, no de negocio.
 *
 * <p>
 * Un {@code User} con rol {@code ADMIN} no necesita estar vinculado
 * a ningún cliente ni mecánico. Las relaciones son opcionales.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends AuditableEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre de usuario para el login. Único en el sistema.
     * Normalmente el NIF o el email de la persona.
     */
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    /**
     * Contraseña encriptada con BCrypt.
     * NUNCA se almacena en texto plano. Spring Security se encarga
     * de verificarla mediante {@code PasswordEncoder}.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Rol del usuario en el sistema.
     * Persiste como texto (STRING) para mayor legibilidad en la BD.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /**
     * Indica si la cuenta está activa.
     * El admin puede desactivar cuentas sin borrarlas.
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /**
     * Cliente vinculado a esta cuenta. Puede ser {@code null}
     * si el usuario es un mecánico o un admin puro.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", unique = true)
    private Client client;

    /**
     * Mecánico vinculado a esta cuenta. Puede ser {@code null}
     * si el usuario es un cliente o un admin puro.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", unique = true)
    private Mechanic mechanic;

    // =========================================================================
    // IMPLEMENTACIÓN DE UserDetails
    // Spring Security llama a estos métodos para autenticar y autorizar.
    // =========================================================================

    /**
     * Devuelve los permisos del usuario.
     * Spring Security espera que los roles tengan el prefijo "ROLE_".
     * {@code SimpleGrantedAuthority("ROLE_ADMIN")} se traduce a hasRole("ADMIN").
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Indica si la cuenta no ha expirado. Siempre {@code true} en este sistema.
     * Se podría implementar con una fecha de expiración si fuera necesario.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indica si la cuenta no está bloqueada.
     * Se podría usar para bloquear temporalmente por intentos fallidos.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indica si las credenciales no han expirado.
     * Se podría implementar para forzar cambio de contraseña periódico.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indica si la cuenta está habilitada.
     * Mapeado al campo {@code enabled} de la entidad.
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
