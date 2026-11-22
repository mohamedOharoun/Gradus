package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.search.FilterSheetState
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.components.SelectableTagChip

// Lo que hay dentro de la hoja de filtros (Figma «4 · Buscar con filtros», solo la sección
// Etiquetas): cabecera, etiquetas y «Mostrar cursos». Sin estado y sin ventana, para poder verla
// en una preview. `onApply` y `onClose` llegan aparte de `actions` porque FilterSheet los envuelve
// con la animación de cierre; el resto se avisa directamente con `actions`.
@Composable
fun FilterSheetContent(
    state: FilterSheetState,
    actions: SearchActions,
    onApply: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        SheetHeader(canReset = state.canReset, onReset = actions.onResetFilters, onClose = onClose)
        // Si hay muchas etiquetas («Ver todas») la sección se desplaza y el botón se queda abajo.
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            TagsSection(state, actions)
        }
        PrimaryButton(text = SHOW_COURSES_TEXT, onClick = onApply, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))
    }
}

@Composable
private fun SheetHeader(canReset: Boolean, onReset: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = FILTERS_LABEL,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        // Desactivado sin etiquetas que quitar (decisión F): Material ya lo atenúa.
        TextButton(onClick = onReset, enabled = canReset, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(RESET_FILTERS_TEXT, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        // En esta versión de Material 3 el IconButton mide 40 dp: se fuerza a 48 dp (como en SearchField).
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = CLOSE_FILTERS_DESCRIPTION)
        }
    }
}

@Composable
private fun TagsSection(state: FilterSheetState, actions: SearchActions) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
        Text(
            text = TAGS_SECTION_TITLE,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = TAGS_SECTION_HINT,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (state.visibleTags.isEmpty()) {
        Text(
            text = NO_TAGS_TEXT,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        return
    }
    // El espacio vertical es 0: cada chip ya reserva 48 dp de alto para el toque
    // (`minimumInteractiveComponentSize`), así que otro hueco entre filas sería de más.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        state.visibleTags.forEach { tag ->
            // `key` identifica cada chip por su etiqueta: al pulsar «Ver todas» se añaden chips y
            // los que ya estaban conservan su sitio.
            key(tag) {
                SelectableTagChip(
                    tag = tag,
                    selected = state.isSelected(tag),
                    onToggle = { actions.onToggleTag(tag) },
                )
            }
        }
        // Al pulsarlo desaparece; no hay «Ver menos» (decisión F).
        if (state.canShowAll) ShowAllChip(onClick = actions.onShowAllTags)
    }
}

// «Ver todas»: del mismo tamaño que un chip de etiqueta pero con el borde discontinuo del Figma.
// Material no tiene bordes discontinuos, así que se dibuja a mano con `drawBehind`.
@Composable
private fun ShowAllChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        // Como FiltersButton: el rol de botón para que TalkBack lo anuncie (Surface no lo pone solo).
        modifier = modifier.minimumInteractiveComponentSize().semantics { role = Role.Button },
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = color,
    ) {
        Row(
            modifier = Modifier.height(32.dp).dashedBorder(color).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(SHOW_ALL_TAGS_TEXT, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(size.height / 2),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        ),
    )
}
