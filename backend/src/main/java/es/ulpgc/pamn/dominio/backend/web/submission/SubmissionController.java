package es.ulpgc.pamn.dominio.backend.web.submission;

import es.ulpgc.pamn.dominio.backend.application.AuthenticationResult;
import es.ulpgc.pamn.dominio.backend.application.submission.DownloadPhotoResult;
import es.ulpgc.pamn.dominio.backend.application.submission.DownloadSubmissionPhoto;
import es.ulpgc.pamn.dominio.backend.application.submission.ListMySubmissions;
import es.ulpgc.pamn.dominio.backend.application.submission.SubmitPhoto;
import es.ulpgc.pamn.dominio.backend.application.submission.SubmitPhotoCommand;
import es.ulpgc.pamn.dominio.backend.domain.submission.Jpeg;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Traduce HTTP ↔ casos de uso de las entregas con foto (docs/tareas/ejercicio-foto.md, paso 3e-2). Sin reglas
 * de negocio: lee la petición, llama al caso de uso y envía lo que decide {@link SubmissionReplies}. Las tres
 * rutas van con {@code RequireSession}: el alumno es siempre el de la sesión, nunca uno que diga el cliente.
 */
public final class SubmissionController {

    /** El campo de fichero del multipart y el de texto con la versión (contrato de la API). */
    static final String PHOTO_FIELD = "photo";
    static final String VERSION_FIELD = "version";

    private final SubmitPhoto submitPhoto;
    private final ListMySubmissions listMySubmissions;
    private final DownloadSubmissionPhoto downloadPhoto;

    public SubmissionController(SubmitPhoto submitPhoto, ListMySubmissions listMySubmissions,
                                DownloadSubmissionPhoto downloadPhoto) {
        this.submitPhoto = Objects.requireNonNull(submitPhoto, "submitPhoto");
        this.listMySubmissions = Objects.requireNonNull(listMySubmissions, "listMySubmissions");
        this.downloadPhoto = Objects.requireNonNull(downloadPhoto, "downloadPhoto");
    }

    /**
     * {@code POST /api/courses/{id}/exercises/{exerciseId}/submissions} (multipart: {@code photo} y
     * {@code version}): 201, 400, 404 o 413. El tamaño se mira antes de leer nada (decisión O).
     */
    public void submit(Context ctx, AuthenticationResult.Authenticated authenticated) {
        Optional<UUID> courseId = SubmissionParams.id(ctx.pathParam("id"));
        if (courseId.isEmpty()) {
            send(ctx, SubmissionReplies.courseNotFound());
            return;
        }
        // getContentLengthLong: ctx.contentLength() es un int y daría -1 con más de 2 GB.
        if (UploadLimit.exceededBy(ctx.req().getContentLengthLong())) {
            send(ctx, SubmissionReplies.requestTooLarge());
            return;
        }
        String rawVersion = ctx.formParam(VERSION_FIELD);
        if (rawVersion == null) {
            send(ctx, SubmissionReplies.missingVersion());
            return;
        }
        Optional<Integer> version = SubmissionParams.version(rawVersion);
        if (version.isEmpty()) {
            send(ctx, SubmissionReplies.invalidVersion());
            return;
        }
        SubmitPhotoCommand command = new SubmitPhotoCommand(authenticated.user().id(), courseId.get(), version.get(),
                ctx.pathParam("exerciseId"), photoBytes(ctx.uploadedFile(PHOTO_FIELD)));
        send(ctx, SubmissionReplies.of(submitPhoto.submit(command)));
    }

    /** {@code GET /api/submissions/mine?courseId=…}: 200 con las propias (la más reciente primero), o 400. */
    public void mine(Context ctx, AuthenticationResult.Authenticated authenticated) {
        String rawCourseId = ctx.queryParam("courseId");
        if (rawCourseId == null) {
            send(ctx, SubmissionReplies.missingCourseId());
            return;
        }
        Optional<UUID> courseId = SubmissionParams.id(rawCourseId);
        if (courseId.isEmpty()) {
            send(ctx, SubmissionReplies.invalidCourseId());
            return;
        }
        ctx.status(HttpStatus.OK)
                .json(SubmissionListResponse.from(listMySubmissions.list(authenticated.user().id(), courseId.get())));
    }

    /** {@code GET /api/submissions/{id}/photo}: 200 {@code image/jpeg}, 403 o 404. */
    public void photo(Context ctx, AuthenticationResult.Authenticated authenticated) {
        Optional<UUID> submissionId = SubmissionParams.id(ctx.pathParam("id"));
        if (submissionId.isEmpty()) {
            send(ctx, SubmissionReplies.submissionNotFound());
            return;
        }
        switch (downloadPhoto.download(authenticated.user().id(), submissionId.get())) {
            case DownloadPhotoResult.Found found -> jpeg(ctx, found.photo());
            case DownloadPhotoResult.NotFound notFound -> send(ctx, SubmissionReplies.submissionNotFound());
            case DownloadPhotoResult.Forbidden forbidden -> send(ctx, SubmissionReplies.notOwner());
        }
    }

    /**
     * Los bytes del fichero {@code photo}, o {@code null} si no vino (el caso de uso lo convierte en «falta la
     * foto»). Ya están limitados por el multipart de Jetty (decisión N); no se mira su nombre ni su tipo.
     */
    private static byte[] photoBytes(UploadedFile photo) {
        if (photo == null) {
            return null;
        }
        try (InputStream content = photo.content()) {
            return content.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la foto recibida", e);
        }
    }

    /**
     * La foto tal cual. {@code private, no-store}: es de un alumno, ningún intermediario debe guardarla;
     * {@code nosniff}: el navegador no intenta adivinar otro tipo aunque los bytes engañen.
     */
    private static void jpeg(Context ctx, Jpeg photo) {
        ctx.status(HttpStatus.OK)
                .contentType("image/jpeg")
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .result(photo.bytes());
    }

    private static void send(Context ctx, JsonReply reply) {
        ctx.status(reply.status()).json(reply.body());
    }
}
