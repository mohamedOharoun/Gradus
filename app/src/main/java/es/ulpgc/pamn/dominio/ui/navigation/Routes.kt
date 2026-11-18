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

    // ── Con sesión: grafo anidado con la barra de navegación (docs/tareas/barra-navegacion.md) ──

    // El grafo entero. Se navega a Main y empieza en Home.
    @Serializable
    data object Main : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Search : Route

    @Serializable
    data object MyCourses : Route

    @Serializable
    data object Settings : Route
}
