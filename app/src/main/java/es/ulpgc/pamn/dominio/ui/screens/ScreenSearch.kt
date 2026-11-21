package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.foundation.layout.Column
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
import es.ulpgc.pamn.dominio.ui.components.SearchField
import es.ulpgc.pamn.dominio.ui.screens.search.SearchActions
import es.ulpgc.pamn.dominio.ui.screens.search.SearchResultsContent

// Pestaña Buscar (Figma «3 · Buscar»), sin estado: pinta lo que recibe y avisa con `actions`.
// `text` es lo que hay en la caja (se está tecleando); `search`, la consulta aplicada y sus resultados.
// Aún sin la fila de filtros (tarea 7). Las previews están en search/ScreenSearchPreviews.kt.
@Composable
fun ScreenSearch(
    text: String,
    search: CourseSearchState,
    actions: SearchActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 20.dp)) {
            Text(
                text = "Buscar cursos",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp).semantics { heading() },
            )
            SearchField(value = text, onValueChange = actions.onTextChange, onSearch = actions.onSearch)
            SearchResultsContent(search = search, actions = actions, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}
