package es.ulpgc.pamn.dominio.backend.domain.course;

/**
 * Ejercicio con foto: el alumno lo resuelve en papel y entrega una foto, que revisará el profesor
 * (docs/tareas/ejercicio-foto.md). No se corrige solo, así que no tiene respuesta en el fichero:
 * {@code explanation} es la solución que ve el alumno al entregar.
 */
public record PhotoExercise(String id, String prompt, String explanation) implements Exercise {

    public PhotoExercise {
        Require.exerciseFields(id, prompt, explanation);
    }
}
