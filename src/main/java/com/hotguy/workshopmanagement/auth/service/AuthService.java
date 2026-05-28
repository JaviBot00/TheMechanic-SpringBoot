package com.hotguy.workshopmanagement.auth.service;

import com.hotguy.workshopmanagement.auth.dto.AuthResponse;
import com.hotguy.workshopmanagement.auth.dto.LoginRequest;
import com.hotguy.workshopmanagement.auth.dto.RefreshRequest;
import com.hotguy.workshopmanagement.auth.dto.RegisterRequest;
import com.hotguy.workshopmanagement.auth.model.RefreshToken;
import com.hotguy.workshopmanagement.auth.model.User;
import com.hotguy.workshopmanagement.auth.repository.RefreshTokenRepository;
import com.hotguy.workshopmanagement.auth.repository.UserRepository;
import com.hotguy.workshopmanagement.client.repository.ClientRepository;
import com.hotguy.workshopmanagement.common.exception.ResourceNotFoundException;
import com.hotguy.workshopmanagement.config.JwtProperties;
import com.hotguy.workshopmanagement.mechanic.repository.MechanicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Servicio que gestiona el ciclo de vida de la autenticación:
 * login, registro de usuarios, renovación de tokens y logout.
 *
 * <p>
 * {@code @Transactional} a nivel de clase hace que todos los métodos
 * públicos se ejecuten dentro de una transacción de BD. Si ocurre una
 * excepción, los cambios se revierten automáticamente (rollback).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ClientRepository clientRepository;
    private final MechanicRepository mechanicRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProperties jwtProperties;

    /**
     * Autentica al usuario y devuelve un par de tokens JWT.
     *
     * <p>
     * Internamente, {@code AuthenticationManager.authenticate()} verifica las
     * credenciales contra la BD usando el {@code UserDetailsService} configurado.
     * Si las credenciales son incorrectas, lanza {@code BadCredentialsException}.
     *
     * @param request credenciales del usuario
     * @return access token + refresh token
     * @throws org.springframework.security.core.AuthenticationException si las
     *                                                                   credenciales
     *                                                                   son
     *                                                                   incorrectas
     */
    public AuthResponse login(LoginRequest request) {
        // 1. Delegar la verificación de credenciales a Spring Security.
        // Internamente: carga el UserDetails por username, compara el hash de la
        // contraseña.
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        // 2. Si llegamos aquí, las credenciales son correctas. Obtener el usuario.
        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 3. Revocar tokens anteriores del usuario (seguridad: una sesión activa por
        // usuario).
        // Comentar esta línea si se quieren permitir múltiples sesiones simultáneas.
        refreshTokenRepository.revokeAllUserTokens(user);

        // 4. Generar los nuevos tokens y persistir el refresh token.
        return generateAndSaveTokens(user);
    }

    /**
     * Registra un nuevo usuario en el sistema. Solo accesible por ADMIN.
     *
     * @param request datos del nuevo usuario
     * @return los tokens del nuevo usuario (login automático tras registro)
     * @throws IllegalArgumentException si el username ya existe
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso: " + request.username());
        }

        // Construir el usuario con la contraseña encriptada (nunca texto plano)
        User.UserBuilder<?, ?> userBuilder = User.builder()
            .username(request.username())
            .password(passwordEncoder.encode(request.password()))
            .role(request.role())
            .enabled(true);

        // Vincular al cliente si se proporcionó clientId
        if (request.clientId() != null) {
            var client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + request.clientId()));
            userBuilder.client(client);
        }

        // Vincular al mecánico si se proporcionó mechanicId
        if (request.mechanicId() != null) {
            var mechanic = mechanicRepository.findById(request.mechanicId())
                .orElseThrow(
                    () -> new ResourceNotFoundException("Mecánico no encontrado: " + request.mechanicId()));
            userBuilder.mechanic(mechanic);
        }

        User savedUser = userRepository.save(userBuilder.build());
        return generateAndSaveTokens(savedUser);
    }

    /**
     * Genera un nuevo access token a partir de un refresh token válido.
     *
     * <p>
     * Implementa "refresh token rotation": al usar el refresh token,
     * se revoca el antiguo y se emite uno nuevo. Esto detecta si un
     * refresh token robado ha sido utilizado (el usuario legítimo recibiría
     * un error al intentar refrescar con el token ya revocado).
     *
     * @param request con el refresh token
     * @return nuevos access token y refresh token
     * @throws IllegalArgumentException si el refresh token no es válido
     */
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new IllegalArgumentException("Refresh token no encontrado"));

        if (!storedToken.isValid()) {
            throw new IllegalArgumentException("Refresh token expirado o revocado");
        }

        User user = storedToken.getUser();

        // Token rotation: revocar el token usado y emitir uno nuevo
        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        return generateAndSaveTokens(user);
    }

    /**
     * Cierra la sesión del usuario revocando todos sus refresh tokens activos.
     *
     * @param username el nombre de usuario que cierra sesión
     */
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        refreshTokenRepository.revokeAllUserTokens(user);
    }

    // =========================================================================
    // MÉTODOS PRIVADOS
    // =========================================================================

    /**
     * Genera el access token JWT y un nuevo refresh token, persiste el refresh
     * token en BD y devuelve la respuesta completa de autenticación.
     *
     * @param user el usuario autenticado
     * @return los tokens generados
     */
    private AuthResponse generateAndSaveTokens(User user) {
        // Generar access token (JWT firmado, stateless)
        String accessToken = jwtService.generateAccessToken(user);

        // Generar refresh token (UUID aleatorio, stateful: guardado en BD)
        String rawRefreshToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
            .token(rawRefreshToken)
            .user(user)
            .revoked(false)
            .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()))
            .build();
        refreshTokenRepository.save(refreshToken);

        String role = user.getAuthorities().iterator().next().getAuthority();
        return new AuthResponse(accessToken, rawRefreshToken, role);
    }
}
