package com.hotguy.workshopmanagement.config;

import com.hotguy.workshopmanagement.auth.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Configuración central de Spring Security.
 *
 * <p>
 * {@code @EnableWebSecurity}: activa la integración de Spring Security con
 * Spring MVC.
 * <p>
 * {@code @EnableMethodSecurity}: activa las anotaciones de seguridad a nivel de
 * método
 * ({@code @PreAuthorize}, {@code @PostAuthorize}), que usaremos en los
 * Controllers y Services
 * para control de acceso más granular.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Configura la cadena de filtros de seguridad HTTP.
     *
     * <p>
     * Este es el método más importante de toda la configuración de seguridad.
     * Define qué endpoints son públicos, cuáles requieren autenticación, y
     * en qué orden se aplican los filtros.
     *
     * @param http el builder de configuración HTTP de Spring Security
     * @return la cadena de filtros configurada
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Desactivar CSRF (Cross-Site Request Forgery).
            // CSRF es necesario para apps web con sesiones y cookies.
            // En APIs REST con JWT no es necesario porque no usamos cookies de sesión.
            .csrf(AbstractHttpConfigurer::disable)

            // Configurar las reglas de autorización por URL
            .authorizeHttpRequests(auth -> auth

                // Endpoints públicos: no requieren token JWT
                .requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh")
                .permitAll()

                // Consola H2 (solo dev, pero la incluimos aquí para no complicar
                // perfiles)
                .requestMatchers("/h2-console/**").permitAll()

                // Swagger UI y documentación OpenAPI
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs",
                    "/api-docs/**")
                .permitAll()

                // Actuator health e info (monitorización sin autenticación)
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // Gestión de usuarios: solo ADMIN
                .requestMatchers("/api/v1/auth/register").hasRole("ADMIN")

                //
                .requestMatchers("/api/v1/auth/logout").authenticated()

                //
                .requestMatchers("/api/v1/vehicles/**").hasAnyRole("ADMIN", "MECHANIC", "CLIENT")

                // Reportes: ADMIN y MECHANIC
                .requestMatchers("/api/v1/reports/**").hasAnyRole("ADMIN", "MECHANIC")

                // Marcar pago: solo ADMIN
                .requestMatchers(HttpMethod.PATCH, "/api/v1/tasks/*/pay")
                .hasRole("ADMIN")

                // El resto de endpoints autenticados se controlan con @PreAuthorize en
                // los controllers
                .anyRequest().authenticated())

            // Política de sesiones: STATELESS.
            // Spring Security NO crea ni usa sesiones HTTP (HttpSession).
            // Cada petición se autentica de nuevo con el JWT.
            // Esto es fundamental para APIs REST escalables.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Registrar nuestro AuthenticationProvider (con BCrypt + UserDetailsService)
            .authenticationProvider(authenticationProvider())

            // Insertar nuestro filtro JWT ANTES del filtro estándar de username/password.
            // El orden importa: nuestro filtro debe ejecutarse primero para establecer
            // la autenticación en el SecurityContext antes de que Spring Security la
            // compruebe.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // Configurar las cabeceras HTTP para permitir la consola H2 en un iframe (solo
            // dev)
            .headers(headers -> headers
                .frameOptions(frame -> {
                    String profile = System.getProperty("spring.profiles.active", "");
                    if (profile.contains("dev")) {
                        frame.sameOrigin();
                    } else {
                        frame.deny();
                    }
                }))

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
            )

            .build();
    }

    /**
     * Configura el proveedor de autenticación DAO (Database Authentication Object).
     *
     * <p>
     * Este bean conecta:
     * <ul>
     * <li>{@code UserDetailsService}: cómo cargar el usuario de la BD</li>
     * <li>{@code PasswordEncoder}: cómo verificar la contraseña</li>
     * </ul>
     *
     * <p>
     * Spring Security lo usa internamente en el {@code AuthenticationManager}
     * cuando se llama a {@code authenticationManager.authenticate()}.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        // provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expone el {@code AuthenticationManager} como bean de Spring.
     *
     * <p>
     * El {@code AuthenticationManager} es el componente central que coordina
     * la autenticación. Lo necesitamos inyectado en {@code AuthService} para
     * llamar a {@code authenticate()} en el proceso de login.
     *
     * @param config la configuración de autenticación de Spring (auto-configurada)
     * @return el AuthenticationManager configurado
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
        throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Define el algoritmo de hash de contraseñas.
     *
     * <p>
     * BCrypt es el estándar de la industria para hashear contraseñas:
     * <ul>
     * <li>Incluye un salt aleatorio (evita ataques de rainbow table)</li>
     * <li>Es deliberadamente lento (configurable, cost=10 por defecto)</li>
     * <li>El mismo texto plano siempre produce hashes distintos</li>
     * </ul>
     *
     * <p>
     * NUNCA almacenar contraseñas en texto plano ni con MD5/SHA1.
     *
     * @return el encoder BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        System.out.println("Password encriptada: " + new BCryptPasswordEncoder().encode("password123"));
        return new BCryptPasswordEncoder();
    }
}
