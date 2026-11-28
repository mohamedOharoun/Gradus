package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

// Baja local en el almacén de progreso: transformación pura sin estado mutable en UserProgress
// y operación atómica, idempotente y por userId explícito en InMemoryProgressStore.
class UnenrollStoreTest {

    private suspend fun anaWithGraphs(): ProgressFixture =
        ProgressFixture().enrolledIn("grafos").also { it.store.markMastered("grafos", "bfs") }

    private fun <T> TestScope.record(flow: Flow<T>): Pair<List<T>, Job> {
        val seen = mutableListOf<T>()
        val job = launch { flow.toList(seen) }
        testScheduler.runCurrent()
        return seen to job
    }

    @Test
    fun `withoutCourse removes the course without mutating the original progress`() {
        val before = UserProgress()
            .enrolledIn("grafos", START)
            .masteredIn("grafos", "bfs", START + 1.minutes)
            .enrolledIn("arboles", START)
            .masteredIn("arboles", "raiz", START + 2.minutes)

        val after = before.withoutCourse("grafos")

        assertFalse(after.isEnrolledIn("grafos"))
        assertTrue(after.masteredIds("grafos").isEmpty())
        assertEquals(before.courses.getValue("arboles"), after.courses.getValue("arboles"))
        assertTrue(before.isEnrolledIn("grafos"))
        assertEquals(setOf("bfs"), before.masteredIds("grafos"))
    }

    @Test
    fun `withoutCourse returns the same instance when the course is absent`() {
        val before = UserProgress().enrolledIn("grafos", START)
        assertSame(before, before.withoutCourse("redes"))
    }

    @Test
    fun `unenrolling removes enrollment and all mastered modules`() = runTest {
        val fixture = anaWithGraphs()
        fixture.uploadAll()
        fixture.store.markMastered("grafos", "dfs")

        fixture.store.unenroll("ana", "grafos")

        assertFalse(fixture.store.enrolled("grafos").first())
        assertTrue(fixture.store.mastered("grafos").first().isEmpty())
        assertEquals(UserProgress(), fixture.store.progressOf("ana"))
        assertTrue(fixture.store.pending("ana").isEmpty)
    }

    @Test
    fun `unenrolling removes the course and its mastered modules from pending`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos", "arboles")
        fixture.store.markMastered("grafos", "bfs")
        fixture.store.markMastered("arboles", "raiz")

        fixture.store.unenroll("ana", "grafos")

        assertSameSnapshot(
            snapshot(enrollment("arboles", START, "raiz" to START)),
            fixture.store.pending("ana"),
        )
    }

    @Test
    fun `unenrolling when not enrolled does not fail or change state`() = runTest {
        val fixture = ProgressFixture().enrolledIn("arboles")
        val before = fixture.store.progressOf("ana")

        fixture.store.unenroll("desconocido", "grafos")
        fixture.store.unenroll("ana", "grafos")

        assertEquals(before, fixture.store.progressOf("ana"))
        assertEquals(UserProgress(), fixture.store.progressOf("desconocido"))
    }

    @Test
    fun `unenrolling twice is idempotent`() = runTest {
        val fixture = anaWithGraphs()

        fixture.store.unenroll("ana", "grafos")
        fixture.store.unenroll("ana", "grafos")

        assertFalse(fixture.store.enrolled("grafos").first())
        assertTrue(fixture.store.mastered("grafos").first().isEmpty())
        assertEquals(UserProgress(), fixture.store.progressOf("ana"))
    }

    @Test
    fun `unenrolling does not affect other courses of the same user`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos", "arboles")
        fixture.store.markMastered("grafos", "bfs")
        fixture.store.markMastered("arboles", "raiz")
        val expectedTrees = fixture.store.progressOf("ana").courses.getValue("arboles")

        fixture.store.unenroll("ana", "grafos")

        assertEquals(expectedTrees, fixture.store.progressOf("ana").courses.getValue("arboles"))
        assertTrue(fixture.store.enrolled("arboles").first())
        assertEquals(setOf("raiz"), fixture.store.mastered("arboles").first())
    }

    @Test
    fun `unenrolling does not affect the same course of another user`() = runTest {
        val fixture = anaWithGraphs()
        fixture.logInAs("beto")
        fixture.store.enroll("grafos")
        fixture.store.markMastered("grafos", "dfs")
        val expectedBeto = fixture.store.progressOf("beto")

        fixture.store.unenroll("ana", "grafos")

        assertEquals(expectedBeto, fixture.store.progressOf("beto"))
        assertSameSnapshot(
            snapshot(enrollment("grafos", START, "dfs" to START)),
            fixture.store.pending("beto"),
        )
    }

    @Test
    fun `unenrolling works with no active session`() = runTest {
        val fixture = anaWithGraphs()
        fixture.sessions.clear()

        fixture.store.unenroll("ana", "grafos")

        assertEquals(UserProgress(), fixture.store.progressOf("ana"))
        assertTrue(fixture.store.pending("ana").isEmpty)
    }

    @Test
    fun `unenrolling acts on the given userId even when another user is logged in`() = runTest {
        val fixture = anaWithGraphs()
        fixture.logInAs("beto")
        fixture.store.enroll("grafos")
        fixture.store.markMastered("grafos", "dfs")

        fixture.store.unenroll("ana", "grafos")

        assertEquals(UserProgress(), fixture.store.progressOf("ana"))
        assertTrue(fixture.store.enrolled("grafos").first())
        assertEquals(setOf("dfs"), fixture.store.mastered("grafos").first())
    }

    @Test
    fun `active enrolled and mastered flows emit false and emptySet on unenroll`() = runTest {
        val fixture = anaWithGraphs()
        val seenEnrolled = mutableListOf<Boolean>()
        val seenMastered = mutableListOf<Set<String>>()
        val enrolledJob = launch { fixture.store.enrolled("grafos").take(2).toList(seenEnrolled) }
        val masteredJob = launch { fixture.store.mastered("grafos").take(2).toList(seenMastered) }
        testScheduler.runCurrent()

        fixture.store.unenroll("ana", "grafos")
        enrolledJob.join()
        masteredJob.join()

        assertEquals(listOf(true, false), seenEnrolled)
        assertEquals(listOf(setOf("bfs"), emptySet()), seenMastered)
    }

    @Test
    fun `unenrolling does not notify listeners of another course or another user`() = runTest {
        val fixture = ProgressFixture().enrolledIn("grafos", "arboles")
        fixture.store.markMastered("arboles", "raiz")
        val (anaTreesEnrolled, anaEnrolledJob) = record(fixture.store.enrolled("arboles"))
        val (anaTreesMastered, anaMasteredJob) = record(fixture.store.mastered("arboles"))

        fixture.logInAs("beto")
        fixture.store.enroll("grafos")
        fixture.store.markMastered("grafos", "dfs")
        val (betoGraphsEnrolled, betoEnrolledJob) = record(fixture.store.enrolled("grafos"))
        val (betoGraphsMastered, betoMasteredJob) = record(fixture.store.mastered("grafos"))

        fixture.store.unenroll("ana", "grafos")
        testScheduler.runCurrent()
        listOf(anaEnrolledJob, anaMasteredJob, betoEnrolledJob, betoMasteredJob).forEach { it.cancel() }

        assertEquals(listOf(true), anaTreesEnrolled)
        assertEquals(listOf(setOf("raiz")), anaTreesMastered)
        assertEquals(listOf(true), betoGraphsEnrolled)
        assertEquals(listOf(setOf("dfs")), betoGraphsMastered)
    }

    @Test
    fun `unenrolling an absent course does not emit to active listeners`() = runTest {
        val fixture = ProgressFixture().loggedInAsAna()
        val (seenEnrolled, enrolledJob) = record(fixture.store.enrolled("grafos"))
        val (seenMastered, masteredJob) = record(fixture.store.mastered("grafos"))

        fixture.store.unenroll("ana", "grafos")
        testScheduler.runCurrent()
        enrolledJob.cancel()
        masteredJob.cancel()

        assertEquals(listOf(false), seenEnrolled)
        assertEquals(listOf(emptySet()), seenMastered)
    }

    @Test
    fun `re-enrolling after unenroll starts clean with new date pending enrollment and no mastered modules`() = runTest {
        val fixture = anaWithGraphs()
        fixture.uploadAll()
        fixture.store.unenroll("ana", "grafos")

        val later = START + 10.minutes
        fixture.clock.time = later
        fixture.store.enroll("grafos")

        assertTrue(fixture.store.enrolled("grafos").first())
        assertTrue(fixture.store.mastered("grafos").first().isEmpty())
        assertEquals(
            CourseProgress(Enrollment("grafos", later), enrollmentPending = true),
            fixture.store.progressOf("ana").courses.getValue("grafos"),
        )
        assertSameSnapshot(snapshot(enrollment("grafos", later)), fixture.store.pending("ana"))
        assertPendingWithinMastered(fixture.store.progressOf("ana"))
    }

    @Test
    fun `mastering after unenroll fails with IllegalStateException and leaves nothing written`() = runTest {
        val fixture = anaWithGraphs()
        fixture.store.unenroll("ana", "grafos")

        assertFailsWith<IllegalStateException> { fixture.store.markMastered("grafos", "dfs") }

        assertEquals(UserProgress(), fixture.store.progressOf("ana"))
        assertFalse(fixture.store.enrolled("grafos").first())
        assertTrue(fixture.store.mastered("grafos").first().isEmpty())
        assertTrue(fixture.store.pending("ana").isEmpty)
    }
}
