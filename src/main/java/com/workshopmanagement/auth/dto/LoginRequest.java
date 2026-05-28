package com.workshopmanagement.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de petición para el endpoint de login.
 *
 * <p>Contiene las credenciales del usuario. Se validan automáticamente
 * con Bean Validation antes de llegar al Service.
 *
 * <p>Usamos {@code record} de Java 17+ en lugar de clase con Lombok porque
 * los records son inmutables por naturaleza y más concisos para DTOs simples.
 *
 * @param username nombre de usuario
 * @param password contraseña en texto plano (solo en tránsito HTTPS, nunca se persiste)
 */
public record LoginRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}
