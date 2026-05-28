package com.workshopmanagement.client.dto;

import java.time.Instant;

/**
 * DTO de respuesta para operaciones sobre clientes.
 *
 * <p>Solo expone los campos seguros: no incluye campos internos de JPA
 * ({@code deletedAt}) ni referencias circulares ({@code vehicles} completos).
 * El número de vehículos se expone como contador para evitar cargas innecesarias.
 *
 * @param id           identificador técnico interno
 * @param clientCode   código de cliente de negocio
 * @param name         nombre de pila
 * @param surname1     primer apellido
 * @param surname2     segundo apellido
 * @param nif          NIF
 * @param email        email de contacto
 * @param telephone    teléfono de contacto
 * @param vehicleCount número de vehículos activos del cliente
 * @param createdAt    fecha de alta en el sistema
 * @param updatedAt    fecha de última modificación
 */
public record ClientResponse(
        Long id,
        Integer clientCode,
        String name,
        String surname1,
        String surname2,
        String nif,
        String email,
        String telephone,
        int vehicleCount,
        Instant createdAt,
        Instant updatedAt
) {}
