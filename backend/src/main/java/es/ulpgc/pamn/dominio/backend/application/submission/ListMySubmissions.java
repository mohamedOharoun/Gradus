package es.ulpgc.pamn.dominio.backend.application.submission;

import es.ulpgc.pamn.dominio.backend.domain.submission.Submission;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: las entregas de un alumno en un curso (docs/tareas/ejercicio-foto.md, paso 3c), para que la
 * app sepa qué ejercicios ya entregó. Sin los bytes de las fotos: listar no debe cargar fotos de 2 MB.
 *
 * <p>La más reciente primero (decisión I: cada envío es una entrega nueva y cuenta la última); a igual
 * instante, por id de mayor a menor, para que el orden sea siempre el mismo. El orden se decide aquí y no
 * en el repositorio: así la regla está en un solo sitio y se prueba sin base de datos.
 *
 * <p>Un curso que no existe da una lista vacía: no hay entregas, y así no se cuenta qué cursos existen.
 */
public final class ListMySubmissions {

    static final Comparator<Submission> NEWEST_FIRST =
            Comparator.comparing(Submission::submittedAt).thenComparing(Submission::id).reversed();

    private final SubmissionRepository submissions;

    public ListMySubmissions(SubmissionRepository submissions) {
        this.submissions = Objects.requireNonNull(submissions, "submissions");
    }

    /** {@code studentId} sale de la sesión: cada alumno solo ve las suyas (decisión H). */
    public List<Submission> list(UUID studentId, UUID courseId) {
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(courseId, "courseId");
        return submissions.mine(studentId, courseId).stream().sorted(NEWEST_FIRST).toList();
    }
}
