package com.workshopmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Punto de entrada de la aplicación Workshop Management API.
 *
 * <p>{@code @SpringBootApplication} es una anotación compuesta que activa tres cosas:
 * <ul>
 *   <li>{@code @SpringBootConfiguration}: marca esta clase como fuente de configuración</li>
 *   <li>{@code @EnableAutoConfiguration}: activa la auto-configuración de Spring Boot
 *       (Tomcat, JPA, Security, etc.) basándose en las dependencias del classpath</li>
 *   <li>{@code @ComponentScan}: escanea el paquete actual y subpaquetes en busca de
 *       componentes Spring (@Component, @Service, @Repository, @Controller, etc.)</li>
 * </ul>
 *
 * <p>{@code @EnableJpaAuditing} activa el sistema de auditoría de Spring Data JPA,
 * que permite rellenar automáticamente campos como {@code createdAt} y {@code updatedAt}
 * en las entidades anotadas con {@code @EntityListeners(AuditingEntityListener.class)}.
 */
@SpringBootApplication
@EnableJpaAuditing
public class WorkshopManagementApplication {

    /**
     * Método principal que arranca el contexto de Spring Boot.
     *
     * <p>Internamente, {@code SpringApplication.run()} realiza los siguientes pasos:
     * <ol>
     *   <li>Crea un {@code ApplicationContext} (contenedor de beans)</li>
     *   <li>Registra todos los beans encontrados por el component scan</li>
     *   <li>Ejecuta las auto-configuraciones aplicables</li>
     *   <li>Arranca el servidor Tomcat embebido</li>
     *   <li>Ejecuta las migraciones Flyway contra la base de datos</li>
     * </ol>
     *
     * @param args argumentos de línea de comandos (p.ej. --spring.profiles.active=dev)
     */
    public static void main(String[] args) {
        SpringApplication.run(WorkshopManagementApplication.class, args);
    }
}
