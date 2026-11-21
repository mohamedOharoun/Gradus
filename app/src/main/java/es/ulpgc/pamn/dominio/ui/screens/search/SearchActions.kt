package es.ulpgc.pamn.dominio.ui.screens.search

// Todo lo que la pantalla Buscar puede avisar, junto en un objeto (patrón «objeto parámetro»),
// como LoginActions. Así ScreenSearch no recibe cinco lambdas sueltas, y las previews usan SearchActions().
data class SearchActions(
    val onTextChange: (String) -> Unit = {},
    val onSearch: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onRetry: () -> Unit = {},
    // Recibe el id del curso tocado; quien navega es MainNavGraph, no la pantalla.
    val onCourseClick: (String) -> Unit = {},
)
