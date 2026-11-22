package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.course.search.CourseSearchState
import es.ulpgc.pamn.dominio.course.search.FilterSheetState
import es.ulpgc.pamn.dominio.ui.components.SearchField
import es.ulpgc.pamn.dominio.ui.screens.search.FilterBar
import es.ulpgc.pamn.dominio.ui.screens.search.FilterSheet
import es.ulpgc.pamn.dominio.ui.screens.search.SearchActions
import es.ulpgc.pamn.dominio.ui.screens.search.SearchResultsContent

// El margen de los lados de la pantalla.
private val ScreenMargin = 20.dp

// Pestaña Buscar (Figma «3 · Buscar»), sin estado: pinta lo que recibe y avisa con `actions`.
// `text` es lo que hay en la caja (se está tecleando); `search`, la consulta aplicada y sus resultados;
// `filters`, la hoja de filtros (`null` = cerrada). Las previews están en search/ScreenSearchPreviews.kt.
@Composable
fun ScreenSearch(
    text: String,
    search: CourseSearchState,
    filters: FilterSheetState?,
    actions: SearchActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // El margen de los lados no va en la columna sino en cada hijo: la fila de filtros ocupa todo el
        // ancho y lo lleva como relleno interior, así los chips que no caben pasan por debajo del borde
        // de la pantalla en lugar de cortarse en el margen.
        val margin = Modifier.padding(horizontal = ScreenMargin)
        Column(Modifier.safeDrawingPadding()) {
            Text(
                text = "Buscar cursos",
                style = MaterialTheme.typography.headlineLarge,
                modifier = margin.padding(top = 24.dp, bottom = 16.dp).semantics { heading() },
            )
            SearchField(value = text, onValueChange = actions.onTextChange, onSearch = actions.onSearch, modifier = margin)
            FilterBar(
                activeFilters = search.query.activeFilters,
                actions = actions,
                contentPadding = PaddingValues(horizontal = ScreenMargin),
            )
            SearchResultsContent(search = search, actions = actions, modifier = margin.weight(1f).fillMaxWidth())
        }
        // La hoja es una ventana aparte que se dibuja encima: da igual en qué sitio del árbol se ponga.
        filters?.let { FilterSheet(state = it, actions = actions) }
    }
}
