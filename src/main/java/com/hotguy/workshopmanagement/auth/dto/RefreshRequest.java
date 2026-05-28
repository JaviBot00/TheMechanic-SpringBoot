package com.hotguy.workshopmanagement.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de petición para renovar el access token.
 *
 * @param refreshToken el refresh token obtenido en el login
 */
public record RefreshRequest(
    @NotBlank(message = "El refresh token es obligatorio") String refreshToken) {
}
