package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PlaceholderAction
import es.ulpgc.pamn.dominio.ui.components.PlaceholderScreen
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Provisional: inicio con un saludo, al que se llega tras registrarse o iniciar sesión.
// El botón al curso de ejemplo también es provisional (camino-curso-y-ejercicios.md, tarea 5):
// se irá cuando Inicio muestre los cursos inscritos.
@Composable
fun ScreenHome(onOpenCourse: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(title = "¡Hola!", modifier = modifier) {
        PlaceholderAction(text = "Abrir curso de ejemplo", onClick = onOpenCourse)
    }
}

@LightDarkPreviews
@Composable
private fun ScreenHomePreview() {
    GradusTheme { ScreenHome(onOpenCourse = {}) }
}
