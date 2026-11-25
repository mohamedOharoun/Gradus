package es.ulpgc.pamn.dominio.backend.web.submission;

import es.ulpgc.pamn.dominio.backend.application.submission.SubmitPhotoResult;
import es.ulpgc.pamn.dominio.backend.domain.submission.JpegCheck;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.domain.validation.ValidationError;
import es.ulpgc.pamn.dominio.backend.web.ErrorResponse;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Qué código HTTP y qué error corresponde a cada desenlace (docs/tareas/ejercicio-foto.md, contrato y decisión P). */
class SubmissionRepliesTest {

    /** El único error de la respuesta; falla si no es un {@link ErrorResponse} con exactamente uno. */
    private static ValidationError onlyError(JsonReply reply) {
        ErrorResponse body = assertInstanceOf(ErrorResponse.class, reply.body());
        assertEquals(1, body.errors().size());
        return body.errors().getFirst();
    }

    private static void assertError(JsonReply reply, HttpStatus status, String field, ErrorCode code) {
        assertEquals(status, reply.status());
        ValidationError error = onlyError(reply);
        assertEquals(field, error.field());
        assertEquals(code, error.code());
        assertTrue(!error.message().isBlank(), "el mensaje ayuda a quien prueba con Yaak");
    }

    // --- Entregar ---

    @Test
    void aSubmittedPhotoIsCreatedWithItsSubmission() {
        var submission = SubmissionResponsesTest.submission(1, 300);
        JsonReply reply = SubmissionReplies.of(new SubmitPhotoResult.Submitted(submission));

        assertEquals(HttpStatus.CREATED, reply.status());
        assertEquals(SubmissionResponse.from(submission), reply.body());
    }

    @Test
    void anUnknownCourseOrVersionIsNotFound() {
        assertError(SubmissionReplies.of(new SubmitPhotoResult.CourseNotFound()),
                HttpStatus.NOT_FOUND, "id", ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void anExerciseThatIsNotAPhotoStepIsNotFound() {
        assertError(SubmissionReplies.of(new SubmitPhotoResult.NotAPhotoExercise()),
                HttpStatus.NOT_FOUND, "exerciseId", ErrorCode.EXERCISE_NOT_FOUND);
    }

    // Falta el campo «photo» (o llegó vacío, o como texto en vez de fichero): es un campo obligatorio.
    @Test
    void aMissingPhotoIsRequired() {
        assertError(SubmissionReplies.of(new SubmitPhotoResult.InvalidPhoto(new JpegCheck.Empty())),
                HttpStatus.BAD_REQUEST, "photo", ErrorCode.REQUIRED);
    }

    @Test
    void somethingThatIsNotAJpegIsAnInvalidPhoto() {
        assertError(SubmissionReplies.of(new SubmitPhotoResult.InvalidPhoto(new JpegCheck.NotJpeg())),
                HttpStatus.BAD_REQUEST, "photo", ErrorCode.INVALID_PHOTO);
    }

    @Test
    void aPhotoOverTheMaximumIsTooLarge() {
        JsonReply reply = SubmissionReplies.of(new SubmitPhotoResult.InvalidPhoto(new JpegCheck.TooLarge(2_000_001)));

        assertError(reply, HttpStatus.CONTENT_TOO_LARGE, "photo", ErrorCode.PHOTO_TOO_LARGE);
        // El mensaje dice cuánto ocupa y cuánto se permite, para que quien prueba sepa por cuánto se pasa.
        assertTrue(onlyError(reply).message().contains("2000001"));
        assertTrue(onlyError(reply).message().contains("2000000"));
    }

    // Decisión O: la petición entera pasa del límite (lo dice su Content-Length) y no se llega a leer.
    @Test
    void aRequestOverTheLimitIsTooLarge() {
        assertError(SubmissionReplies.requestTooLarge(), HttpStatus.CONTENT_TOO_LARGE, "photo",
                ErrorCode.PHOTO_TOO_LARGE);
    }

    @Test
    void aMissingVersionIsRequired() {
        assertError(SubmissionReplies.missingVersion(), HttpStatus.BAD_REQUEST, "version", ErrorCode.REQUIRED);
    }

    @Test
    void aVersionThatIsNotAPositiveNumberIsAnInvalidParameter() {
        assertError(SubmissionReplies.invalidVersion(), HttpStatus.BAD_REQUEST, "version", ErrorCode.INVALID_PARAMETER);
    }

    // Un {id} que no es un UUID no identifica ningún curso: 404, como en las rutas de cursos.
    @Test
    void aCourseIdThatIsNotAUuidIsNotFound() {
        assertError(SubmissionReplies.courseNotFound(), HttpStatus.NOT_FOUND, "id", ErrorCode.COURSE_NOT_FOUND);
    }

    // --- Listar las propias ---

    // En la consulta es distinto: courseId es un parámetro obligatorio, no una ruta. Falta o no vale → 400.
    @Test
    void aMissingCourseIdInTheQueryIsRequired() {
        assertError(SubmissionReplies.missingCourseId(), HttpStatus.BAD_REQUEST, "courseId", ErrorCode.REQUIRED);
    }

    @Test
    void aCourseIdInTheQueryThatIsNotAUuidIsAnInvalidParameter() {
        assertError(SubmissionReplies.invalidCourseId(), HttpStatus.BAD_REQUEST, "courseId",
                ErrorCode.INVALID_PARAMETER);
    }

    // --- Descargar la foto ---

    @Test
    void aSubmissionThatDoesNotExistIsNotFound() {
        assertError(SubmissionReplies.submissionNotFound(), HttpStatus.NOT_FOUND, "id", ErrorCode.SUBMISSION_NOT_FOUND);
    }

    // Decisión H: solo el alumno que la subió.
    @Test
    void someoneElsesSubmissionIsForbidden() {
        assertError(SubmissionReplies.notOwner(), HttpStatus.FORBIDDEN, "id", ErrorCode.NOT_SUBMISSION_OWNER);
    }

    // --- La respuesta en sí ---

    @Test
    void aReplyNeedsAStatusAndABody() {
        assertThrows(NullPointerException.class, () -> new JsonReply(null, "x"));
        assertThrows(NullPointerException.class, () -> new JsonReply(HttpStatus.OK, null));
    }
}
