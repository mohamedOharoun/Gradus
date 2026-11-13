package es.ulpgc.pamn.dominio.ui.navigation

import kotlinx.serialization.Serializable

// Cada pantalla de la app es una ruta. Al ser "sealed", todas las rutas posibles están en este fichero.
sealed interface Route {

    @Serializable
    data object Welcome : Route

    @Serializable
    data object Register : Route

    @Serializable
    data object Login : Route

    @Serializable
    data object Home : Route
}
