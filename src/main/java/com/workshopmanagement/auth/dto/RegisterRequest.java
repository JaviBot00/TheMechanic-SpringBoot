package com.workshopmanagement.auth.dto;

import com.workshopmanagement.auth.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de petición para crear un nuevo usuario. Solo accesible por ADMIN.
 *
 * @param username   nombre de usuario único
 * @param password   contraseña (mínimo 8 caracteres)
 * @param role       rol asignado al nuevo usuario
 * @param clientId   ID del cliente a vincular (opcional, para rol CLIENT)
 * @param mechanicId ID del mecánico a vincular (opcional, para rol MECHANIC)
 */
public record RegisterRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,

        @NotNull(message = "El rol es obligatorio")
        Role role,

        Long clientId,

        Long mechanicId
) {}
