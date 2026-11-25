package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

// Las fechas salen del Clock inyectado, no del reloj del sistema, y la primera se conserva.
class ProgressDatesTest {

    private fun ProgressFixture.course(courseId: String) = store.progressOf("ana").courses.getValue(courseId)

    @Test
    fun `the enrollment date comes from the clock`() = runTest {
        val fixture = ProgressFixture()
        fixture.clock.time = START + 5.minutes
        fixture.enrolledIn("grafos")
        assertEquals(START + 5.minutes, fixture.course("grafos").enrollment.enrolledAt)
    }

    @Test
    fun `the mastered date comes from the clock`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.clock.time = START + 10.minutes
        fixture.store.markMastered("grafos", "bfs")
        assertEquals(START + 10.minutes, fixture.course("grafos").mastered.getValue("bfs").masteredAt)
    }

    @Test
    fun `each mastered module has its own date`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.clock.time = START + 1.minutes
        fixture.store.markMastered("grafos", "bfs")
        fixture.clock.time = START + 2.minutes
        fixture.store.markMastered("grafos", "dfs")
        val mastered = fixture.course("grafos").mastered
        assertEquals(START + 1.minutes, mastered.getValue("bfs").masteredAt)
        assertEquals(START + 2.minutes, mastered.getValue("dfs").masteredAt)
    }

    @Test
    fun `enrolling twice keeps the first date`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.clock.time = START + 30.minutes
        fixture.store.enroll("grafos")
        assertEquals(START, fixture.course("grafos").enrollment.enrolledAt)
    }

    @Test
    fun `mastering twice keeps the first date`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.clock.time = START + 1.minutes
        fixture.store.markMastered("grafos", "bfs")
        fixture.clock.time = START + 30.minutes
        fixture.store.markMastered("grafos", "bfs")
        assertEquals(START + 1.minutes, fixture.course("grafos").mastered.getValue("bfs").masteredAt)
    }

    @Test
    fun `enrolling twice keeps what was already mastered`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos")
        fixture.store.markMastered("grafos", "bfs")
        fixture.store.enroll("grafos")
        assertEquals(setOf("bfs"), fixture.course("grafos").mastered.keys)
    }
}
