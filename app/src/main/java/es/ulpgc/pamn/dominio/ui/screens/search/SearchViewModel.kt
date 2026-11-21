package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.course.CourseGateway
import es.ulpgc.pamn.dominio.course.search.CourseSearchState
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

    // Dos trabajos independientes: la espera al teclear y la petición en camino. Así, «Cargar más» o
    // «Reintentar» durante la espera no se la comen: la búsqueda tecleada llega igual a los 300 ms.
    private var typing: Job? = null
    private var request: Job? = null

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
