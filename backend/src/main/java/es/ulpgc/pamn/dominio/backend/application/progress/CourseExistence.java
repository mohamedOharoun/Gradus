package es.ulpgc.pamn.dominio.backend.application.progress;

import java.util.Set;
import java.util.UUID;

/**
 * Puerto de salida de solo lectura: qué cursos existen. Es una interfaz aparte de {@code
 * CourseRepository} (segregación de interfaces): aquí solo hacen falta los ids, sin cargar contenido.
 */
public interface CourseExistence {

    /** Devuelve, de los ids pedidos, los que existen. Una sola consulta por petición. */
    Set<UUID> findExisting(Set<UUID> courseIds);
}
