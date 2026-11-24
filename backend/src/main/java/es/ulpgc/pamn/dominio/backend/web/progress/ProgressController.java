package es.ulpgc.pamn.dominio.backend.web.progress;

import es.ulpgc.pamn.dominio.backend.application.AuthenticationResult;
import es.ulpgc.pamn.dominio.backend.application.progress.SyncProgress;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.web.ErrorResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Objects;

/**
 * Traduce HTTP ↔ caso de uso de sincronizar el progreso (api-progreso.md). Sin reglas de negocio: lee el cuerpo
 * como bytes (nunca con {@code bodyAsClass}) y elige la respuesta con un {@code switch} sobre el resultado
 * sellado del lector. La ruta está protegida con {@code RequireSession}: el usuario es siempre el de la sesión.
 */
public final class ProgressController {

    private final ProgressRequestReader reader;
    private final SyncProgress syncProgress;

    public ProgressController(ProgressRequestReader reader, SyncProgress syncProgress) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.syncProgress = Objects.requireNonNull(syncProgress, "syncProgress");
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
}
