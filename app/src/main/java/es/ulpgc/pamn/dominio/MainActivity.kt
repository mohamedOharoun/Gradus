package es.ulpgc.pamn.dominio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
            GradusTheme {
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
