package es.ulpgc.pamn.dominio.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds

// pending(userId) con Room: lo que falta por subir, con lo dominado dentro de su inscripción.
@RunWith(AndroidJUnit4::class)
class RoomPendingTest {

    @get:Rule
    val progress = RoomProgressRule()

    private val store get() = progress.store

    @Test
    fun nothing_is_pending_at_the_start() = runTest {
        progress.logInAs(ANA)

        assertTrue(store.pending(ANA).isEmpty)
    }

    @Test
    fun an_unknown_user_has_nothing_pending() = runTest {
        progress.enrolledAsAna(GRAPHS)

        assertTrue(store.pending(BETO).isEmpty)
    }

    @Test
    fun an_enrollment_is_pending_with_its_exact_date() = runTest {
        progress.clock.time = START + 123_456_789.nanoseconds
        progress.enrolledAsAna(GRAPHS)

        assertSameSnapshot(snapshot(enrollment(GRAPHS, START + 123_456_789.nanoseconds)), store.pending(ANA))
    }

    @Test
    fun a_mastered_module_is_pending_inside_its_enrollment_with_its_date() = runTest {
        progress.enrolledAsAna(GRAPHS)
        progress.clock.time = START + 1.minutes
        store.markMastered(GRAPHS, "bfs")

        assertSameSnapshot(snapshot(enrollment(GRAPHS, START, "bfs" to START + 1.minutes)), store.pending(ANA))
    }

    @Test
    fun a_pending_module_goes_inside_its_already_uploaded_enrollment_with_only_the_pending_modules() = runTest {
        progress.enrolledAsAna(GRAPHS, TREES)
        store.markMastered(GRAPHS, "bfs")
        progress.uploadAll()
        progress.clock.time = START + 2.minutes
        store.markMastered(GRAPHS, "dfs")

        // Ni «bfs» (ya subido) ni TREES (ya subido y sin nada pendiente).
        assertSameSnapshot(snapshot(enrollment(GRAPHS, START, "dfs" to START + 2.minutes)), store.pending(ANA))
    }

    @Test
    fun pending_is_per_user_even_in_the_same_course() = runTest {
        progress.enrolledAsAna(GRAPHS)
        store.markMastered(GRAPHS, "bfs")
        progress.logInAs(BETO)
        progress.clock.time = START + 1.minutes
        store.enroll(GRAPHS)

        assertSameSnapshot(snapshot(enrollment(GRAPHS, START, "bfs" to START)), store.pending(ANA))
        assertSameSnapshot(snapshot(enrollment(GRAPHS, START + 1.minutes)), store.pending(BETO))
    }

    // Decisión 11: el usuario va por parámetro; pending no lee la sesión.
    @Test
    fun pending_reads_the_given_user_not_the_session() = runTest {
        progress.enrolledAsAna(GRAPHS)
        progress.logInAs(BETO)

        assertSameSnapshot(snapshot(enrollment(GRAPHS, START)), store.pending(ANA))
    }
}
