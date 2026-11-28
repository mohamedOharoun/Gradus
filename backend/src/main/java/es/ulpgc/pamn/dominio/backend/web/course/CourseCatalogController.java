package es.ulpgc.pamn.dominio.backend.web.course;

import es.ulpgc.pamn.dominio.backend.application.course.FindCourse;
import es.ulpgc.pamn.dominio.backend.application.course.SearchCourses;
import es.ulpgc.pamn.dominio.backend.application.course.SearchCoursesQuery;
import es.ulpgc.pamn.dominio.backend.application.course.SearchResult;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseVersion;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import es.ulpgc.pamn.dominio.backend.web.ErrorResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Objects;
import java.util.Optional;

/**
 * Traduce HTTP ↔ casos de uso de consulta (§4.3). Las tres rutas son públicas: buscar y descargar cursos no
 * necesita sesión. Sin reglas de negocio: los parámetros se pasan tal como llegan.
 */
public final class CourseCatalogController {

    private final FindCourse findCourse;
    private final SearchCourses searchCourses;

    public CourseCatalogController(FindCourse findCourse, SearchCourses searchCourses) {
        this.findCourse = Objects.requireNonNull(findCourse, "findCourse");
        this.searchCourses = Objects.requireNonNull(searchCourses, "searchCourses");
    }

    /**
     * {@code GET /api/courses?q=&tag=&language=&author=&published=&page=&size=}: 200 o 400. {@code tag}
     * puede repetirse.
     */
    public void search(Context ctx) {
        SearchCoursesQuery query = new SearchCoursesQuery(ctx.queryParam("q"), ctx.queryParams("tag"),
                ctx.queryParam("language"), ctx.queryParam("author"), ctx.queryParam("published"),
                ctx.queryParam("page"), ctx.queryParam("size"));
        switch (searchCourses.search(query)) {
            case SearchResult.Found found -> ctx.json(CoursePageResponse.from(found.page()));
            case SearchResult.Rejected rejected -> ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse(rejected.errors()));
        }
    }

    /** {@code GET /api/courses/{id}}: la última versión, con contenido; 404. */
    public void latest(Context ctx) {
        respond(ctx, "id", PathIds.courseId(ctx.pathParam("id")).flatMap(findCourse::latest));
    }

    /** {@code GET /api/courses/{id}/versions/{version}}: una versión concreta, con contenido; 404. */
    public void version(Context ctx) {
        Optional<CourseVersion> found = PathIds.courseId(ctx.pathParam("id"))
                .flatMap(id -> PathIds.version(ctx.pathParam("version")).flatMap(number -> findCourse.version(id, number)));
        respond(ctx, "version", found);
    }

    private static void respond(Context ctx, String field, Optional<CourseVersion> version) {
        version.ifPresentOrElse(
                found -> ctx.json(CourseVersionResponse.from(found)),
                () -> ctx.status(HttpStatus.NOT_FOUND).json(ErrorResponse.of(field, ErrorCode.COURSE_NOT_FOUND,
                        "id".equals(field) ? "No existe ese curso." : "No existe ese curso o esa versión.")));
    }
}
