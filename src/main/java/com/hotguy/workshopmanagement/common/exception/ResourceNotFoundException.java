package com.hotguy.workshopmanagement.common.exception;

/**
 * Excepción lanzada cuando no se encuentra un recurso solicitado.
 * Equivalente a una respuesta HTTP 404 Not Found.
 *
 * <p>
 * Extiende {@code RuntimeException} (unchecked) para no obligar a los
 * callers a capturarla explícitamente. El {@link GlobalExceptionHandler}
 * la captura globalmente y devuelve el 404 adecuado.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * @param message descripción del recurso no encontrado (ej. "Cliente no
     *                encontrado: 42")
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
