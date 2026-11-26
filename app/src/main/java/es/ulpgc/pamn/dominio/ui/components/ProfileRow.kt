package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.account.initialsOf
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// La fila del perfil en Ajustes (Figma 6): avatar con las iniciales, nombre, correo y «Editar».
// - Las iniciales son decorativas: TalkBack lee el nombre entero, no «M O».
// - `editEnabled = false`: editar aún no funciona (fase 2): «Próximamente» y no se puede pulsar.
// Para TalkBack es un solo elemento: «Mohamed Oharoun, nombre@ejemplo.com, Editar, botón».
@Composable
fun ProfileRow(
    name: String,
    email: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    editEnabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .clickable(enabled = editEnabled, role = Role.Button, onClick = onEdit)
            .defaultMinSize(minHeight = 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Avatar(initials = initialsOf(name))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // «Editar» como enlace morado; «Próximamente» igual que en SettingsRow (gris atenuado).
        if (editEnabled) {
            Text(text = stringResource(R.string.action_edit), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        } else {
            Text(
                text = stringResource(R.string.coming_soon),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.dimmedUnless(false),
            )
        }
    }
}

// Círculo morado con las iniciales. TalkBack no lo lee (el nombre ya está al lado).
@Composable
private fun Avatar(initials: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@LightDarkPreviews
@Composable
private fun ProfileRowPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column {
                ProfileRow(name = "Mohamed Oharoun", email = "nombre@ejemplo.com", onEdit = {})
                ProfileRow(name = "Ana", email = "ana@ejemplo.com", onEdit = {}, editEnabled = false)
            }
        }
    }
}
