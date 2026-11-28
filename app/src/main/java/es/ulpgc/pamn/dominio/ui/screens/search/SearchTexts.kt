package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.search.SearchProblem
import es.ulpgc.pamn.dominio.ui.text.UiText

// Los textos de la pantalla Buscar, como funciones puras: sin Compose, para probarlas en el
// ordenador (SearchTextsTest) igual que CourseTexts.
// Desde docs/tareas/ajustes.md, paso 9b, devuelven UiText: cada idioma pone sus palabras en strings.xml.

// Título y texto de un aviso de la pantalla (un problema o una lista vacía). Sin texto, `body` es null.
data class SearchMessage(val title: UiText, val body: UiText? = null)

val MORE_FAILED_TEXT: UiText = UiText.of(R.string.search_more_failed)

// «1 resultado», «8 resultados», «0 resultados» (un plural: cada idioma decide la forma).
fun resultsCountText(count: Int): UiText = UiText.plural(R.plurals.search_results, count)

fun SearchProblem.toText(): SearchMessage = when (this) {
    SearchProblem.UNREACHABLE ->
        SearchMessage(UiText.of(R.string.search_unreachable_title), UiText.of(R.string.search_unreachable_body))
    SearchProblem.SERVER_FAILURE ->
        SearchMessage(UiText.of(R.string.search_server_failure_title), UiText.of(R.string.search_server_failure_body))
}

// Con texto o etiquetas, nada coincide; sin ellas, es que el catálogo está vacío.
// Un texto en blanco cuenta como «sin texto» (CourseQuery.hasText).
fun emptyResultsText(query: CourseQuery): SearchMessage =
    if (query.hasText || query.tags.isNotEmpty()) {
        SearchMessage(UiText.of(R.string.search_no_matches_title), UiText.of(R.string.search_no_matches_body))
    } else {
        SearchMessage(UiText.of(R.string.search_catalog_empty))
    }
