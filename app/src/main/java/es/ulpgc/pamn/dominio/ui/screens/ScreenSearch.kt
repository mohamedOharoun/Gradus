package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PlaceholderScreen
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Provisional: pestaña Buscar (Figma 3 y 4). Su contenido tendrá su propio documento de tarea.
@Composable
fun ScreenSearch(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = "Buscar", modifier = modifier)
}

@LightDarkPreviews
@Composable
private fun ScreenSearchPreview() {
    GradusTheme { ScreenSearch() }
}
