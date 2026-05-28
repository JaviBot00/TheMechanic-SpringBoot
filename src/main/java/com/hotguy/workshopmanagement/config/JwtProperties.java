package com.hotguy.workshopmanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Clase de configuración que lee las propiedades JWT del fichero
 * {@code application.yml}.
 *
 * <p>
 * {@code @ConfigurationProperties(prefix = "security.jwt")} indica a Spring
 * Boot
 * que lea todas las propiedades que empiecen por {@code security.jwt} y las
 * mapee
 * automáticamente a los campos de esta clase. Por ejemplo:
 * 
 * <pre>
 * security:
 *   jwt:
 *     secret-key: "mi-clave"          → secretKey
 *     access-token-expiration: 3600000 → accessTokenExpiration
 * </pre>
 *
 * <p>
 * Spring Boot convierte automáticamente el kebab-case del YAML
 * ({@code secret-key})
 * al camelCase de Java ({@code secretKey}).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * Clave secreta para firmar y verificar los tokens JWT.
     * En desarrollo viene del {@code application.yml}.
     * En producción debe venir de la variable de entorno
     * {@code SECURITY_JWT_SECRET_KEY}.
     * Mínimo 256 bits (32 caracteres) para el algoritmo HS256.
     */
    private String secretKey;

    /**
     * Duración del access token en milisegundos.
     * Por defecto: 3600000 ms = 1 hora.
     */
    private long accessTokenExpiration;

    /**
     * Duración del refresh token en milisegundos.
     * Por defecto: 604800000 ms = 7 días.
     */
    private long refreshTokenExpiration;
}
