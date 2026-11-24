package es.ulpgc.pamn.dominio.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Lo que se ve en lugar del valor cuando algo aún no funciona (ajustes.md, decisión A).
const val COMING_SOON = "Próximamente"

// Transparencia del contenido desactivado (la que usa Material 3).
internal const val DISABLED_ALPHA = 0.38f

// Una fila pulsable de Ajustes (Figma 6): icono, texto, valor opcional («Español») y ›.
// - `enabled = false`: aún no funciona. Se ve atenuada con «Próximamente» en lugar del valor y no
//   se puede pulsar; TalkBack la lee «desactivada» (decisión A).
// - `danger = true`: acción peligrosa («Eliminar cuenta»), en rojo.
// Toda la fila es un solo botón para TalkBack: «Idioma, Español, botón».
@Composable
fun SettingsRow(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val base = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val color = if (enabled) base else base.copy(alpha = DISABLED_ALPHA)
    val shownValue = if (enabled) value else COMING_SOON

    SettingsRowLayout(
        icon = icon,
        label = label,
        color = color,
        modifier = modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        if (shownValue != null) {
            Text(
                text = shownValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.dimmedUnless(enabled),
            )
        }
        if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.dimmedUnless(enabled),
            )
        }
    }
}

internal fun Color.dimmedUnless(enabled: Boolean): Color = if (enabled) this else copy(alpha = DISABLED_ALPHA)

@LightDarkPreviews
@Composable
private fun SettingsRowPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsRow(icon = R.drawable.ic_language, label = "Idioma", value = "Español", onClick = {})
                SettingsRow(icon = R.drawable.ic_language, label = "Idioma", value = "Español", onClick = {}, enabled = false)
                SettingsRow(icon = R.drawable.ic_logout, label = "Cerrar sesión", onClick = {}, showChevron = false)
                SettingsRow(icon = R.drawable.ic_delete, label = "Eliminar cuenta", onClick = {}, showChevron = false, danger = true)
            }
        }
    }
}
