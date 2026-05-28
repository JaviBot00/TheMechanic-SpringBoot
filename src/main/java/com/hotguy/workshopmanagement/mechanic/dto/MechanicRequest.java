package com.hotguy.workshopmanagement.mechanic.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * DTO de petición para crear o actualizar un mecánico.
 *
 * @param name             nombre de pila
 * @param surname1         primer apellido
 * @param surname2         segundo apellido (opcional)
 * @param nif              NIF del mecánico
 * @param email            correo electrónico
 * @param telephone        teléfono de contacto
 * @param registrationDate fecha de incorporación al taller
 * @param specialty        especialidad principal
 */
public record MechanicRequest(
    @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String name,

    @NotBlank(message = "El primer apellido es obligatorio") @Size(max = 100) String surname1,

    @Size(max = 100) String surname2,

    @NotBlank(message = "El NIF es obligatorio") @Pattern(regexp = "^[0-9]{8}[A-Z]$", message = "El NIF debe tener 8 dígitos y una letra mayúscula") String nif,

    @NotBlank(message = "El email es obligatorio") @Email String email,

    @Size(max = 20) String telephone,

    @NotNull(message = "La fecha de registro es obligatoria") @PastOrPresent(message = "La fecha de registro no puede ser futura") LocalDate registrationDate,

    @NotBlank(message = "La especialidad es obligatoria") @Size(max = 100) String specialty) {
}
