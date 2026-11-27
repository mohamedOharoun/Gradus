package es.ulpgc.pamn.dominio.backend.web.course;

import es.ulpgc.pamn.dominio.backend.application.AuthenticationResult;
import es.ulpgc.pamn.dominio.backend.application.course.ListMyCourses;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Objects;

/**
 * Controlador de los cursos propios del usuario autenticado (docs/tareas/subida-cursos.md, paso 1a).
 * Separado de {@link CourseCatalogController} (estrictamente público) y de
 * {@link CoursePublicationController} (validar/publicar).
 */
public final class MyCoursesController {

    private final ListMyCourses listMyCourses;

    public MyCoursesController(ListMyCourses listMyCourses) {
        this.listMyCourses = Objects.requireNonNull(listMyCourses, "listMyCourses");
    }

    /** {@code GET /api/users/me/courses}: 200 con la última versión de cada curso del usuario autenticado. */
    public void mine(Context ctx, AuthenticationResult.Authenticated authenticated) {
        ctx.status(HttpStatus.OK)
                .json(CourseListResponse.from(listMyCourses.list(authenticated.user().id())));
    }
}
