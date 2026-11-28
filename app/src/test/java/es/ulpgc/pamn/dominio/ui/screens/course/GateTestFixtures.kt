package es.ulpgc.pamn.dominio.ui.screens.course

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.progress.EnrollmentStore
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.course.progress.ProgressStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

// Ayudantes de los tests de la puerta (9b): un curso en cadena A → B → C y almacenes de prueba.

internal val nodes = Module(id = "a", title = "Nodos")
internal val edges = Module(id = "b", title = "Aristas", requires = listOf("a"))
internal val paths = Module(id = "c", title = "Caminos", requires = listOf("b"))

internal val gateCourse = Course(id = "grafos", title = "Grafos", modules = listOf(nodes, edges, paths))

// El almacén real en memoria, con una sesión ya guardada (sin inscripción ni progreso).
internal suspend fun loggedInStore(): InMemoryProgressStore {
    val sessions = InMemorySessionStore()
    sessions.save(
        Session(
            token = "token-de-prueba",
            expiresAt = "2099-01-01T00:00:00Z",
            account = Account(id = "ana", name = "Ana", email = "ana@example.com", createdAt = "2026-01-01T00:00:00Z"),
        ),
    )
    return InMemoryProgressStore(sessions)
}

// Lo dominado, fijado desde fuera. Solo para el caso que el almacén real no permite: un módulo
// dominado sin inscripción (markMastered lanza sin inscripción).
internal class FixedMasteredStore(mastered: Set<String>) : ProgressStore {
    private val flow = MutableStateFlow(mastered)
    override fun mastered(courseId: String): Flow<Set<String>> = flow
    override suspend fun markMastered(courseId: String, moduleId: String) = error("Not used in these tests")
}

// Una inscripción que todavía no ha respondido: su Flow no emite nunca.
internal class SilentEnrollmentStore : EnrollmentStore {
    override fun enrolled(courseId: String): Flow<Boolean> = MutableSharedFlow()
    override suspend fun enroll(courseId: String) = error("Not used in these tests")
    override suspend fun unenroll(userId: String, courseId: String) = error("Not used in these tests")
}
