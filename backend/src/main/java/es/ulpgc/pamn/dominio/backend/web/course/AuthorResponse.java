package es.ulpgc.pamn.dominio.backend.web.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseAuthor;

/** El autor de un curso, con su nombre para que la app no necesite otra petición (§4.4). */
public record AuthorResponse(String id, String name) {

    public static AuthorResponse from(CourseAuthor author) {
        return new AuthorResponse(author.id().toString(), author.name());
    }
}
