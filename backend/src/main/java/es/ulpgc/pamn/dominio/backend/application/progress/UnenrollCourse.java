package es.ulpgc.pamn.dominio.backend.application.progress;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: dar de baja a un usuario de un curso, borrando su inscripción y todos los módulos
 * dominados asociados en cascada. La operación es idempotente: si no había inscripción, no falla.
 */
public final class UnenrollCourse {

    private final EnrollmentRepository enrollments;

    public UnenrollCourse(EnrollmentRepository enrollments) {
        this.enrollments = Objects.requireNonNull(enrollments, "enrollments");
    }

    /** El usuario es siempre el de la sesión; nunca se lee del cuerpo. */
    public void unenroll(UUID userId, UUID courseId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(courseId, "courseId");
        enrollments.delete(userId, courseId);
    }
}
