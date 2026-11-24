package es.ulpgc.pamn.dominio.data

import android.content.SharedPreferences
import es.ulpgc.pamn.dominio.settings.AppSettings
import es.ulpgc.pamn.dominio.settings.PreferredTags
import es.ulpgc.pamn.dominio.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// Adaptador del puerto SettingsStore (ajustes.md, decisión D): guarda las preferencias en el
// móvil con SharedPreferences, que viene con Android. Lee el fichero una vez al crearse; después,
// cada cambio se escribe en el fichero y se avisa por el StateFlow (la app cambia el tema al momento).
class SharedPreferencesSettingsStore(private val prefs: SharedPreferences) : SettingsStore {

    private val state = MutableStateFlow(read())

    override val settings: StateFlow<AppSettings> = state

    override suspend fun setDarkMode(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply()
        state.update { it.copy(darkMode = dark) }
    }

    // Fase 3: una etiqueta por línea. No se usa putStringSet porque un Set no guarda el orden, y las
    // etiquetas se ven en el orden en que se añadieron. Una etiqueta nunca lleva un salto de línea:
    // PreferredTags las normaliza con tagKey(), que convierte cualquier espacio en « ».
    override suspend fun setPreferredTags(tags: PreferredTags) {
        prefs.edit().putString(KEY_PREFERRED_TAGS, tags.values.joinToString(SEPARATOR)).apply()
        state.update { it.copy(preferredTags = tags) }
    }

    // Sin la clave = nunca se ha tocado el interruptor = null (sigue al sistema, decisión B).
    // Las etiquetas vuelven a pasar por las reglas al leerlas (PreferredTags.of): nunca entra basura.
    private fun read(): AppSettings = AppSettings(
        darkMode = if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else null,
        preferredTags = PreferredTags.of(prefs.getString(KEY_PREFERRED_TAGS, "").orEmpty().split(SEPARATOR)),
    )

    private companion object {
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_PREFERRED_TAGS = "preferred_tags"
        const val SEPARATOR = "\n"
    }
}
