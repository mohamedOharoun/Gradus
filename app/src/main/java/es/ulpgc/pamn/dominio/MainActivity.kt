package es.ulpgc.pamn.dominio

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.ulpgc.pamn.dominio.data.AppContainer
import es.ulpgc.pamn.dominio.ui.navigation.GradusNavHost
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.init(this)
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
                GradusNavHost(accountGateway = AppContainer.accountGateway, sessionStore = AppContainer.sessionStore)
            }
        }
    }
}
