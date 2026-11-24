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

    // ── Dentro del grafo Main, pero sin barra (docs/tareas/camino-curso-y-ejercicios.md) ──

    // Editar el nombre, desde Ajustes (docs/tareas/ajustes.md, fase 2).
    @Serializable
    data object EditProfile : Route

    // Cambiar la contraseña, desde Ajustes (docs/tareas/ajustes.md, fase 2).
    @Serializable
    data object ChangePassword : Route

    // Elegir las etiquetas preferidas, desde Ajustes (docs/tareas/ajustes.md, fase 3).
    @Serializable
    data object PreferredTags : Route

    // La ficha de un curso (docs/tareas/ficha-curso.md): de qué va, antes de entrar en su camino.
    @Serializable
    data class CourseDetail(val courseId: String) : Route

    // El camino de un curso. data class (no object) porque lleva un dato: qué curso abrir.
    @Serializable
    data class Course(val courseId: String) : Route

    // Las notas con cámara de un módulo (docs/tareas/notas-camara.md).
    @Serializable
    data class ModuleNotes(val courseId: String, val moduleId: String) : Route

    // Hacer una foto para las notas de un módulo (docs/tareas/notas-camara.md, tarea 5).
    @Serializable
    data class TakeNote(val courseId: String, val moduleId: String) : Route

    // Ver una nota con cámara (docs/tareas/notas-camara.md, tarea 6).
    @Serializable
    data class NoteViewer(val noteId: String) : Route

    // La práctica de un módulo de un curso.
    @Serializable
    data class Exercise(val courseId: String, val moduleId: String) : Route
}
