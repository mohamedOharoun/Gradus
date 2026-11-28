package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

// pending(userId): lo que falta por subir, con la forma del contrato (lo dominado dentro de su inscripción).
class PendingProgressTest {

    @Test
    fun `nothing is pending at the start`() = runTest {
        val fixture = ProgressFixture().loggedInAsAna()
        assertTrue(fixture.store.pending("ana").isEmpty)
    }

    @Test
    fun `an unknown user has nothing pending`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        assertTrue(fixture.store.pending("bea").isEmpty)
    }

    @Test
    fun `an enrollment is pending with its date`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        assertSameSnapshot(snapshot(enrollment("grafos", START)), fixture.store.pending("ana"))
    }

    @Test
    fun `a mastered module is pending inside its enrollment with its date`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.clock.time = START + 1.minutes
        fixture.store.markMastered("grafos", "bfs")
        assertSameSnapshot(
            snapshot(enrollment("grafos", START, "bfs" to START + 1.minutes)),
            fixture.store.pending("ana"),
        )
        assertPendingWithinMastered(fixture.store.progressOf("ana"))
    }

    @Test
    fun `a pending module goes inside its already uploaded enrollment with only the pending modules`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        fixture.uploadAll()
        fixture.clock.time = START + 2.minutes
        fixture.store.markMastered("grafos", "dfs")
        assertSameSnapshot(
            snapshot(enrollment("grafos", START, "dfs" to START + 2.minutes)),
            fixture.store.pending("ana"),
        )
        assertPendingWithinMastered(fixture.store.progressOf("ana"))
    }

    @Test
    fun `pending is per user`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.logInAs("bea")
        fixture.store.enroll("redes")
        assertSameSnapshot(snapshot(enrollment("grafos", START)), fixture.store.pending("ana"))
        assertSameSnapshot(snapshot(enrollment("redes", START)), fixture.store.pending("bea"))
    }
}
