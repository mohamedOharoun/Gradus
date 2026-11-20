package es.ulpgc.pamn.dominio.backend.web.course;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersion;

/**
 * Una versión con su contenido (§4.4): los metadatos y {@code "content": {…el JSON del autor…}}.
 * {@link JsonUnwrapped} pone los campos de {@code course} al mismo nivel que {@code content}, y
 * {@link JsonRawValue} inserta el contenido tal como está guardado, sin parsearlo ni volver a serializarlo:
 * así los bytes servidos son los del {@code sha256} publicado.
 */
public record CourseVersionResponse(@JsonUnwrapped CourseResponse course, @JsonRawValue String content) {

    public static CourseVersionResponse from(CourseVersion version) {
        return new CourseVersionResponse(CourseResponse.from(version.summary()), version.content().text());
    }
}
