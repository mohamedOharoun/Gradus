package es.ulpgc.pamn.dominio.ui.screens.detail

import androidx.compose.runtime.Composable
import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.Publication
import es.ulpgc.pamn.dominio.data.demoCourse
import es.ulpgc.pamn.dominio.data.demoMastered
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.screens.course.CourseProblem
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Previews de ScreenCourseDetail, una por estado, con el curso de ejemplo (10 módulos) publicado.

private val published = demoCourse.copy(
    publication = Publication(Author("u1", "Ana Ruiz"), version = 3, publishedAt = "2026-10-03T08:35:00Z"),
    description = "Conjuntos, relaciones, grafos, árboles y tablas hash, paso a paso. Cada módulo " +
        "empieza con una lección corta y termina con una prueba de dominio: hasta que no la apruebas, " +
        "no se desbloquea lo que depende de él. Pensado para primero de carrera.",
)

@Composable
private fun Preview(state: CourseDetailState) {
    GradusTheme { ScreenCourseDetail(state, CourseDetailActions()) }
}

@LightDarkPreviews
@Composable
private fun UnenrolledPreview() = Preview(CourseDetailState.Ready(published, enrolled = false))

@LightDarkPreviews
@Composable
private fun EnrollingPreview() = Preview(CourseDetailState.Ready(published, enrolled = false, enrolling = true))

@LightDarkPreviews
@Composable
private fun EnrolledZeroProgressPreview() = Preview(CourseDetailState.Ready(published, enrolled = true))

@LightDarkPreviews
@Composable
private fun EnrolledWithProgressPreview() =
    Preview(CourseDetailState.Ready(published, mastered = demoMastered, enrolled = true))

@LightDarkPreviews
@Composable
private fun EnrolledAllMasteredPreview() = Preview(
    CourseDetailState.Ready(
        course = published,
        mastered = published.modules.map { it.id }.toSet(),
        enrolled = true,
    ),
)

// Sin datos de publicación (sin autor ni fecha) y sin descripción.
@LightDarkPreviews
@Composable
private fun WithoutPublicationPreview() =
    Preview(CourseDetailState.Ready(demoCourse.copy(description = ""), enrolled = false))

@LightDarkPreviews
@Composable
private fun LoadingPreview() = Preview(CourseDetailState.Loading)

@LightDarkPreviews
@Composable
private fun ProblemPreview() = Preview(CourseDetailState.Problem(CourseProblem.UNREACHABLE))
