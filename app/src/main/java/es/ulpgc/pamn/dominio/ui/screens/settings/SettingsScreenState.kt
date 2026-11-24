package es.ulpgc.pamn.dominio.ui.screens.settings

// Lo que enseña la pantalla de Ajustes (ajustes.md, tarea 5). A Ajustes solo se llega con la
// sesión iniciada, así que siempre hay nombre y correo.
// `darkMode`: cómo está ahora la app (lo elegido o, si no se ha tocado, el del sistema; decisión B).
data class SettingsScreenState(
    val name: String,
    val email: String,
    val darkMode: Boolean,
    // Fase 3: las etiquetas preferidas, en su orden.
    val preferredTags: List<String> = emptyList(),
)

// Lo que se puede hacer hoy. El idioma (fase 3b) aún no
// funciona: salen desactivados y no necesitan acción (decisión A).
class SettingsActions(
    val onDarkModeChange: (Boolean) -> Unit,
    val onLogOut: () -> Unit,
    // Fase 2: abren «Editar perfil», «Cambiar contraseña» y el diálogo «Eliminar cuenta».
    val onEditProfile: () -> Unit = {},
    val onChangePassword: () -> Unit = {},
    val onDeleteAccount: () -> Unit = {},
    // Fase 3: abre «Etiquetas preferidas».
    val onEditTags: () -> Unit = {},
)
