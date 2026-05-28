package com.workshopmanagement.client.dto;

import jakarta.validation.constraints.*;

/**
 * DTO de petición para crear o actualizar un cliente.
 *
 * <p>Las anotaciones de Bean Validation se evalúan automáticamente
 * cuando el Controller recibe una petición con {@code @Valid}.
 *
 * @param clientCode código de cliente (debe ser positivo)
 * @param name       nombre de pila
 * @param surname1   primer apellido
 * @param surname2   segundo apellido (opcional)
 * @param nif        NIF en formato estándar español
 * @param email      dirección de correo electrónico
 * @param telephone  número de teléfono (opcional)
 */
public record ClientRequest(
        @NotNull(message = "El código de cliente es obligatorio")
        @Positive(message = "El código de cliente debe ser un número positivo")
        Integer clientCode,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @NotBlank(message = "El primer apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String surname1,

        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String surname2,

        @NotBlank(message = "El NIF es obligatorio")
        @Pattern(regexp = "^[0-9]{8}[A-Z]$", message = "El NIF debe tener 8 dígitos seguidos de una letra mayúscula")
        String nif,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email no es válido")
        String email,

        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String telephone
) {}
