package es.ulpgc.pamn.dominio.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Mientras la pantalla que lo llama esté visible, los iconos de la barra de estado se ven blancos
// (para fondos oscuros). Al salir de la pantalla se restaura lo que había.
@Composable
fun LightStatusBarIcons() {
    if (LocalInspectionMode.current) return // en las previews no hay ventana real
    val window = LocalActivity.current?.window ?: return
    val view = LocalView.current

    DisposableEffect(window, view) {
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}
