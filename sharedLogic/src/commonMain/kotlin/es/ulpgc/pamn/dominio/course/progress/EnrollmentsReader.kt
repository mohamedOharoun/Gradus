package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.Flow

// Puerto (patrón Strategy) de solo lectura: todas las inscripciones del estudiante de la sesión, con lo
// dominado (docs/tareas/inicio.md, paso 2). Lo usa la pantalla Inicio. Va aparte de EnrollmentStore
// (segregación de interfaces): quien solo pregunta por un curso no tiene que saber leerlos todos.
// Lo implementan InMemoryProgressStore y RoomProgressStore.
interface EnrollmentsReader {
    // Es un Flow: quien lo escucha recibe la lista actual y, después, cada cambio (inscribirse, dominar,
    // lo que traiga la sincronización). La sesión se lee al empezar a escuchar; sin sesión emite una
    // lista vacía y termina. El orden de la lista no está garantizado.
    fun enrollments(): Flow<List<EnrollmentSnapshot>>
}

// Todo el progreso de un usuario con la forma del contrato, pendiente o no (pending() solo da lo que
// falta por subir).
internal fun UserProgress.snapshot(): List<EnrollmentSnapshot> =
    courses.values.map { EnrollmentSnapshot(it.enrollment, it.mastered.values.toList()) }
