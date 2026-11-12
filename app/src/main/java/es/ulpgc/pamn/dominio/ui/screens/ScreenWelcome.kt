package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PlaceholderAction
import es.ulpgc.pamn.dominio.ui.components.PlaceholderScreen
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Provisional (T2). En T3 se sustituye por la bienvenida del Figma.
@Composable
fun ScreenWelcome(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(title = "Bienvenida", modifier = modifier) {
        PlaceholderAction(text = "Crear cuenta", onClick = onCreateAccount)
        PlaceholderAction(text = "Iniciar sesión", onClick = onLogin)
    }
}

@LightDarkPreviews
@Composable
private fun ScreenWelcomePreview() {
    GradusTheme { ScreenWelcome(onCreateAccount = {}, onLogin = {}) }
}
