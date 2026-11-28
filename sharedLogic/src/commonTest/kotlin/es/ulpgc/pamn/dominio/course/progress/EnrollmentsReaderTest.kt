package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

// Todas las inscripciones del estudiante con lo dominado (docs/tareas/inicio.md, paso 2): lo que
// necesita la pantalla Inicio. El orden de la lista no está garantizado: los tests ordenan por curso.
class EnrollmentsReaderTest {

    private fun List<EnrollmentSnapshot>.byCourse() = sortedBy { it.enrollment.courseId }

    @Test
    fun `with no enrollments the list is empty`() = runTest {
        val store = ProgressFixture().loggedInAsAna().store
        assertTrue(store.enrollments().first().isEmpty())
    }

    // Cada inscripción con su fecha y sus módulos dominados, cada uno con la suya.
    @Test
    fun `every enrollment comes with its dates and mastered modules`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos", "arboles")
        fixture.clock.time = START + 2.hours
        fixture.store.markMastered("grafos", "bfs")

        assertEquals(
            listOf(
                EnrollmentSnapshot(Enrollment("arboles", START), emptyList()),
                EnrollmentSnapshot(Enrollment("grafos", START), listOf(MasteredModule("bfs", START + 2.hours))),
            ),
            fixture.store.enrollments().first().byCourse(),
        )
    }

    @Test
    fun `only the enrollments of the session user are read`() = runTest {
        val fixture = ProgressFixture()
        fixture.logInAs("bob")
        fixture.store.enroll("redes")
        fixture.logInAs("ana")
        fixture.store.enroll("grafos")

        assertEquals(listOf("grafos"), fixture.store.enrollments().first().map { it.enrollment.courseId })
    }

    // Inicio se basa en lo que hay en el servidor: lo que trae la sincronización también se lee.
    @Test
    fun `what the server sent is read too`() = runTest {
        val fixture = ProgressFixture().loggedInAsAna()
        val fromServer = EnrollmentSnapshot(
            Enrollment("kotlin", Instant.parse("2026-11-01T09:00:00Z")),
            listOf(MasteredModule("variables", Instant.parse("2026-11-02T09:00:00Z"))),
        )
        fixture.store.settle("ana", sent = ProgressSnapshot(emptyList()), remote = ProgressSnapshot(listOf(fromServer)))

        assertEquals(listOf(fromServer), fixture.store.enrollments().first())
    }

    // Inicio se actualiza al volver de estudiar sin preguntar otra vez.
    @Test
    fun `who listens gets each newly mastered module`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        val seen = mutableListOf<List<EnrollmentSnapshot>>()
        val listening = launch { fixture.store.enrollments().take(2).toList(seen) }
        testScheduler.runCurrent()
        fixture.store.markMastered("grafos", "bfs")
        listening.join()

        assertEquals(listOf(emptyList(), listOf("bfs")), seen.map { list -> list.single().mastered.map { it.moduleId } })
    }

    // Como los demás puertos del almacén: sin sesión, el valor vacío y se acaba.
    @Test
    fun `without a session the list is empty and the flow ends`() = runTest {
        val store = ProgressFixture().store
        assertEquals(listOf(emptyList()), store.enrollments().toList())
    }
}
