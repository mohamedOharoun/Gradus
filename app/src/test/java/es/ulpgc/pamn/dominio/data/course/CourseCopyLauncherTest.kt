package es.ulpgc.pamn.dominio.data.course

import es.ulpgc.pamn.dominio.course.CourseGateway
import es.ulpgc.pamn.dominio.course.CourseOutcome
import es.ulpgc.pamn.dominio.course.SearchOutcome
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.data.demoCourse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests JVM de CourseCopyLauncher (12b-2): lanza la precarga del curso en un ámbito de aplicación
// sin bloquear a quien la pide, descarta desenlaces de fallo de red/servidor sin reintentar,
// captura Exception del almacén (best effort) y siempre relanza CancellationException.
@OptIn(ExperimentalCoroutinesApi::class)
class CourseCopyLauncherTest {

    @Test
    fun `requestCopy calls gateway course with the given courseId`() = runTest {
        val gateway = RecordingCourseGateway { CourseOutcome.Found(demoCourse) }
        val launcher = CourseCopyLauncher(gateway, this)

        launcher.requestCopy("graphs")
        runCurrent()

        assertEquals(listOf("graphs"), gateway.requested)
    }

    @Test
    fun `requestCopy does not block the caller while download is in flight`() = runTest {
        val gate = CompletableDeferred<CourseOutcome>()
        val gateway = RecordingCourseGateway { gate.await() }
        val launcher = CourseCopyLauncher(gateway, this)

        launcher.requestCopy("graphs")
        runCurrent()

        assertEquals(listOf("graphs"), gateway.requested)
        assertEquals(0, gateway.completed.size)

        gate.complete(CourseOutcome.Found(demoCourse))
        runCurrent()

        assertEquals(listOf("graphs"), gateway.completed)
    }

    @Test
    fun `requestCopy downloads distinct courses independently`() = runTest {
        val gateway = RecordingCourseGateway { CourseOutcome.Found(demoCourse) }
        val launcher = CourseCopyLauncher(gateway, this)

        launcher.requestCopy("graphs")
        launcher.requestCopy("trees")
        runCurrent()

        assertEquals(listOf("graphs", "trees"), gateway.requested)
        assertEquals(listOf("graphs", "trees"), gateway.completed)
    }

    @Test
    fun `failure outcomes finish cleanly without retrying`() = runTest {
        val outcomes = listOf(
            CourseOutcome.Unreachable,
            CourseOutcome.ServerFailure,
            CourseOutcome.NotFound,
            CourseOutcome.UnsupportedFormat,
        )
        val answers = outcomes.iterator()
        val gateway = RecordingCourseGateway { answers.next() }
        val launcher = CourseCopyLauncher(gateway, this)

        outcomes.indices.forEach { launcher.requestCopy("course-$it") }
        runCurrent()

        assertEquals(listOf("course-0", "course-1", "course-2", "course-3"), gateway.requested)
        assertEquals(4, gateway.completed.size)
    }

    @Test
    fun `cancelling the launcher scope cancels an in-flight download`() = runTest {
        val gate = CompletableDeferred<CourseOutcome>()
        var sawCancellation = false
        val gateway = RecordingCourseGateway {
            try {
                gate.await()
            } catch (e: CancellationException) {
                sawCancellation = true
                throw e
            }
        }
        val launcherScope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        val launcher = CourseCopyLauncher(gateway, launcherScope)

        launcher.requestCopy("graphs")
        runCurrent()
        val childJob = launcherScope.coroutineContext.job.children.single()

        launcherScope.cancel()
        runCurrent()

        assertTrue(sawCancellation)
        assertTrue(childJob.isCancelled)
    }

    // Si gateway.course lanza CancellationException, no se traga en el catch(Exception):
    // la corrutina hija acaba cancelada y el SupervisorJob deja vivo el ámbito.
    @Test
    fun `CancellationException from gateway is rethrown and not swallowed`() = runTest {
        val gate = CompletableDeferred<CourseOutcome>()
        val gateway = RecordingCourseGateway { gate.await() }
        val launcherScope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        val launcher = CourseCopyLauncher(gateway, launcherScope)

        launcher.requestCopy("graphs")
        runCurrent()
        val childJob = launcherScope.coroutineContext.job.children.single()

        gate.cancel(CancellationException("cancelled"))
        runCurrent()

        assertTrue(childJob.isCancelled)
        assertTrue(launcherScope.isActive)
        launcherScope.cancel()
    }

    // Precarga oportunista (best effort): una Exception del almacén no llega al handler no capturado
    // ni inutiliza el ámbito para peticiones siguientes.
    @Test
    fun `a storage exception during download does not fail the scope or block subsequent requests`() = runTest {
        val uncaught = mutableListOf<Throwable>()
        val handler = CoroutineExceptionHandler { _, error -> uncaught += error }
        val gateway = RecordingCourseGateway { id ->
            if (id == "broken") throw IllegalStateException("disk full")
            CourseOutcome.Found(demoCourse)
        }
        val launcherScope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob() + handler)
        val launcher = CourseCopyLauncher(gateway, launcherScope)

        launcher.requestCopy("broken")
        runCurrent()

        assertTrue(uncaught.isEmpty())
        assertTrue(launcherScope.isActive)

        launcher.requestCopy("graphs")
        runCurrent()

        assertEquals(listOf("broken", "graphs"), gateway.requested)
        assertEquals(listOf("graphs"), gateway.completed)
        launcherScope.cancel()
    }
}

private class RecordingCourseGateway(
    private val answer: suspend (String) -> CourseOutcome,
) : CourseGateway {
    val requested = mutableListOf<String>()
    val completed = mutableListOf<String>()

    override suspend fun search(query: CourseQuery, page: Int): SearchOutcome =
        error("Not used in this test")

    override suspend fun course(id: String): CourseOutcome {
        requested += id
        val outcome = answer(id)
        completed += id
        return outcome
    }
}
