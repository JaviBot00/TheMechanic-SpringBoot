package com.workshopmanagement.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de SpringDoc OpenAPI para la documentación automática de la API.
 *
 * <p>{@code @OpenAPIDefinition}: define los metadatos globales de la API
 * (título, versión, contacto) y los requisitos de seguridad globales.
 *
 * <p>{@code @SecurityScheme}: define el esquema de autenticación JWT Bearer
 * que aparecerá en el botón "Authorize" de Swagger UI. Permite probar
 * los endpoints protegidos directamente desde la documentación.
 *
 * <p>Con esta configuración, Swagger UI mostrará un candado en cada endpoint
 * protegido y permitirá introducir el token JWT para probarlo.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Workshop Management API",
                version = "1.0.0",
                description = "API REST para la gestión integral de un taller mecánico. " +
                        "Incluye gestión de clientes, vehículos, mecánicos y órdenes de trabajo " +
                        "con autenticación JWT y control de acceso por roles.",
                contact = @Contact(
                        name = "Workshop Management",
                        email = "admin@workshopmanagement.com"
                )
        ),
        security = @SecurityRequirement(name = "Bearer Authentication")
)
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Introduce el token JWT obtenido en /api/v1/auth/login. " +
                "Formato: Bearer {token}"
)
public class OpenApiConfig {
    // La configuración se hace completamente mediante anotaciones.
    // No se necesita código adicional en el cuerpo de la clase.
}
