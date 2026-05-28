package com.hotguy.workshopmanagement.auth.service;

import com.hotguy.workshopmanagement.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio responsable de la generación y validación de tokens JWT (JSON Web
 * Token).
 *
 * <p>
 * Un JWT tiene tres partes separadas por puntos:
 * 
 * <pre>
 * header.payload.signature
 * eyJhbGci...  eyJzdWIi...  SflKxwRJ...
 * </pre>
 * <ul>
 * <li><b>Header</b>: algoritmo de firma (HS256) y tipo de token</li>
 * <li><b>Payload</b>: claims (datos): subject (username), rol, expiración,
 * etc.</li>
 * <li><b>Signature</b>: HMAC del header+payload con la clave secreta</li>
 * </ul>
 *
 * <p>
 * El token es <b>stateless</b>: el servidor no necesita guardarlo. Solo
 * necesita
 * la clave secreta para verificar que la firma es válida. Si alguien manipula
 * el
 * payload, la firma deja de coincidir y el token se rechaza.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Genera un access token JWT para el usuario dado.
     *
     * <p>
     * El token incluye como claims:
     * <ul>
     * <li>{@code sub}: username del usuario (subject estándar JWT)</li>
     * <li>{@code role}: rol del usuario (claim personalizado)</li>
     * <li>{@code iat}: fecha de emisión (issued at)</li>
     * <li>{@code exp}: fecha de expiración</li>
     * </ul>
     *
     * @param userDetails el usuario para el que se genera el token
     * @return el token JWT como String
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        // Añadimos el rol como claim personalizado para no tener que
        // consultar la BD en cada petición para conocer el rol del usuario.
        extraClaims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        return buildToken(extraClaims, userDetails, jwtProperties.getAccessTokenExpiration());
    }

    /**
     * Extrae el username (subject) del token JWT.
     *
     * @param token el token JWT
     * @return el username almacenado en el campo {@code sub}
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Verifica si el token es válido para el usuario dado.
     * Un token es válido si el username coincide y no ha expirado.
     *
     * @param token       el token JWT a validar
     * @param userDetails el usuario contra el que validar
     * @return {@code true} si el token es válido
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Token malformado, firma inválida, etc.
            return false;
        }
    }

    /**
     * Comprueba si el token ha superado su fecha de expiración.
     *
     * @param token el token JWT
     * @return {@code true} si el token ha expirado
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // =========================================================================
    // MÉTODOS PRIVADOS
    // =========================================================================

    /**
     * Construye y firma un token JWT con los claims y duración indicados.
     *
     * @param extraClaims claims adicionales a incluir en el payload
     * @param userDetails datos del usuario
     * @param expiration  duración en milisegundos
     * @return token JWT firmado
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrae un claim genérico del token aplicando la función indicada.
     * Patrón funcional para reutilizar la lógica de parseo del token.
     *
     * @param token          el token JWT
     * @param claimsResolver función que extrae el claim deseado
     * @param <T>            tipo del claim
     * @return el valor del claim
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parsea y verifica la firma del token, devolviendo todos sus claims.
     * Lanza {@link JwtException} si el token es inválido o la firma no coincide.
     *
     * @param token el token JWT
     * @return todos los claims del payload
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Extrae la fecha de expiración del token. */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Deriva la clave de firma HMAC-SHA256 a partir del secreto configurado.
     * La clave se genera en cada llamada (no se cachea) para simplificar,
     * ya que el coste es mínimo y así la clave nunca queda en memoria estática.
     *
     * @return clave criptográfica para firmar/verificar JWT
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
