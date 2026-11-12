package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PlaceholderAction
import es.ulpgc.pamn.dominio.ui.components.PlaceholderScreen
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Provisional (T2). En T7 se añaden correo, contraseña y validación.
@Composable
fun ScreenLogin(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(title = "Iniciar sesión", modifier = modifier, onBack = onBack) {
        PlaceholderAction(text = "Entrar", onClick = onLoggedIn)
    }
}

@LightDarkPreviews
@Composable
private fun ScreenLoginPreview() {
    GradusTheme { ScreenLogin(onBack = {}, onLoggedIn = {}) }
}
