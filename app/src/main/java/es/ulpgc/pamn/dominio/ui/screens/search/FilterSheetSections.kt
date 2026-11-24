package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.course.search.FilterSheetState
import es.ulpgc.pamn.dominio.course.search.PublishedWithin
import es.ulpgc.pamn.dominio.ui.components.SelectableTagChip
import es.ulpgc.pamn.dominio.ui.components.SingleChoiceRow

// Las tres secciones de la hoja de filtros (Figma «4 · Buscar con filtros»): Etiquetas, Creador y
// Fecha de publicación. Aparte de FilterSheetContent para que ese fichero solo tenga la cabecera y
// el botón. Sin estado: reciben el borrador (`FilterSheetState`) y avisan con `SearchActions`.
// Cada sección tiene varios elementos (título y contenido): van en un `Column` que recibe el `modifier`,
// así el que la usa coloca la sección entera.

// El título de una sección, con el estilo y el rol de encabezado de todas (TalkBack salta de
// encabezado en encabezado). `hint` es la nota pequeña que va a su lado («· elige una o varias»).
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, hint: String? = null) {
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier.padding(top = 8.dp, bottom = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        if (hint != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TagsSection(state: FilterSheetState, actions: SearchActions, modifier: Modifier = Modifier) {
    Column(modifier) {
        SectionTitle(title = TAGS_SECTION_TITLE, hint = TAGS_SECTION_HINT)
        if (state.visibleTags.isEmpty()) {
            Text(
                text = NO_TAGS_TEXT,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
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
    }
}

// «Creador»: el campo enseña el creador del borrador, tal como se escribió (decisión B de la tarea 11).
@Composable
fun CreatorSection(state: FilterSheetState, actions: SearchActions, modifier: Modifier = Modifier) {
    Column(modifier) {
        SectionTitle(title = CREATOR_FIELD_DESCRIPTION)
        CreatorField(value = state.draft.author, onValueChange = actions.onAuthorChange)
    }
}

// «Fecha de publicación»: los tres botones del Figma, con la fecha del borrador elegida.
@Composable
fun PublishedSection(state: FilterSheetState, actions: SearchActions, modifier: Modifier = Modifier) {
    Column(modifier) {
        SectionTitle(title = PUBLISHED_SECTION_TITLE)
        SingleChoiceRow(
            options = PublishedWithin.entries,
            selected = state.draft.published,
            label = ::publishedLabel,
            onSelect = actions.onPublishedChange,
        )
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
