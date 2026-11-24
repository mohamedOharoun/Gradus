package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Un bloque de Ajustes (Figma 6): el título en mayúsculas («CUENTA») y debajo una tarjeta con
// filas. El título es un encabezado para TalkBack: se puede saltar de sección en sección.
// Las filas se separan con SettingsDivider.
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(content = content)
        }
    }
}

// La raya fina entre dos filas de una tarjeta de Ajustes.
@Composable
fun SettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = MaterialTheme.colorScheme.outlineVariant)
}

@LightDarkPreviews
@Composable
private fun SettingsSectionPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsSection(title = "Sesión", modifier = Modifier.padding(16.dp)) {
                SettingsRow(icon = R.drawable.ic_logout, label = "Cerrar sesión", onClick = {}, showChevron = false)
                SettingsDivider()
                SettingsRow(
                    icon = R.drawable.ic_delete, label = "Eliminar cuenta", onClick = {},
                    showChevron = false, danger = true, enabled = false,
                )
            }
        }
    }
}
