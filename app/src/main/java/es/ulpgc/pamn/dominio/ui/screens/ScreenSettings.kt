package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PlaceholderScreen
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Provisional: pestaña Ajustes (Figma 6). Su contenido tendrá su propio documento de tarea.
@Composable
fun ScreenSettings(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = "Ajustes", modifier = modifier)
}

@LightDarkPreviews
@Composable
private fun ScreenSettingsPreview() {
    GradusTheme { ScreenSettings() }
}
