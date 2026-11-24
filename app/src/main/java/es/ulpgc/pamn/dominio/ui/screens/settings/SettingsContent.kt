package es.ulpgc.pamn.dominio.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.ProfileRow
import es.ulpgc.pamn.dominio.ui.components.SettingsDivider
import es.ulpgc.pamn.dominio.ui.components.SettingsRow
import es.ulpgc.pamn.dominio.ui.components.SettingsSection
import es.ulpgc.pamn.dominio.ui.components.SettingsSwitchRow
import es.ulpgc.pamn.dominio.ui.components.TagLabel
import es.ulpgc.pamn.dominio.ui.screens.tags.tagLabel

// Las cuatro secciones del Figma 6, una función cada una. Lo que aún no funciona va con
// `enabled = false` (decisión A): se ve, atenuado y con «Próximamente».

@Composable
internal fun AccountSection(state: SettingsScreenState, actions: SettingsActions, modifier: Modifier = Modifier) {
    SettingsSection(title = "Cuenta", modifier = modifier) {
        ProfileRow(name = state.name, email = state.email, onEdit = actions.onEditProfile)
        SettingsDivider()
        SettingsRow(icon = R.drawable.ic_lock_outline, label = "Contraseña", value = "Cambiar", onClick = actions.onChangePassword)
    }
}

@Composable
internal fun AppearanceSection(state: SettingsScreenState, actions: SettingsActions, modifier: Modifier = Modifier) {
    SettingsSection(title = "Apariencia e idioma", modifier = modifier) {
        SettingsSwitchRow(
            icon = R.drawable.ic_dark_mode,
            label = "Modo oscuro",
            checked = state.darkMode,
            onCheckedChange = actions.onDarkModeChange,
        )
        SettingsDivider()
        SettingsRow(icon = R.drawable.ic_language, label = "Idioma", value = "Español", onClick = {}, enabled = false)
    }
}

// Fase 3: las etiquetas elegidas como en los cursos («#grafos») y «Editar» para cambiarlas.
// Decisión N: hasta que Buscar las use, la frase dice «Pronto».
@Composable
internal fun InterestsSection(state: SettingsScreenState, actions: SettingsActions, modifier: Modifier = Modifier) {
    SettingsSection(title = "Temas que te interesan", modifier = modifier) {
        SettingsRow(
            icon = R.drawable.ic_tag, label = "Etiquetas preferidas", value = "Editar",
            onClick = actions.onEditTags, showChevron = false,
        )
        Column(
            modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Pronto verás antes los cursos con estas etiquetas en Inicio y en Buscar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.preferredTags.isEmpty()) {
                Text("Aún no has elegido ninguna.", style = MaterialTheme.typography.bodySmall)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.preferredTags.forEach { TagLabel(tagLabel(it)) }
                }
            }
        }
    }
}

@Composable
internal fun SessionSection(actions: SettingsActions, modifier: Modifier = Modifier) {
    SettingsSection(title = "Sesión", modifier = modifier) {
        SettingsRow(icon = R.drawable.ic_logout, label = "Cerrar sesión", onClick = actions.onLogOut, showChevron = false)
        SettingsDivider()
        SettingsRow(
            icon = R.drawable.ic_delete, label = "Eliminar cuenta", onClick = actions.onDeleteAccount,
            showChevron = false, danger = true,
        )
    }
}
