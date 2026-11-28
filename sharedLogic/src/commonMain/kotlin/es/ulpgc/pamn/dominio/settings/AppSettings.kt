package es.ulpgc.pamn.dominio.settings

// Las preferencias de la app que se cambian en Ajustes (docs/tareas/ajustes.md): el modo oscuro y
// las etiquetas preferidas (fase 3). El idioma no va aquí: lo guarda Android (LocaleManager).
// `darkMode`: null = el del sistema (decisión B: mientras no se toque el interruptor).
data class AppSettings(
    val darkMode: Boolean? = null,
    val preferredTags: PreferredTags = PreferredTags(),
) {
    fun isDark(systemDark: Boolean): Boolean = darkMode ?: systemDark
}
