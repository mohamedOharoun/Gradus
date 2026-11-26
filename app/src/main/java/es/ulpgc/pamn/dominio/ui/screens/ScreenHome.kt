package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PlaceholderScreen
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Provisional: inicio con un saludo, al que se llega tras registrarse o iniciar sesión.
@Composable
fun ScreenHome(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.home_hello), modifier = modifier)
}

@LightDarkPreviews
@Composable
private fun ScreenHomePreview() {
    GradusTheme { ScreenHome() }
}
