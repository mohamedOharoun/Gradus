package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PlaceholderScreen
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Provisional: inicio con un saludo, al que se llega tras registrarse o iniciar sesión.
@Composable
fun ScreenHome(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = "¡Hola!", modifier = modifier)
}

@LightDarkPreviews
@Composable
private fun ScreenHomePreview() {
    GradusTheme { ScreenHome() }
}
