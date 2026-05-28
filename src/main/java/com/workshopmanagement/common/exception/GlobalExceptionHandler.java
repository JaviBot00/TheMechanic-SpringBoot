package com.workshopmanagement.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para todos los controladores REST.
 *
 * <p>{@code @RestControllerAdvice}: intercepta las excepciones lanzadas en cualquier
 * {@code @RestController} y permite devolver respuestas HTTP apropiadas en lugar de
 * propagarlas como errores 500 genéricos.
 *
 * <p>Usamos {@link ProblemDetail} (RFC 9457), el estándar moderno de Spring Boot 3+
 * para respuestas de error en APIs REST. Devuelve JSON con estructura estandarizada:
 * <pre>
 * {
 *   "type": "https://workshopmanagement.com/errors/not-found",
 *   "title": "Not Found",
 *   "status": 404,
 *   "detail": "Cliente no encontrado: 42",
 *   "instance": "/api/v1/clients/42",
 *   "timestamp": "2026-01-15T10:30:00Z"
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja recursos no encontrados → 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso no encontrado");
        problem.setType(URI.create("https://workshopmanagement.com/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Maneja argumentos de negocio incorrectos → 400 Bad Request.
     * Ej: username ya en uso, operación no permitida en el estado actual.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Petición incorrecta");
        problem.setType(URI.create("https://workshopmanagement.com/errors/bad-request"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Maneja estados de negocio inválidos → 409 Conflict.
     * Ej: intentar cobrar una tarea no finalizada.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Operación no permitida");
        problem.setType(URI.create("https://workshopmanagement.com/errors/conflict"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Maneja fallos de validación Bean Validation → 400 Bad Request.
     * Se activa cuando un DTO con {@code @Valid} tiene campos inválidos.
     * Devuelve un mapa con los campos y sus mensajes de error.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Los datos enviados no son válidos"
        );
        problem.setTitle("Error de validación");
        problem.setType(URI.create("https://workshopmanagement.com/errors/validation"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    /**
     * Maneja intentos de acceso sin autenticación → 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Credenciales incorrectas o token inválido"
        );
        problem.setTitle("No autenticado");
        problem.setType(URI.create("https://workshopmanagement.com/errors/unauthorized"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Maneja intentos de acceso a recursos sin permiso suficiente → 403 Forbidden.
     * Ej: un CLIENT intentando acceder a un endpoint solo de ADMIN.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "No tiene permisos para realizar esta operación"
        );
        problem.setTitle("Acceso denegado");
        problem.setType(URI.create("https://workshopmanagement.com/errors/forbidden"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Manejador de última instancia: captura cualquier excepción no manejada → 500.
     * En producción, el mensaje de error interno NO debe exponerse al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Se ha producido un error interno. Contacte con el administrador."
        );
        problem.setTitle("Error interno del servidor");
        problem.setType(URI.create("https://workshopmanagement.com/errors/internal"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
