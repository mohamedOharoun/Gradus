package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PlaceholderScreen
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Provisional: pestaña Mis cursos (Figma 5). Su contenido tendrá su propio documento de tarea.
@Composable
fun ScreenMyCourses(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.tab_my_courses), modifier = modifier)
}

@LightDarkPreviews
@Composable
private fun ScreenMyCoursesPreview() {
    GradusTheme { ScreenMyCourses() }
}
