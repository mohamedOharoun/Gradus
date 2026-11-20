package es.ulpgc.pamn.dominio.backend.application.course;

/**
 * Puerto de salida: convierte los bytes de un curso en sus records del dominio (capas 1–2, §3.1).
 * Sirve tanto para un curso recién recibido como para el contenido de una versión ya guardada, que se
 * lee solo con el lector y no con las reglas: así, si las reglas se endurecen, las versiones antiguas
 * se siguen pudiendo leer.
 */
public interface CourseReader {

    CourseReadResult read(byte[] body);
}
