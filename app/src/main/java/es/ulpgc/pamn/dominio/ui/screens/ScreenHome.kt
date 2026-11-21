package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.ulpgc.pamn.dominio.ui.components.ErrorText
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PlaceholderAction
import es.ulpgc.pamn.dominio.ui.components.PlaceholderScreen
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Provisional: inicio con un saludo, al que se llega tras registrarse o iniciar sesión.
// El botón también es provisional (cursos-backend-app.md, tarea 7): abre el curso más reciente del
// servidor hasta que exista Buscar. `notice`: por qué no se ha podido abrir.
@Composable
fun ScreenHome(onOpenCourse: () -> Unit, modifier: Modifier = Modifier, notice: String? = null) {
    PlaceholderScreen(title = "¡Hola!", modifier = modifier) {
        PlaceholderAction(text = "Abrir curso del servidor", onClick = onOpenCourse)
        notice?.let { ErrorText(it) }
    }
}

@LightDarkPreviews
@Composable
private fun ScreenHomePreview() {
    GradusTheme { ScreenHome(onOpenCourse = {}, notice = "No se ha podido conectar con el servidor.") }
}
