package es.ulpgc.pamn.dominio.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import es.ulpgc.pamn.dominio.ui.components.MainScaffold
import es.ulpgc.pamn.dominio.ui.screens.ScreenHome
import es.ulpgc.pamn.dominio.ui.screens.ScreenMyCourses
import es.ulpgc.pamn.dominio.ui.screens.ScreenSearch
import es.ulpgc.pamn.dominio.ui.screens.ScreenSettings

// Grafo anidado de las pantallas con sesión (docs/tareas/barra-navegacion.md).
// Va en su propio fichero para que GradusNavHost solo tenga que llamar a mainGraph(...).
// Sin animación entre pestañas: cada pestaña pinta su propia barra y, con el fundido por defecto,
// la barra parpadearía al cruzarse con la de la pestaña anterior.
fun NavGraphBuilder.mainGraph(navController: NavController) {
    navigation<Route.Main>(
        startDestination = Route.Home,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        tab<Route.Home>(TopLevelDestination.HOME, navController) { ScreenHome(it) }
        tab<Route.Search>(TopLevelDestination.SEARCH, navController) { ScreenSearch(it) }
        tab<Route.MyCourses>(TopLevelDestination.MY_COURSES, navController) { ScreenMyCourses(it) }
        tab<Route.Settings>(TopLevelDestination.SETTINGS, navController) { ScreenSettings(it) }
    }
}

// Una pestaña: su pantalla dentro del armazón con la barra, marcando esa pestaña.
private inline fun <reified T : Any> NavGraphBuilder.tab(
    destination: TopLevelDestination,
    navController: NavController,
    crossinline screen: @Composable (Modifier) -> Unit,
) {
    composable<T> {
        MainScaffold(selected = destination, onSelect = navController::navigateToTab) { screen(it) }
    }
}

// Cambio de pestaña con el patrón recomendado por Android para la navegación inferior:
// - popUpTo(Home) + saveState: debajo solo queda Inicio, y se guarda el estado de lo que se quita.
// - launchSingleTop: pulsar la pestaña en la que ya estás no apila otra copia.
// - restoreState: al volver a una pestaña, recupera cómo estaba.
// Así «atrás» desde cualquier pestaña lleva a Inicio y, desde Inicio, cierra la app.
private fun NavController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo<Route.Home> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
