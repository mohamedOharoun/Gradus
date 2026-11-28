package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.search.DraftCount
import es.ulpgc.pamn.dominio.course.search.FilterSheetState
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.text.asString

// Lo que hay dentro de la hoja de filtros (Figma «4 · Buscar con filtros»): cabecera, las secciones
// Etiquetas, Creador y Fecha de publicación (en FilterSheetSections) y «Mostrar N cursos». Sin estado
// y sin ventana, para poder verla en una preview. `count` (lo que se sabe del recuento del borrador)
// llega ya calculado. `onApply` y `onClose` llegan aparte de `actions` porque FilterSheet los envuelve
// con la animación de cierre; el resto se avisa directamente con `actions`.
@Composable
fun FilterSheetContent(
    state: FilterSheetState,
    count: DraftCount,
    actions: SearchActions,
    onApply: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        SheetHeader(canReset = state.canReset, onReset = actions.onResetFilters, onClose = onClose)
        // Las tres secciones se desplazan juntas (con «Ver todas» o con el teclado abierto no caben)
        // y el botón se queda abajo (decisión H de la tarea 11).
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            TagsSection(state, actions)
            Spacer(Modifier.height(SECTION_GAP))
            CreatorSection(state, actions)
            Spacer(Modifier.height(SECTION_GAP))
            PublishedSection(state, actions)
        }
        // Sin cursos que mostrar no se puede pulsar (decisión C); con «Contando» sí, para no
        // bloquear al usuario. Sin `liveRegion`: TalkBack no anuncia el número al cambiar (decisión G).
        PrimaryButton(
            text = showCoursesText(count).asString(),
            onClick = onApply,
            enabled = count != DraftCount.Known(0),
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
        )
    }
}

// El hueco entre una sección y la siguiente.
private val SECTION_GAP = 16.dp

@Composable
private fun SheetHeader(canReset: Boolean, onReset: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = FILTERS_LABEL.asString(),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        // Desactivado sin filtros que quitar (decisión F; con creador y fecha, decisión D de la
        // tarea 11): Material ya lo atenúa.
        TextButton(onClick = onReset, enabled = canReset, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(RESET_FILTERS_TEXT.asString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
            Icon(painterResource(R.drawable.ic_close), contentDescription = CLOSE_FILTERS_DESCRIPTION.asString())
        }
    }
}
