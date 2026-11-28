package es.ulpgc.pamn.dominio

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.ui.navigation.GradusNavHost
import es.ulpgc.pamn.dominio.ui.navigation.StartViewModel
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

class MainActivity : ComponentActivity() {

    // Vive con la Activity (sobrevive al giro) y escucha la sesión guardada desde que se crea.
    private val startViewModel: StartViewModel by viewModels {
        StartViewModel.factory(AppContainer.sessionUpdates, AppContainer.progressSync)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // El modo oscuro elegido en Ajustes; si no se ha tocado, el del sistema (decisión B).
            val settings by AppContainer.settingsStore.settings.collectAsStateWithLifecycle()
            val dark = settings.isDark(systemDark = isSystemInDarkTheme())

            // Los iconos de las barras del sistema (hora, batería) siguen a la app, no al sistema.
            // Se vuelve a ejecutar solo cuando cambia `dark`.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }

            GradusTheme(darkTheme = dark) {
                // Con lifecycle: si la sesión se borra con la app en segundo plano, al volver ya se aplica.
                val start = startViewModel.state.collectAsStateWithLifecycle().value
                GradusNavHost(
                    start = start,
                    accountGateway = AppContainer.accountGateway,
                    sessionStore = AppContainer.sessionStore,
                    progressSync = AppContainer.progressSync,
                )
            }
        }
    }
}
