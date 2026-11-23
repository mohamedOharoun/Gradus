package es.ulpgc.pamn.dominio.ui.screens.search

import es.ulpgc.pamn.dominio.course.search.ActiveFilter
import es.ulpgc.pamn.dominio.course.search.PublishedWithin

// Todo lo que la pantalla Buscar puede avisar, junto en un objeto (patrón «objeto parámetro»),
// como LoginActions. Así ScreenSearch no recibe una docena de lambdas sueltas, y las previews usan SearchActions().
data class SearchActions(
    val onTextChange: (String) -> Unit = {},
    val onSearch: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onRetry: () -> Unit = {},
    // Recibe el id del curso tocado; quien navega es MainNavGraph, no la pantalla.
    val onCourseClick: (String) -> Unit = {},
    // Filtros (tarea 7). Los chips de la fila quitan el filtro al momento; la hoja trabaja con un
    // borrador y solo busca con `onApplyFilters` (decisión 7 del documento de la tarea).
    val onOpenFilters: () -> Unit = {},
    val onToggleTag: (String) -> Unit = {},
    val onResetFilters: () -> Unit = {},
    val onShowAllTags: () -> Unit = {},
    // Creador y fecha de la hoja (tarea 11d-1). Reciben el texto tal como se escribe y la opción
    // elegida; el ViewModel los conecta con el borrador en el 11d-2.
    val onAuthorChange: (String) -> Unit = {},
    val onPublishedChange: (PublishedWithin) -> Unit = {},
    val onApplyFilters: () -> Unit = {},
    val onCloseFilters: () -> Unit = {},
    val onRemoveFilter: (ActiveFilter) -> Unit = {},
)
