package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.Flow

// Puerto (patrón Strategy): dónde guarda la app qué módulos ha dominado el estudiante en cada curso.
// Hoy, InMemoryProgressStore (se pierde al cerrar la app); más adelante, otra con Room (SQLite)
// que lo guarde en el móvil, sin cambiar a quien lo usa.
interface ProgressStore {

    // Los ids de los módulos dominados en el curso por el estudiante de la sesión actual. Es un Flow:
    // quien lo escucha (el camino) recibe el valor actual y, después, cada cambio. La sesión se lee
    // al empezar a escuchar; sin sesión emite un conjunto vacío y termina.
    fun mastered(courseId: String): Flow<Set<String>>

    // Apunta que el módulo está dominado (con la fecha de ahora) para el estudiante de la sesión actual.
    // - Si ya lo estaba, no cambia nada y se conserva la primera fecha.
    // - Si el estudiante no está inscrito en el curso, lanza IllegalStateException y no deja nada
    //   escrito: no existe un módulo dominado sin inscripción.
    // - Sin sesión, también lanza IllegalStateException.
    // Ambos son fallos de programación (la interfaz debe comprobar studyAccess antes), por eso no
    // se capturan. Cualquier otra implementación (p. ej. Room) tiene que cumplirlo igual.
    suspend fun markMastered(courseId: String, moduleId: String)
}
