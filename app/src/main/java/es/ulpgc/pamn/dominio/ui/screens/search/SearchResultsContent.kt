package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.search.CourseSearchState
import es.ulpgc.pamn.dominio.course.search.SearchStatus
import es.ulpgc.pamn.dominio.ui.components.CourseCard
import es.ulpgc.pamn.dominio.ui.components.ProblemMessage
import es.ulpgc.pamn.dominio.ui.components.RetryButton
import es.ulpgc.pamn.dominio.ui.text.asString

// Lo que hay debajo de la caja de búsqueda según el estado de CourseSearchState (decide aquí, en un
// solo `if`, qué se enseña). Sin estado: no busca nada, solo avisa con `actions`.
@Composable
fun SearchResultsContent(
    search: CourseSearchState,
    actions: SearchActions,
    modifier: Modifier = Modifier,
) {
    val status = search.status
    val searching = stringResource(R.string.search_searching)
    when {
        // Primera carga: no hay nada que enseñar todavía.
        status is SearchStatus.Loading && search.results.isEmpty() -> Centered(modifier) {
            CircularProgressIndicator(Modifier.semantics { contentDescription = searching })
        }
        status is SearchStatus.Failed -> Centered(modifier) {
            val message = status.problem.toText()
            ProblemMessage(message.title.asString(), body = message.body?.asString().orEmpty(), onRetry = actions.onRetry)
        }
        search.isEmpty -> Centered(modifier) {
            val message = emptyResultsText(search.query)
            ProblemMessage(message.title.asString(), body = message.body?.asString().orEmpty())
        }
        else -> ResultsList(search, actions, modifier)
    }
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ResultsList(search: CourseSearchState, actions: SearchActions, modifier: Modifier) {
    val status = search.status
    val hasFooter = search.canLoadMore || status is SearchStatus.LoadingMore || status is SearchStatus.MoreFailed
    // Una posición de lista por consulta: con otra consulta empieza arriba; al volver de un curso
    // (misma consulta) se recupera donde estaba. rememberSaveable la guarda al salir de la pantalla.
    val listState = rememberSaveable(search.query, saver = LazyListState.Saver) { LazyListState() }
    val searching = stringResource(R.string.search_searching)
    Column(modifier) {
        // Hueco fijo de 4 dp (con 8 dp de aire encima): la barra aparece y desaparece sin empujar la lista.
        Box(Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 4.dp)) {
            if (status is SearchStatus.Loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = searching })
            }
        }
        Text(
            text = resultsCountText(search.total).asString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // liveRegion: TalkBack anuncia el número cuando cambia (al acabar cada búsqueda).
            modifier = Modifier.padding(vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // key = id: Compose reconoce cada tarjeta aunque cambie de puesto (por eso
            // CourseSearchState no deja repetir cursos).
            items(search.results, key = { it.id }) { course ->
                CourseCard(course = course, onClick = { actions.onCourseClick(course.id) })
            }
            // Sin clave propia: en Android las claves deben poder guardarse en un Bundle (un
            // objeto propio no vale) y el pie, que va siempre el último, no la necesita.
            if (hasFooter) {
                item(contentType = "footer") { ListFooter(search, actions) }
            }
        }
    }
}

// El final de la lista: «Cargar más», su carga o su fallo.
@Composable
private fun ListFooter(search: CourseSearchState, actions: SearchActions) {
    // Fuera de semantics {}: ahí no se puede leer strings.xml.
    val loadingMore = stringResource(R.string.search_loading_more)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when (search.status) {
            is SearchStatus.LoadingMore -> CircularProgressIndicator(
                Modifier.padding(12.dp).size(24.dp).semantics { contentDescription = loadingMore },
            )
            is SearchStatus.MoreFailed -> Column(
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    MORE_FAILED_TEXT.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                RetryButton(onClick = actions.onRetry)
            }
            else -> OutlinedButton(onClick = actions.onLoadMore, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.search_load_more))
            }
        }
    }
}
