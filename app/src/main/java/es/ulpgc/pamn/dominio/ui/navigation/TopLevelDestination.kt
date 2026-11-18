package es.ulpgc.pamn.dominio.ui.navigation

import androidx.annotation.DrawableRes
import es.ulpgc.pamn.dominio.R

// Las pestañas de la barra de navegación, en el orden en que se ven (Figma «Barra de navegación»).
// La barra se pinta recorriendo entries: añadir una pestaña es añadir una línea aquí.
enum class TopLevelDestination(
    val route: Route,
    val label: String,
    @DrawableRes val icon: Int,
) {
    HOME(Route.Home, "Inicio", R.drawable.ic_home),
    SEARCH(Route.Search, "Buscar", R.drawable.ic_search),
    MY_COURSES(Route.MyCourses, "Mis cursos", R.drawable.ic_book),
    SETTINGS(Route.Settings, "Ajustes", R.drawable.ic_tune),
}
