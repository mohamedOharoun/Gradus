package es.ulpgc.pamn.dominio.backend.web.progress;

import es.ulpgc.pamn.dominio.backend.application.AuthenticationResult;
import es.ulpgc.pamn.dominio.backend.application.progress.SyncProgress;
import es.ulpgc.pamn.dominio.backend.application.progress.UnenrollCourse;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.web.CanonicalUuid;
import es.ulpgc.pamn.dominio.backend.web.ErrorResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Objects;

/**
 * Traduce HTTP ↔ casos de uso de progreso (api-progreso.md). Sin reglas de negocio: en {@code sync} lee el
 * cuerpo como bytes (nunca con {@code bodyAsClass}) y elige la respuesta con un {@code switch} sobre el
 * resultado sellado del lector; en {@code unenroll} valida el UUID de la ruta y delega en el caso de uso.
 * Las rutas están protegidas con {@code RequireSession}: el usuario es siempre el de la sesión.
 */
public final class ProgressController {

    private final ProgressRequestReader reader;
    private final SyncProgress syncProgress;
    private final UnenrollCourse unenrollCourse;

    public ProgressController(ProgressRequestReader reader, SyncProgress syncProgress, UnenrollCourse unenrollCourse) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.syncProgress = Objects.requireNonNull(syncProgress, "syncProgress");
        this.unenrollCourse = Objects.requireNonNull(unenrollCourse, "unenrollCourse");
    }

    /**
     * {@code POST /api/progress/sync}: 200 con todo el progreso del usuario o 400 si el cuerpo no tiene la forma
     * del contrato. El 413 (cuerpo mayor que {@code maxRequestSize}) lo lanza {@code bodyAsBytes()} y lo contesta
     * Javalin: aquí no se captura.
     */
    public void sync(Context ctx, AuthenticationResult.Authenticated authenticated) {
        switch (reader.read(ctx.bodyAsBytes())) {
            case ParsedRequest.Parsed parsed -> ctx.status(HttpStatus.OK).json(SyncResponse.from(
                    syncProgress.sync(authenticated.user().id(), parsed.enrollments())));
            case ParsedRequest.Malformed malformed -> ctx.status(HttpStatus.BAD_REQUEST).json(ErrorResponse.of(
                    "body", ErrorCode.MALFORMED_BODY,
                    "El cuerpo no tiene la forma esperada: { \"enrollments\": [ { \"courseId\", \"enrolledAt\", "
                            + "\"mastered\": [ { \"moduleId\", \"masteredAt\" } ] } ] }, con fechas ISO-8601."));
        }
    }

    /**
     * {@code DELETE /api/progress/enrollments/{courseId}}: 204 sin cuerpo (idempotente) si {@code courseId} es
     * un UUID canónico, o 404 ({@code COURSE_NOT_FOUND}) si no lo es.
     */
    public void unenroll(Context ctx, AuthenticationResult.Authenticated authenticated) {
        CanonicalUuid.parse(ctx.pathParam("courseId")).ifPresentOrElse(
                courseId -> {
                    unenrollCourse.unenroll(authenticated.user().id(), courseId);
                    ctx.status(HttpStatus.NO_CONTENT);
                },
                () -> ctx.status(HttpStatus.NOT_FOUND).json(ErrorResponse.of(
                        "courseId", ErrorCode.COURSE_NOT_FOUND, "No existe ese curso.")));
    }
}
