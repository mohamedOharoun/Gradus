package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.SearchProblem

// Los textos de la pantalla Buscar, como funciones puras: sin Compose, para probarlas en el
// ordenador (SearchTextsTest) igual que CourseTexts.

// Título y texto de un aviso de la pantalla (un problema o una lista vacía). Sin texto, `body` va vacío.
data class SearchMessage(val title: String, val body: String = "")

const val MORE_FAILED_TEXT = "No se han podido cargar más cursos."

// «1 resultado», «8 resultados», «0 resultados».
fun resultsCountText(count: Int): String = if (count == 1) "1 resultado" else "$count resultados"

fun SearchProblem.toText(): SearchMessage = when (this) {
    SearchProblem.UNREACHABLE ->
        SearchMessage("Sin conexión", "No se ha podido buscar. Comprueba la conexión y vuelve a intentarlo.")
    SearchProblem.SERVER_FAILURE ->
        SearchMessage("Algo ha fallado", "El servidor no ha respondido bien. Vuelve a intentarlo en un momento.")
}

// Con texto o etiquetas, nada coincide; sin ellas, es que el catálogo está vacío.
// Un texto en blanco cuenta como «sin texto» (CourseQuery.hasText).
fun emptyResultsText(query: CourseQuery): SearchMessage =
    if (query.hasText || query.tags.isNotEmpty()) {
        SearchMessage("No hay cursos que coincidan", "Prueba con otras palabras.")
    } else {
        SearchMessage("Todavía no hay cursos publicados")
    }
