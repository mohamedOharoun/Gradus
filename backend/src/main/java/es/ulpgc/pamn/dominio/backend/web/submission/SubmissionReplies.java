package es.ulpgc.pamn.dominio.backend.web.submission;

import es.ulpgc.pamn.dominio.backend.application.submission.SubmitPhotoResult;
import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import es.ulpgc.pamn.dominio.backend.domain.submission.JpegCheck;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.web.ErrorResponse;
import io.javalin.http.HttpStatus;

/**
 * Qué responder en cada caso de las rutas de entregas (docs/tareas/ejercicio-foto.md, contrato de la API y
 * decisión P). Funciones puras: no tocan el {@code Context}, así que cada código tiene su test.
 */
final class SubmissionReplies {

    private SubmissionReplies() {
    }

    /** {@code POST …/submissions}: un {@code switch} sobre el resultado sellado; si aparece otro, no compila. */
    static JsonReply of(SubmitPhotoResult result) {
        return switch (result) {
            case SubmitPhotoResult.Submitted submitted ->
                    new JsonReply(HttpStatus.CREATED, SubmissionResponse.from(submitted.submission()));
            case SubmitPhotoResult.CourseNotFound notFound -> courseNotFound();
            case SubmitPhotoResult.NotAPhotoExercise notPhoto -> error(HttpStatus.NOT_FOUND, "exerciseId",
                    ErrorCode.EXERCISE_NOT_FOUND, "En esa versión del curso no hay un ejercicio con foto con ese id.");
            case SubmitPhotoResult.InvalidPhoto invalid -> invalidPhoto(invalid.reason());
        };
    }

    private static JsonReply invalidPhoto(JpegCheck reason) {
        return switch (reason) {
            case JpegCheck.Empty empty -> error(HttpStatus.BAD_REQUEST, "photo", ErrorCode.REQUIRED,
                    "Falta la foto: envíala como fichero en el campo «photo».");
            case JpegCheck.NotJpeg notJpeg -> error(HttpStatus.BAD_REQUEST, "photo", ErrorCode.INVALID_PHOTO,
                    "La foto tiene que ser un JPEG.");
            case JpegCheck.TooLarge tooLarge -> error(HttpStatus.CONTENT_TOO_LARGE, "photo", ErrorCode.PHOTO_TOO_LARGE,
                    "La foto ocupa " + tooLarge.size() + " bytes; el máximo es " + Jpeg.MAX_BYTES + ".");
            // SubmitPhotoResult.InvalidPhoto no se puede crear con una foto válida (lo comprueba su constructor).
            case JpegCheck.Valid valid -> throw new IllegalArgumentException("Una foto válida no es un rechazo");
        };
    }

    /** Decisión O: el {@code Content-Length} ya pasa del límite y la petición no se llega a leer. */
    static JsonReply requestTooLarge() {
        return error(HttpStatus.CONTENT_TOO_LARGE, "photo", ErrorCode.PHOTO_TOO_LARGE,
                "La petición pasa de " + UploadLimit.MAX_REQUEST_BYTES + " bytes; la foto puede ocupar como mucho "
                        + Jpeg.MAX_BYTES + ".");
    }

    static JsonReply missingVersion() {
        return error(HttpStatus.BAD_REQUEST, "version", ErrorCode.REQUIRED,
                "Falta la versión del curso: envíala en el campo «version».");
    }

    static JsonReply invalidVersion() {
        return error(HttpStatus.BAD_REQUEST, "version", ErrorCode.INVALID_PARAMETER,
                "La versión tiene que ser un número entero positivo.");
    }

    /** El curso o la versión no existen, o el {@code {id}} de la ruta no es un UUID. */
    static JsonReply courseNotFound() {
        return error(HttpStatus.NOT_FOUND, "id", ErrorCode.COURSE_NOT_FOUND, "No existe ese curso o esa versión.");
    }

    /** {@code GET /api/submissions/mine} sin {@code ?courseId=}. */
    static JsonReply missingCourseId() {
        return error(HttpStatus.BAD_REQUEST, "courseId", ErrorCode.REQUIRED,
                "Falta el curso: añade ?courseId=… a la ruta.");
    }

    static JsonReply invalidCourseId() {
        return error(HttpStatus.BAD_REQUEST, "courseId", ErrorCode.INVALID_PARAMETER,
                "El curso tiene que ser un UUID.");
    }

    /** La entrega no existe, o el {@code {id}} de la ruta no es un UUID. */
    static JsonReply submissionNotFound() {
        return error(HttpStatus.NOT_FOUND, "id", ErrorCode.SUBMISSION_NOT_FOUND, "No existe esa entrega.");
    }

    /** Decisión H: la foto solo la descarga quien la subió. */
    static JsonReply notOwner() {
        return error(HttpStatus.FORBIDDEN, "id", ErrorCode.NOT_SUBMISSION_OWNER,
                "Esa entrega es de otra cuenta: solo la puede ver quien la subió.");
    }

    private static JsonReply error(HttpStatus status, String field, ErrorCode code, String message) {
        return new JsonReply(status, ErrorResponse.of(field, code, message));
    }
}
