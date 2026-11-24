package es.ulpgc.pamn.dominio.backend.web.progress;

import com.fasterxml.jackson.annotation.JsonInclude;
import es.ulpgc.pamn.dominio.backend.application.progress.SyncResult;
import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.MasteredModule;
import es.ulpgc.pamn.dominio.backend.domain.progress.SkippedEntry;

import java.util.List;

/**
 * El cuerpo de la respuesta 200 (api-progreso.md). Mapper de {@link SyncResult} a registros de solo datos:
 * no reordena nada, las listas salen en el orden en que llegan.
 */
public record SyncResponse(List<EnrollmentResponse> enrollments, List<SkippedResponse> skipped) {

    public static SyncResponse from(SyncResult result) {
        return new SyncResponse(
                result.enrollments().stream().map(EnrollmentResponse::from).toList(),
                result.skipped().stream().map(SkippedResponse::from).toList());
    }

    public record EnrollmentResponse(String courseId, String enrolledAt, List<MasteredResponse> mastered) {

        static EnrollmentResponse from(Enrollment enrollment) {
            return new EnrollmentResponse(
                    enrollment.courseId().toString(),
                    enrollment.enrolledAt().toString(), // ISO-8601 UTC, con fracción si la hay
                    enrollment.mastered().stream().map(MasteredResponse::from).toList());
        }
    }

    public record MasteredResponse(String moduleId, String masteredAt) {

        static MasteredResponse from(MasteredModule module) {
            return new MasteredResponse(module.moduleId(), module.masteredAt().toString());
        }
    }

    /**
     * {@code SkippedEntry.moduleId} es un {@code Optional}, que Jackson no serializa bien sin un módulo extra:
     * aquí es un {@code String} que se omite (no se escribe como {@code null}) cuando no hay módulo.
     */
    public record SkippedResponse(String courseId, @JsonInclude(JsonInclude.Include.NON_NULL) String moduleId,
                                  String code) {

        static SkippedResponse from(SkippedEntry entry) {
            return new SkippedResponse(entry.courseId(), entry.moduleId().orElse(null), entry.code().name());
        }
    }
}
