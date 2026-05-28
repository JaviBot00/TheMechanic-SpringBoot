package com.workshopmanagement.auth.model;

/**
 * Roles de usuario disponibles en el sistema.
 *
 * <p>Spring Security utiliza estos roles para controlar el acceso a los
 * endpoints. La convención de Spring es prefijar los roles con "ROLE_",
 * pero al usar {@code hasRole("ADMIN")} en la configuración de seguridad,
 * Spring añade el prefijo automáticamente.
 *
 * <p>Los roles definen niveles de acceso:
 * <ul>
 *   <li>{@code ADMIN}: acceso total, gestión de usuarios</li>
 *   <li>{@code MECHANIC}: gestión de tareas y vehículos, sin gestión de usuarios</li>
 *   <li>{@code CLIENT}: acceso de solo lectura a sus propios datos</li>
 * </ul>
 */
public enum Role {

    /**
     * Administrador del sistema.
     * Puede realizar cualquier operación, incluyendo la creación
     * y gestión de cuentas de usuario.
     */
    ADMIN,

    /**
     * Mecánico del taller.
     * Puede gestionar tareas, vehículos y consultar clientes,
     * pero no puede gestionar usuarios ni marcar pagos.
     */
    MECHANIC,

    /**
     * Cliente del taller.
     * Solo puede consultar sus propios datos: vehículos y tareas.
     */
    CLIENT
}
