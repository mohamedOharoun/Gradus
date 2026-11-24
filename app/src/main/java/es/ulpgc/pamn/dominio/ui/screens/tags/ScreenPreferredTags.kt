package es.ulpgc.pamn.dominio.ui.screens.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.ui.components.FormField
import es.ulpgc.pamn.dominio.ui.components.FormTopBar
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.components.RemovableFilterChip

// «Etiquetas preferidas» (Ajustes → Temas que te interesan → Editar; docs/tareas/ajustes.md, fase 3).
// No está en Figma: sigue el estilo de los formularios. Sin estado. Cada cambio se guarda al momento.
// Previews en ScreenPreferredTagsPreviews.kt.
@Composable
fun ScreenPreferredTags(
    state: PreferredTagsUiState,
    actions: PreferredTagsActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            FormTopBar(title = "Etiquetas preferidas", onBack = actions.onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Decisión N: hasta que Buscar las use, «Pronto».
                Text(
                    "Pronto verás antes los cursos con estas etiquetas en Inicio y en Buscar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FormField(
                    label = "Nueva etiqueta",
                    value = state.draft,
                    onValueChange = actions.onDraftChange,
                    error = state.problem?.message(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                PrimaryButton(text = "Añadir", onClick = actions.onAdd)
                ChosenTags(state.tags, actions.onRemove)
            }
        }
    }
}

// Las elegidas, cada una con su ✕ (todo el chip la quita, como los filtros de Buscar).
@Composable
private fun ChosenTags(tags: List<String>, onRemove: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Tus etiquetas",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(countText(tags.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (tags.isEmpty()) {
            Text("Aún no has elegido ninguna.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    RemovableFilterChip(label = tagLabel(tag), removeDescription = removeTagDescription(tag), onRemove = { onRemove(tag) })
                }
            }
        }
    }
}
