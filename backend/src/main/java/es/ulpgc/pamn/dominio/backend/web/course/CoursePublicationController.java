package es.ulpgc.pamn.dominio.backend.web.course;

import es.ulpgc.pamn.dominio.backend.application.AuthenticationResult;
import es.ulpgc.pamn.dominio.backend.application.course.CourseValidationResult;
import es.ulpgc.pamn.dominio.backend.application.course.Publication;
import es.ulpgc.pamn.dominio.backend.application.course.PublishCourse;
import es.ulpgc.pamn.dominio.backend.application.course.PublishCourseVersion;
import es.ulpgc.pamn.dominio.backend.application.course.ValidateCourse;
import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseAuthor;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseReport;
import es.ulpgc.pamn.dominio.backend.domain.course.SizeLimit;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.web.ErrorResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import io.javalin.http.HttpStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Traduce HTTP ↔ casos de uso de validar y publicar (§4.3). Sin reglas de negocio: lee el cuerpo como bytes
 * (nunca con {@code bodyAsClass}) y elige la respuesta con un {@code switch} sobre cada resultado sellado.
 * Las tres rutas están protegidas con {@code RequireSession}: el autor es siempre el usuario de la sesión.
 */
public final class CoursePublicationController {

    private final ValidateCourse validateCourse;
    private final PublishCourse publishCourse;
    private final PublishCourseVersion publishCourseVersion;

    public CoursePublicationController(ValidateCourse validateCourse, PublishCourse publishCourse,
                                       PublishCourseVersion publishCourseVersion) {
        this.validateCourse = Objects.requireNonNull(validateCourse, "validateCourse");
        this.publishCourse = Objects.requireNonNull(publishCourse, "publishCourse");
        this.publishCourseVersion = Objects.requireNonNull(publishCourseVersion, "publishCourseVersion");
    }

    /** {@code POST /api/courses/validate}: 200 con el informe, sea válido o no; 413. */
    public void validate(Context ctx, AuthenticationResult.Authenticated authenticated) {
        Optional<byte[]> body = body(ctx);
        if (body.isEmpty()) {
            tooLarge(ctx);
            return;
        }
        switch (validateCourse.validate(body.get())) {
            case CourseValidationResult.Checked checked -> report(ctx, HttpStatus.OK, checked.report());
            case CourseValidationResult.TooLarge tooLarge -> report(ctx, HttpStatus.CONTENT_TOO_LARGE, tooLarge.report());
        }
    }

    /** {@code POST /api/courses}: 201, 400, 413 o 422. */
    public void publish(Context ctx, AuthenticationResult.Authenticated authenticated) {
        Optional<byte[]> body = body(ctx);
        if (body.isEmpty()) {
            tooLarge(ctx);
            return;
        }
        switch (publishCourse.publish(authorOf(authenticated.user()), body.get())) {
            case Publication.Published published -> published(ctx, published);
            case Publication.TooLarge tooLarge -> report(ctx, HttpStatus.CONTENT_TOO_LARGE, tooLarge.report());
            case Publication.Unreadable unreadable -> report(ctx, HttpStatus.BAD_REQUEST, unreadable.report());
            case Publication.Rejected rejected -> report(ctx, HttpStatus.UNPROCESSABLE_CONTENT, rejected.report());
        }
    }

    /** {@code POST /api/courses/{id}/versions}: además de los de publicar, 403, 404 y 409. */
    public void publishVersion(Context ctx, AuthenticationResult.Authenticated authenticated) {
        Optional<UUID> courseId = PathIds.courseId(ctx.pathParam("id"));
        if (courseId.isEmpty()) {
            courseNotFound(ctx);
            return;
        }
        Optional<byte[]> body = body(ctx);
        if (body.isEmpty()) {
            tooLarge(ctx);
            return;
        }
        switch (publishCourseVersion.publish(authenticated.user().id(), courseId.get(), body.get())) {
            case Publication.Published published -> published(ctx, published);
            case Publication.TooLarge tooLarge -> report(ctx, HttpStatus.CONTENT_TOO_LARGE, tooLarge.report());
            case Publication.Unreadable unreadable -> report(ctx, HttpStatus.BAD_REQUEST, unreadable.report());
            case Publication.Rejected rejected -> report(ctx, HttpStatus.UNPROCESSABLE_CONTENT, rejected.report());
            case Publication.CourseNotFound notFound -> courseNotFound(ctx);
            case Publication.NotAuthor notAuthor -> ctx.status(HttpStatus.FORBIDDEN).json(ErrorResponse.of(
                    "id", ErrorCode.NOT_COURSE_AUTHOR, "Solo el autor del curso puede publicar versiones nuevas."));
            case Publication.IdenticalToLatest identical -> ctx.status(HttpStatus.CONFLICT).json(ErrorResponse.of(
                    "body", ErrorCode.IDENTICAL_TO_LATEST,
                    "El contenido es idéntico al de la última versión: no se ha creado ninguna versión nueva."));
        }
    }

    /**
     * El cuerpo tal como llegó. Javalin comprueba el límite de {@code maxRequestSize} al leerlo: si lo supera,
     * vacío, y se responde 413 con el mismo informe que da el lector. Javalin 7.2.4 lanza un
     * {@link HttpResponseException} genérico con el estado 413, no la subclase {@code ContentTooLargeResponse}
     * (comprobado en su código y con una petición real), así que se reconoce por el estado.
     */
    private static Optional<byte[]> body(Context ctx) {
        try {
            return Optional.of(ctx.bodyAsBytes());
        } catch (HttpResponseException e) {
            if (e.getStatus() == HttpStatus.CONTENT_TOO_LARGE.getCode()) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /** El usuario de la sesión, como autor. Su nombre se guarda en la respuesta para no pedirlo otra vez. */
    private static CourseAuthor authorOf(User user) {
        return new CourseAuthor(user.id(), user.name().value());
    }

    private static void published(Context ctx, Publication.Published published) {
        ctx.status(HttpStatus.CREATED).json(PublishedCourseResponse.from(published.course(), published.report()));
    }

    private static void report(Context ctx, HttpStatus status, CourseReport report) {
        ctx.status(status).json(CourseReportResponse.from(report));
    }

    private static void tooLarge(Context ctx) {
        report(ctx, HttpStatus.CONTENT_TOO_LARGE, new CourseReport(List.of(SizeLimit.exceeded())));
    }

    private static void courseNotFound(Context ctx) {
        ctx.status(HttpStatus.NOT_FOUND).json(ErrorResponse.of("id", ErrorCode.COURSE_NOT_FOUND, "No existe ese curso."));
    }
}
