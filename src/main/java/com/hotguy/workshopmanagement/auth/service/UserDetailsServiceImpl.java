package com.hotguy.workshopmanagement.auth.service;

import com.hotguy.workshopmanagement.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link UserDetailsService} que carga los datos del usuario
 * desde la base de datos.
 *
 * <p>
 * Spring Security llama a este servicio durante el proceso de autenticación
 * para obtener los datos del usuario (credenciales, roles, estado de la
 * cuenta).
 * Es el puente entre Spring Security y nuestra BD.
 *
 * <p>
 * Al inyectarse en {@code SecurityConfig} como bean, Spring Security lo usa
 * automáticamente en el {@code AuthenticationManager}.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Carga el usuario por su nombre de usuario.
     *
     * <p>
     * {@code @Transactional(readOnly = true)}: operación de solo lectura.
     * Esto permite a la BD optimizar la query (sin bloqueos de escritura)
     * y a Hibernate no hacer flush del contexto de persistencia.
     *
     * @param username el nombre de usuario a buscar
     * @return el {@link UserDetails} del usuario (nuestra entidad {@code User} lo
     * implementa)
     * @throws UsernameNotFoundException si no existe el usuario
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Usuario no encontrado con username: " + username));
    }
}
