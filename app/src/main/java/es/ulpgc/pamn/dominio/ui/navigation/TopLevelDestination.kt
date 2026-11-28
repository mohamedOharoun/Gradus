package es.ulpgc.pamn.dominio.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import es.ulpgc.pamn.dominio.R

// Las pestañas de la barra de navegación, en el orden en que se ven (Figma «Barra de navegación»).
// La barra se pinta recorriendo entries: añadir una pestaña es añadir una línea aquí.
enum class TopLevelDestination(
    val route: Route,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
    HOME(Route.Home, R.string.tab_home, R.drawable.ic_home),
    SEARCH(Route.Search, R.string.tab_search, R.drawable.ic_search),
    MY_COURSES(Route.MyCourses, R.string.tab_my_courses, R.drawable.ic_book),
    SETTINGS(Route.Settings, R.string.tab_settings, R.drawable.ic_tune),
}
