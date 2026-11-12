package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PlaceholderAction
import es.ulpgc.pamn.dominio.ui.components.PlaceholderScreen
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Provisional (T2). En T8–T11 se convierte en el registro en tres pasos.
@Composable
fun ScreenRegister(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(title = "Registro", modifier = modifier, onBack = onBack) {
        PlaceholderAction(text = "Registrarme", onClick = onRegistered)
    }
}

@LightDarkPreviews
@Composable
private fun ScreenRegisterPreview() {
    GradusTheme { ScreenRegister(onBack = {}, onRegistered = {}) }
}
