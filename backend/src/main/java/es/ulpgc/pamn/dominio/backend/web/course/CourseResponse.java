package es.ulpgc.pamn.dominio.backend.web.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseMetadata;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseSummary;

import java.util.List;

/**
 * Los metadatos de una versión (§4.4): la respuesta 201 y cada resultado de la búsqueda. Los campos de
 * búsqueda normalizados y el tamaño son internos y no se envían.
 */
public record CourseResponse(String id, int version, AuthorResponse author, String publishedAt, int formatVersion,
                             String title, String description, String language, List<String> tags,
                             int moduleCount, String sha256) {

    public static CourseResponse from(CourseSummary summary) {
        CourseMetadata metadata = summary.metadata();
        return new CourseResponse(
                summary.id().toString(),
                summary.version(),
                AuthorResponse.from(summary.author()),
                summary.publishedAt().toString(), // ISO-8601, como en el resto de la API
                metadata.formatVersion(),
                metadata.title(),
                metadata.description(),
                metadata.language(),
                metadata.tags(),
                metadata.moduleCount(),
                metadata.sha256());
    }
}
