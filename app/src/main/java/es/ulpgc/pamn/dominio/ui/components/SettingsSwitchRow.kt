package es.ulpgc.pamn.dominio.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Una fila de Ajustes con interruptor («Modo oscuro»). Se pulsa en cualquier parte de la fila,
// no solo en el interruptor. Para TalkBack es un solo interruptor con su estado:
// «Modo oscuro, interruptor, activado».
@Composable
fun SettingsSwitchRow(
    @DrawableRes icon: Int,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsRowLayout(
        icon = icon,
        label = label,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        // onCheckedChange = null: el interruptor solo se dibuja; quien escucha el toque es la fila.
        Switch(checked = checked, onCheckedChange = null)
    }
}

@LightDarkPreviews
@Composable
private fun SettingsSwitchRowPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column {
                var dark by remember { mutableStateOf(true) }
                SettingsSwitchRow(icon = R.drawable.ic_dark_mode, label = "Modo oscuro", checked = dark, onCheckedChange = { dark = it })
                SettingsSwitchRow(icon = R.drawable.ic_dark_mode, label = "Modo oscuro", checked = false, onCheckedChange = {})
            }
        }
    }
}
