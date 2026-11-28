package es.ulpgc.pamn.dominio.backend.application.submission;

import java.util.Objects;
import java.util.UUID;

/**
 * Lo que llega para entregar una foto. {@code studentId} sale de la sesión, nunca del cliente;
 * {@code photo} son los bytes tal como llegaron (puede ser {@code null} si faltaba el campo).
 */
public record SubmitPhotoCommand(UUID studentId, UUID courseId, int courseVersion, String exerciseId, byte[] photo) {

    public SubmitPhotoCommand {
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(courseId, "courseId");
        Objects.requireNonNull(exerciseId, "exerciseId");
    }

    /** Sin los bytes: una foto de 2 MB no debe acabar en los logs. */
    @Override
    public String toString() {
        return "SubmitPhotoCommand[studentId=" + studentId + ", courseId=" + courseId + ", courseVersion="
                + courseVersion + ", exerciseId=" + exerciseId + ", photo="
                + (photo == null ? "ninguna" : photo.length + " bytes") + "]";
    }
}
