package es.ulpgc.pamn.dominio.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.settings.AppLanguage
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Elegir el idioma de la app (fase 3, decisiones O y S): El del sistema, Español o English. Tocar uno lo
// elige y cierra el diálogo. Sin estado: lo elegido viene por parámetro y se avisa con `onChoose`.
@Composable
fun LanguageDialog(
    selected: AppLanguage,
    onChoose: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.language_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.language_dialog_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // selectableGroup: TalkBack lee «1 de 3» y cada opción como botón de opción.
                Column(Modifier.selectableGroup()) {
                    AppLanguage.entries.forEach { language ->
                        LanguageOption(language, selected = language == selected, onClick = { onChoose(language) })
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

// Toda la fila se toca (48 dp de alto como mínimo); el botón de opción solo dibuja.
@Composable
private fun LanguageOption(language: AppLanguage, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(language.label().asString(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}

@LightDarkPreviews
@Composable
private fun LanguageDialogPreview() {
    GradusTheme {
        Surface { LanguageDialog(selected = AppLanguage.SYSTEM, onChoose = {}, onDismiss = {}) }
    }
}
