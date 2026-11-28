package es.ulpgc.pamn.dominio.backend.domain.course;

/**
 * Un paso de la parte formativa de un módulo: una lección o un ejercicio. Está sellada, así que un
 * {@code switch} sobre un {@code Step} con los cuatro casos no necesita {@code default}, y el
 * compilador avisa si algún día se añade un tipo nuevo y alguien olvida tratarlo.
 */
public sealed interface Step permits Lesson, Exercise {

    /** Id único en todo el curso, compartido con módulos y ejercicios (§2.4). */
    String id();
}
