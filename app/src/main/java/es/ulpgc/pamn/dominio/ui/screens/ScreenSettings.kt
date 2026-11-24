package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.ui.screens.settings.AccountSection
import es.ulpgc.pamn.dominio.ui.screens.settings.AppearanceSection
import es.ulpgc.pamn.dominio.ui.screens.settings.InterestsSection
import es.ulpgc.pamn.dominio.ui.screens.settings.SessionSection
import es.ulpgc.pamn.dominio.ui.screens.settings.SettingsActions
import es.ulpgc.pamn.dominio.ui.screens.settings.SettingsScreenState

// Pestaña Ajustes (Figma 6, ajustes.md tarea 5). Sin estado: recibe qué enseñar y avisa de lo que
// se toca. Se desplaza si no cabe (pantallas pequeñas o letra grande).
// Las previews están en settings/ScreenSettingsPreviews.kt.
@Composable
fun ScreenSettings(
    state: SettingsScreenState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 24.dp).semantics { heading() },
            )
            AccountSection(state, actions)
            AppearanceSection(state, actions)
            InterestsSection(state, actions)
            SessionSection(actions)
        }
    }
}

// Solo para previews: la pantalla con datos de ejemplo. La usa la preview de MainScaffold.kt
// (`{ ScreenSettings(it) }`), que es del compañero y no tocamos. La app usa la de arriba (SettingsRoute).
@Composable
internal fun ScreenSettings(modifier: Modifier = Modifier) {
    ScreenSettings(
        state = SettingsScreenState("Mohamed Oharoun", "nombre@ejemplo.com", darkMode = isSystemInDarkTheme()),
        actions = SettingsActions(onDarkModeChange = {}, onLogOut = {}),
        modifier = modifier,
    )
}
