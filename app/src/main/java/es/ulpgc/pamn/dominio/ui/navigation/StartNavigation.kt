package es.ulpgc.pamn.dominio.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy

// Reglas de arranque de la navegación. Las puras (startRoute, mustLeaveMain) se prueban en la JVM; las
// que tocan Navigation (isInMain, leaveMain), en GradusNavHostStartTest.

// Con qué ruta empieza el NavHost. Con Loading todavía no se sabe: null (solo se pinta el fondo).
internal fun StartState.startRoute(): Route? = when (this) {
    StartState.Loading -> null
    StartState.SignedIn -> Route.Main
    StartState.SignedOut -> Route.Welcome
}

// La regla de «sin sesión en Main» se define sobre el ESTADO, no sobre el cambio: vale igual cuando la
// sesión se borra con la app abierta que cuando Android restaura la pila dentro de Main y la sesión ya
// no está. Con Loading no se sale: una Main restaurada no se abandona mientras aún se está leyendo.
internal fun StartState.mustLeaveMain(inMain: Boolean): Boolean = this is StartState.SignedOut && inMain

// ¿Está este destino dentro del grafo Main? Main es un grafo anidado: se mira el destino y sus padres.
internal fun NavDestination?.isInMain(): Boolean = this?.hierarchy?.any { it.hasRoute<Route.Main>() } == true

// Sale de Main hacia el inicio de sesión sin poder volver: vacía la pila y navega a Bienvenida y
// después a Login. Queda [Welcome, Login], como en el flujo normal: «atrás» lleva a Bienvenida.
internal fun NavController.leaveMain() {
    navigate(Route.Welcome) { popUpTo(graph.id) { inclusive = true } }
    navigate(Route.Login)
}
