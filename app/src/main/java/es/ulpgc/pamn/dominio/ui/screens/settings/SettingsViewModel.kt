package es.ulpgc.pamn.dominio.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.account.AccountGateway
import es.ulpgc.pamn.dominio.account.SessionStore
import es.ulpgc.pamn.dominio.data.settings.LanguageSetting
import es.ulpgc.pamn.dominio.settings.AppLanguage
import es.ulpgc.pamn.dominio.settings.AppSettings
import es.ulpgc.pamn.dominio.settings.SettingsStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Ajustes (ajustes.md, tarea 6): el perfil sale de la sesión guardada y el modo oscuro del store.
// Recibe los puertos por constructor: no sabe si hay HTTP ni dónde se guarda nada.
class SettingsViewModel(
    private val accounts: AccountGateway,
    private val sessions: SessionStore,
    private val settings: SettingsStore,
    private val languages: LanguageSetting,
) : ViewModel() {

    companion object {
        fun factory(accounts: AccountGateway, sessions: SessionStore, settings: SettingsStore, languages: LanguageSetting) =
            viewModelFactory { initializer { SettingsViewModel(accounts, sessions, settings, languages) } }
    }

    var state by mutableStateOf<SettingsState>(SettingsState.Loading)
        private set

    // Lo elegido (o null = el del sistema). La pantalla lo combina con el tema del sistema.
    val appSettings: StateFlow<AppSettings> = settings.settings

    init {
        viewModelScope.launch {
            state = sessions.current()?.let { SettingsState.Ready(it.account) } ?: SettingsState.LoggedOut
        }
    }

    // Al volver a Ajustes (p. ej. desde «Editar perfil»): se vuelve a leer la sesión para enseñar el
    // nombre nuevo. Si se está cerrando la sesión, no se toca nada.
    fun onResume() {
        if (!isIdle) return
        viewModelScope.launch {
            val session = sessions.current()
            if (isIdle && session != null) state = SettingsState.Ready(session.account)
        }
    }

    // Con el perfil cargado y sin cerrar sesión.
    private val isIdle: Boolean get() = (state as? SettingsState.Ready)?.loggingOut == false

    // ── Idioma (fase 3, decisiones O, S y V) ──

    // El idioma de la app ahora y si se puede elegir (Android 13+).
    var language by mutableStateOf(languages.current())
        private set
    val canChooseLanguage: Boolean get() = languages.canChooseLanguage

    // La elección de idioma abierta. Vive aquí para que girar el móvil no la cierre.
    var languagePickerOpen by mutableStateOf(false)
        private set

    fun onOpenLanguage() {
        if (canChooseLanguage) languagePickerOpen = true
    }

    fun onDismissLanguage() {
        languagePickerOpen = false
    }

    // Elegir el que ya estaba no hace nada: cambiarlo vuelve a crear toda la pantalla.
    fun onLanguageChosen(chosen: AppLanguage) {
        if (!canChooseLanguage) return
        languagePickerOpen = false
        if (chosen == language) return
        language = chosen
        languages.choose(chosen)
    }

    // Decisión B: al tocar el interruptor, el modo queda fijado y se recuerda.
    fun onDarkModeChange(dark: Boolean) {
        viewModelScope.launch { settings.setDarkMode(dark) }
    }

    // Decisión C: sin confirmación, y la sesión se borra del móvil aunque el servidor no responda
    // (el usuario quiere salir; la del servidor caduca sola). Solo una vez: los toques de más se ignoran.
    fun onLogOut() {
        val current = state as? SettingsState.Ready ?: return
        if (current.loggingOut) return
        state = current.copy(loggingOut = true)
        viewModelScope.launch {
            sessions.current()?.let { accounts.logOut(it) }
            sessions.clear()
            state = SettingsState.LoggedOut
        }
    }
}
