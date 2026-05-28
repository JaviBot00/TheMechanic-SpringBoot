package com.workshopmanagement.auth.controller;

import com.workshopmanagement.auth.dto.AuthResponse;
import com.workshopmanagement.auth.dto.LoginRequest;
import com.workshopmanagement.auth.dto.RefreshRequest;
import com.workshopmanagement.auth.dto.RegisterRequest;
import com.workshopmanagement.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para los endpoints de autenticación.
 *
 * <p>{@code @RestController}: combina {@code @Controller} y {@code @ResponseBody}.
 * Todos los métodos devuelven directamente el objeto que se serializa a JSON,
 * sin necesidad de añadir {@code @ResponseBody} en cada método.
 *
 * <p>{@code @RequestMapping("/api/v1/auth")}: prefijo común para todos los endpoints de este controlador.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de login, registro, refresco y logout")
public class AuthController {

    private final AuthService authService;

    /**
     * Autentica un usuario y devuelve los tokens JWT.
     *
     * <p>{@code @Valid}: activa Bean Validation sobre el DTO de entrada.
     * Si alguna validación falla (ej. username en blanco), Spring devuelve
     * automáticamente un 400 Bad Request con los mensajes de error.
     *
     * @param request credenciales del usuario
     * @return 200 OK con access token + refresh token
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica al usuario y devuelve los tokens JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Registra un nuevo usuario. Solo accesible por ADMIN.
     * La autorización por rol se verifica en {@link com.workshopmanagement.config.SecurityConfig}.
     *
     * @param request datos del nuevo usuario
     * @return 201 Created con los tokens del nuevo usuario
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar usuario", description = "Crea una nueva cuenta de usuario (solo ADMIN)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Renueva el access token usando un refresh token válido.
     *
     * @param request con el refresh token
     * @return 200 OK con nuevos access token y refresh token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Renovar token", description = "Genera un nuevo access token a partir del refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Cierra la sesión del usuario autenticado, revocando todos sus refresh tokens.
     *
     * <p>{@code @AuthenticationPrincipal}: inyecta el {@code UserDetails} del usuario
     * autenticado en la petición actual, extraído del SecurityContext por Spring Security.
     * No es necesario parsear el token manualmente.
     *
     * @param userDetails el usuario autenticado (inyectado por Spring Security)
     * @return 204 No Content (operación exitosa sin cuerpo de respuesta)
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoca los tokens de sesión del usuario actual")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
