package es.ulpgc.pamn.dominio.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.ProfileRow
import es.ulpgc.pamn.dominio.ui.components.SettingsDivider
import es.ulpgc.pamn.dominio.ui.components.SettingsRow
import es.ulpgc.pamn.dominio.ui.components.SettingsSection
import es.ulpgc.pamn.dominio.ui.components.SettingsSwitchRow
import es.ulpgc.pamn.dominio.ui.components.TagLabel
import es.ulpgc.pamn.dominio.ui.screens.tags.tagLabel
import es.ulpgc.pamn.dominio.ui.text.asString

// Las cuatro secciones del Figma 6, una función cada una. Lo que aún no funciona va con
// `enabled = false` (decisión A): se ve, atenuado y con «Próximamente».

@Composable
internal fun AccountSection(state: SettingsScreenState, actions: SettingsActions, modifier: Modifier = Modifier) {
    SettingsSection(title = stringResource(R.string.settings_section_account), modifier = modifier) {
        ProfileRow(name = state.name, email = state.email, onEdit = actions.onEditProfile)
        SettingsDivider()
        SettingsRow(
            icon = R.drawable.ic_lock_outline,
            label = stringResource(R.string.field_password),
            value = stringResource(R.string.action_change),
            onClick = actions.onChangePassword,
        )
    }
}

@Composable
internal fun AppearanceSection(state: SettingsScreenState, actions: SettingsActions, modifier: Modifier = Modifier) {
    SettingsSection(title = stringResource(R.string.settings_section_appearance), modifier = modifier) {
        SettingsSwitchRow(
            icon = R.drawable.ic_dark_mode,
            label = stringResource(R.string.settings_dark_mode),
            checked = state.darkMode,
            onCheckedChange = actions.onDarkModeChange,
        )
        SettingsDivider()
        // Decisión V: en Android 10–12 no se puede elegir; la fila dice que sigue al sistema.
        SettingsRow(
            icon = R.drawable.ic_language,
            label = stringResource(R.string.settings_language),
            value = state.language.label().asString(),
            onClick = actions.onChooseLanguage,
            enabled = state.canChooseLanguage,
            disabledValue = stringResource(R.string.language_system),
        )
    }
}

// Fase 3: las etiquetas elegidas como en los cursos («#grafos») y «Editar» para cambiarlas.
// Decisión N: hasta que Buscar las use, la frase dice «Pronto».
@Composable
internal fun InterestsSection(state: SettingsScreenState, actions: SettingsActions, modifier: Modifier = Modifier) {
    SettingsSection(title = stringResource(R.string.settings_section_interests), modifier = modifier) {
        SettingsRow(
            icon = R.drawable.ic_tag, label = stringResource(R.string.settings_preferred_tags), value = stringResource(R.string.action_edit),
            onClick = actions.onEditTags, showChevron = false,
        )
        Column(
            modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.tags_soon),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.preferredTags.isEmpty()) {
                Text(stringResource(R.string.tags_none), style = MaterialTheme.typography.bodySmall)
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
    SettingsSection(title = stringResource(R.string.settings_section_session), modifier = modifier) {
        SettingsRow(icon = R.drawable.ic_logout, label = stringResource(R.string.settings_log_out), onClick = actions.onLogOut, showChevron = false)
        SettingsDivider()
        SettingsRow(
            icon = R.drawable.ic_delete, label = stringResource(R.string.action_delete_account), onClick = actions.onDeleteAccount,
            showChevron = false, danger = true,
        )
    }
}
