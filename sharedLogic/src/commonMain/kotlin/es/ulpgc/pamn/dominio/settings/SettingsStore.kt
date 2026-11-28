package es.ulpgc.pamn.dominio.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// Puerto (patrón Strategy): dónde se guardan las preferencias. En la app, un adaptador con
// SharedPreferences (se recuerdan al cerrarla); en tests y previews, InMemorySettingsStore.
interface SettingsStore {

    // Las preferencias actuales y, después, cada cambio (la app escucha para cambiar el tema).
    // StateFlow: siempre tiene un valor actual, así la app arranca ya con el tema elegido.
    val settings: StateFlow<AppSettings>

    suspend fun setDarkMode(dark: Boolean)

    // Las etiquetas preferidas (fase 3). No toca el modo oscuro.
    suspend fun setPreferredTags(tags: PreferredTags)
}

// En memoria: dura lo que dure el objeto. MutableStateFlow no avisa si el valor nuevo es igual
// al anterior, así que elegir lo mismo otra vez no repinta nada.
class InMemorySettingsStore(initial: AppSettings = AppSettings()) : SettingsStore {

    private val state = MutableStateFlow(initial)

    override val settings: StateFlow<AppSettings> = state

    override suspend fun setDarkMode(dark: Boolean) {
        state.update { it.copy(darkMode = dark) }
    }

    override suspend fun setPreferredTags(tags: PreferredTags) {
        state.update { it.copy(preferredTags = tags) }
    }
}
