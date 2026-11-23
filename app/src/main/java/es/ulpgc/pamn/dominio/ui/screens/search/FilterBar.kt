package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.course.search.ActiveFilter
import es.ulpgc.pamn.dominio.ui.components.FiltersButton
import es.ulpgc.pamn.dominio.ui.components.RemovableFilterChip

// La fila bajo la caja de búsqueda (Figma «3 · Buscar»): «Filtros n» y un chip quitable por cada
// filtro aplicado. Si no caben, la fila se desplaza en horizontal.
// Sin estado: recibe los filtros y avisa con `actions` (abrir la hoja o quitar un filtro).
//
// `contentPadding` es el margen de los lados, como el de LazyColumn: va *dentro* del desplazamiento
// (el primer chip empieza a esa distancia y el último acaba a ella) y la fila ocupa todo el ancho, de
// modo que los chips que no caben pasan por debajo del borde de la pantalla.
@Composable
fun FilterBar(
    activeFilters: List<ActiveFilter>,
    actions: SearchActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Row(
        // El orden importa: el relleno va después de `horizontalScroll` para que se desplace con el contenido.
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(contentPadding).padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FiltersButton(count = activeFilters.size, onClick = actions.onOpenFilters)
        activeFilters.forEach { filter ->
            // `when` sobre una interfaz `sealed`: el compilador obliga a tratar todos los casos (etiqueta,
            // creador y fecha), así que si se añade otro filtro este código no compilará hasta cubrirlo.
            // Cada rama da lo que se ve y lo que lee TalkBack; el chip es el mismo para todos.
            val (label, removeDescription) = when (filter) {
                is ActiveFilter.Tag -> tagChipLabel(filter.name) to removeFilterDescription(filter.name)
                is ActiveFilter.Author -> authorChipLabel(filter.name) to removeAuthorFilterDescription(filter.name)
                is ActiveFilter.Published ->
                    publishedLabel(filter.within) to removePublishedFilterDescription(filter.within)
            }
            RemovableFilterChip(
                label = label,
                removeDescription = removeDescription,
                onRemove = { actions.onRemoveFilter(filter) },
            )
        }
    }
}
