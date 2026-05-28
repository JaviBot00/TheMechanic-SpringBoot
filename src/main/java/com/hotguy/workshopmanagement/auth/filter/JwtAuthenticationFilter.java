package com.hotguy.workshopmanagement.auth.filter;

import com.hotguy.workshopmanagement.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro HTTP que intercepta cada petición para validar el token JWT.
 *
 * <p>
 * Extiende {@link OncePerRequestFilter}, que garantiza que el filtro
 * se ejecuta exactamente una vez por petición HTTP (algunos filtros pueden
 * ejecutarse varias veces en peticiones con forward/include).
 *
 * <p>
 * Flujo del filtro:
 * 
 * <pre>
 * Petición HTTP
 *   │
 *   ├── ¿Tiene header "Authorization: Bearer xxx"? → NO → dejar pasar (Spring Security denegará si el endpoint requiere auth)
 *   │
 *   ├── Extraer el token JWT del header
 *   │
 *   ├── ¿El token tiene username válido? → NO → dejar pasar
 *   │
 *   ├── ¿Ya hay autenticación en el SecurityContext? → SÍ → dejar pasar (ya autenticado)
 *   │
 *   ├── Cargar el usuario de la BD (UserDetailsService)
 *   │
 *   ├── ¿El token es válido para este usuario? → NO → dejar pasar (Spring Security denegará)
 *   │
 *   └── Establecer la autenticación en el SecurityContext → continuar
 * </pre>
 *
 * <p>
 * El {@code SecurityContext} es un almacenamiento por hilo (ThreadLocal) que
 * Spring Security usa para saber quién es el usuario autenticado durante
 * el procesamiento de la petición.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Lógica principal del filtro. Se ejecuta en cada petición HTTP.
     *
     * @param request     la petición HTTP entrante
     * @param response    la respuesta HTTP
     * @param filterChain la cadena de filtros (debemos llamar a {@code doFilter}
     *                    para continuar)
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Extraer el header de autorización
        final String authHeader = request.getHeader("Authorization");

        // Si no hay header o no empieza por "Bearer ", esta petición no lleva JWT.
        // Pasamos al siguiente filtro sin tocar el SecurityContext.
        // Si el endpoint requiere autenticación, Spring Security lo denegará después.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraer el token JWT (quitar el prefijo "Bearer ")
        final String jwt = authHeader.substring(7);

        // 3. Extraer el username del token. Si el token es inválido, jwtService lanzará
        // excepción
        // que capturamos para no propagar al cliente un error 500.
        final String username;
        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Token malformado o firma inválida: dejar pasar, Spring Security denegará
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Si el username es válido y el SecurityContext aún no tiene autenticación
        // (no se ha autenticado en un filtro anterior en esta petición)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 5. Cargar el usuario de la BD para verificar que existe y está activo
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 6. Validar el token contra el usuario cargado
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 7. Crear el objeto de autenticación de Spring Security
                // credentials = null porque ya no necesitamos la contraseña (el JWT es la
                // prueba)
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                // Añadir detalles de la petición (IP, session ID...) al objeto de autenticación
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 8. Guardar la autenticación en el SecurityContext del hilo actual
                // A partir de aquí, Spring Security sabe quién es el usuario
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 9. Continuar con el siguiente filtro en la cadena
        filterChain.doFilter(request, response);
    }
}
