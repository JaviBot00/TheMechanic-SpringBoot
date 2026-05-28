package com.hotguy.workshopmanagement.auth.dto;

/**
 * DTO de respuesta para los endpoints de login y refresh.
 *
 * <p>
 * Devuelve los dos tokens necesarios para el cliente:
 * <ul>
 * <li>{@code accessToken}: JWT de corta duración (1h) que se envía en cada
 * request</li>
 * <li>{@code refreshToken}: token opaco de larga duración (7 días) para renovar
 * el access token</li>
 * </ul>
 *
 * @param accessToken  el JWT para autenticar las peticiones
 * @param refreshToken el token para solicitar nuevos access tokens
 * @param tokenType    siempre "Bearer" (estándar OAuth2/JWT)
 * @param role         rol del usuario para que el frontend adapte la UI
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        String role) {
    /**
     * Constructor de conveniencia que establece el tipo de token como "Bearer".
     */
    public AuthResponse(String accessToken, String refreshToken, String role) {
        this(accessToken, refreshToken, "Bearer", role);
    }
}
