package es.ulpgc.pamn.dominio.ui.screens.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.screens.ScreenSettings
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

private val noActions = SettingsActions(onDarkModeChange = {}, onLogOut = {})

// El interruptor enseña cómo está la app: encendido en la preview oscura, apagado en la clara.
@LightDarkPreviews
@Composable
private fun ScreenSettingsPreview() {
    GradusTheme {
        val dark = isSystemInDarkTheme()
        ScreenSettings(SettingsScreenState("Mohamed Oharoun", "nombre@ejemplo.com", darkMode = dark), noActions)
    }
}

// Un nombre y un correo largos se cortan con «…» sin empujar «Próximamente».
@Preview(name = "Nombre largo", showBackground = true, heightDp = 400)
@Composable
private fun ScreenSettingsLongNamePreview() {
    GradusTheme {
        ScreenSettings(
            SettingsScreenState(
                name = "María José Fernández de la Torre Santana",
                email = "maria.jose.fernandez.delatorre@estudiantes.ulpgc.es",
                darkMode = false,
            ),
            noActions,
        )
    }
}
