package es.ulpgc.pamn.dominio.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.home.HomeCourse
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// «Tus cursos» (Figma «Inicio»): el título de la sección y una tarjeta con una fila por curso, separadas
// por una línea. Sin «Ver todos» (decisión B): salen todos. Avisa con el id del curso tocado
// (state hoisting: la navegación la decide quien la usa).
@Composable
fun HomeCoursesSection(courses: List<HomeCourse>, onOpenCourse: (courseId: String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.home_your_courses),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                courses.forEachIndexed { index, course ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    CourseProgressRow(course, onClick = { onOpenCourse(course.course.id) })
                }
            }
        }
    }
}

@LightDarkPreviews
@Composable
private fun HomeCoursesSectionPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeCoursesSection(HomePreviewData.courses, onOpenCourse = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
