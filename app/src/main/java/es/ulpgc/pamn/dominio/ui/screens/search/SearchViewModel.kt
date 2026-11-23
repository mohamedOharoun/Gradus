package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.course.CourseGateway
import es.ulpgc.pamn.dominio.course.search.ActiveFilter
import es.ulpgc.pamn.dominio.course.search.CourseSearchState
import es.ulpgc.pamn.dominio.course.search.FilterSheetState
import es.ulpgc.pamn.dominio.course.search.PublishedWithin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// La pestaña Buscar: lo que hay escrito en la caja y la lista de resultados. La lógica de la lista
// (páginas, fallos, respuestas que llegan tarde) es de CourseSearchState, en sharedLogic; aquí solo
// se decide CUÁNDO pedir y se habla con el gateway, que llega por constructor.
class SearchViewModel(private val gateway: CourseGateway) : ViewModel() {

    companion object {
        // Tras esta pausa sin teclear se busca (decisión 4).
        val TYPING_PAUSE = 300.milliseconds

        // Factory method, como CourseViewModel.factory.
        fun factory(gateway: CourseGateway) = viewModelFactory {
            initializer { SearchViewModel(gateway) }
        }
    }

    // Lo que hay en la caja. Puede ir por delante de search.query mientras se espera la pausa.
    var text by mutableStateOf("")
        private set

    // La consulta aplicada y sus resultados.
    var search by mutableStateOf(CourseSearchState.start())
        private set

    // La hoja de filtros abierta, con su borrador; `null` = cerrada. Vive aquí (y no en la pantalla)
    // para que girar el móvil con la hoja abierta no la pierda.
    var filters by mutableStateOf<FilterSheetState?>(null)
        private set

    // Dos trabajos independientes: la espera al teclear y la petición en camino. Así, «Cargar más» o
    // «Reintentar» durante la espera no se la comen: la búsqueda tecleada llega igual a los 300 ms.
    private var typing: Job? = null
    private var request: Job? = null

    // El recuento del borrador de la hoja («Mostrar N cursos», tarea 8): la pausa y su petición en un
    // solo trabajo, para cancelar las dos de una vez (decisión E). Contar no toca la lista.
    private var preview: Job? = null

    init {
        fetch()  // al abrir Buscar: todos los cursos, sin esperar
    }

    fun onTextChange(text: String) {
        this.text = text
        typing?.cancel()
        typing = viewModelScope.launch {
            delay(TYPING_PAUSE)
            searchText()
        }
    }

    // Tecla «Buscar»: no espera, y la espera pendiente ya no hace falta.
    fun onSearch() {
        typing?.cancel()
        searchText()
    }

    fun onLoadMore() = moveTo(search.loadMore())

    fun onRetry() = moveTo(search.retry())

    // --- Hoja de filtros (decisión 7 del documento de la tarea) ---

    // Las sugerencias salen de los resultados ya cargados. Si ya está abierta (doble toque rápido),
    // no se vuelve a abrir: se perdería el borrador.
    fun onOpenFilters() {
        if (filters == null) filters = FilterSheetState.open(search.query, search.results)
    }

    fun onToggleTag(tag: String) = editDraft { it.toggle(tag) }

    fun onResetFilters() = editDraft(FilterSheetState::reset)

    fun onShowAllTags() = editFilters(FilterSheetState::showAll)

    // «Mostrar N cursos»: si el recuento terminó y es del borrador de ahora, sus resultados pasan a
    // ser la lista sin pedir nada (decisión D). Si no, busca con el borrador y el texto de la caja,
    // una sola vez (decisión A). La espera al teclear se cancela siempre: o ya va incluida en esta
    // búsqueda o repetiría la misma.
    fun onApplyFilters() {
        val sheet = filters ?: return
        filters = null
        preview?.cancel()
        typing?.cancel()
        val counted = sheet.previewToApply(text)
        if (counted != null) {
            useCounted(counted)
        } else {
            sheet.queryToApply(text, search.query)?.let { moveTo(search.search(it)) }
        }
    }

    // ✕, deslizar, tocar fuera o «atrás»: descarta el borrador y su recuento. La espera al teclear sigue.
    fun onCloseFilters() {
        filters = null
        preview?.cancel()
    }

    // Chip de la fila (decisión B): quita el filtro y busca al momento, con el texto de la caja.
    // El creador se quita entero y la fecha vuelve a «Cualquiera» (tarea 11a-2).
    fun onRemoveFilter(filter: ActiveFilter) {
        typing?.cancel()
        val query = when (filter) {
            is ActiveFilter.Tag -> search.query.withoutTag(filter.name)
            is ActiveFilter.Author -> search.query.withoutAuthor()
            is ActiveFilter.Published -> search.query.withPublished(PublishedWithin.ANY)
        }
        moveTo(search.search(query.withText(text)))
    }

    // Cambia la hoja solo si está abierta; cerrada, no hace nada.
    private fun editFilters(change: (FilterSheetState) -> FilterSheetState) {
        filters = filters?.let(change)
    }

    // Cambia el borrador y vuelve a contar (decisión E): cancela la pausa y la petición en curso (el
    // recuento a medias se olvida con `cancelPreview`, tarea 8c-1) y, si el borrador pide algo que no
    // se sabe ya, cuenta tras la pausa. Si vuelve a pedir lo aplicado o lo ya contado, solo cancela.
    private fun editDraft(change: (FilterSheetState) -> FilterSheetState) {
        val sheet = filters ?: return
        preview?.cancel()
        val edited = change(sheet).cancelPreview()
        filters = edited
        if (edited.previewNeeded(text, search.query)) {
            preview = viewModelScope.launch {
                delay(TYPING_PAUSE)
                countDraft()
            }
        }
    }

    // Empieza a contar con el texto de ahora y pide la página 0 entera: si se aplica, los resultados
    // ya están. La respuesta va a la hoja (cerrarla cancela este trabajo; si aun así llegara una
    // respuesta vieja, CourseSearchState la ignora).
    private suspend fun countDraft() {
        val counting = filters?.startPreview(text) ?: return
        filters = counting
        val pending = counting.preview?.pending ?: return
        val outcome = gateway.search(pending.query, pending.page)
        filters = filters?.receivePreview(pending, outcome)
    }

    // Los resultados del recuento pasan a ser la lista. La petición de la lista que hubiera en camino
    // ya no es de lo aplicado: se cancela (y si llegara, `receive` la ignoraría: no hay nada pendiente).
    private fun useCounted(counted: CourseSearchState) {
        request?.cancel()
        search = counted
    }

    // Siempre busca, aunque el texto no haya cambiado (la máquina vuelve a empezar).
    private fun searchText() = moveTo(search.search(search.query.withText(text)))

    // Si la máquina devuelve el mismo estado, la acción no tocaba (no hay más, ya está cargando, no
    // ha fallado...) y no se cancela nada.
    private fun moveTo(next: CourseSearchState) {
        if (next === search) return
        search = next
        fetch()
    }

    // Pide la petición pendiente. Una nueva cancela la anterior (gana la última); si aun así llegara
    // una respuesta vieja, CourseSearchState.receive la ignora.
    private fun fetch() {
        val pending = search.pending ?: return
        request?.cancel()
        request = viewModelScope.launch {
            val outcome = gateway.search(pending.query, pending.page)
            search = search.receive(pending, outcome)
        }
    }
}

// Las acciones de la pantalla conectadas a este ViewModel, para que MainNavGraph las pase con una
// sola llamada. Es una extensión, fuera de la clase: el ViewModel no depende de SearchActions, y
// SearchActions sigue sin depender de nada (previews y tests). La navegación no es del ViewModel:
// `onCourseClick` llega de fuera y pasa tal cual.
fun SearchViewModel.actions(onCourseClick: (String) -> Unit): SearchActions = SearchActions(
    onTextChange = this::onTextChange,
    onSearch = this::onSearch,
    onLoadMore = this::onLoadMore,
    onRetry = this::onRetry,
    onCourseClick = onCourseClick,
    onOpenFilters = this::onOpenFilters,
    onToggleTag = this::onToggleTag,
    onResetFilters = this::onResetFilters,
    onShowAllTags = this::onShowAllTags,
    onApplyFilters = this::onApplyFilters,
    onCloseFilters = this::onCloseFilters,
    onRemoveFilter = this::onRemoveFilter,
)
