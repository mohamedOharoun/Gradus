package es.ulpgc.pamn.dominio.ui.screens.mycourses

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PlaceholderScreen
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Pantalla provisional de la «Guía para crear cursos con IA» (docs/tareas/subida-cursos.md).
// Recibe `onBack` por parámetro (state hoisting) y reutiliza PlaceholderScreen.
@Composable
fun ScreenCourseGuide(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.course_guide_title),
        onBack = onBack,
        modifier = modifier,
    )
}

@LightDarkPreviews
@Composable
private fun ScreenCourseGuidePreview() {
    GradusTheme {
        ScreenCourseGuide(onBack = {})
    }
}
