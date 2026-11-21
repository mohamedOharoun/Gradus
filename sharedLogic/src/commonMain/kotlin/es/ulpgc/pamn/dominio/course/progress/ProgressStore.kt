package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.Flow

// Puerto (patrón Strategy): dónde guarda la app qué módulos ha dominado el estudiante en cada curso.
// Hoy, InMemoryProgressStore (se pierde al cerrar la app); más adelante, otra con Room (SQLite)
// que lo guarde en el móvil, sin cambiar a quien lo usa.
interface ProgressStore {

    // Los ids de los módulos dominados en el curso. Es un Flow: quien lo escucha (el camino)
    // recibe el valor actual y, después, cada cambio.
    fun mastered(courseId: String): Flow<Set<String>>

    // Apunta que el módulo está dominado. Si ya lo estaba, no cambia nada.
    suspend fun markMastered(courseId: String, moduleId: String)
}
