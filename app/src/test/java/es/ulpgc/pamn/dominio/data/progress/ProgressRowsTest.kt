package es.ulpgc.pamn.dominio.data.progress

import es.ulpgc.pamn.dominio.course.progress.Enrollment
import es.ulpgc.pamn.dominio.course.progress.EnrollmentSnapshot
import es.ulpgc.pamn.dominio.course.progress.MasteredModule
import es.ulpgc.pamn.dominio.course.progress.ProgressSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

// La traducción entre las filas de Room y ProgressSnapshot (la forma del contrato).
class ProgressRowsTest {

    private val start = Instant.parse("2026-11-25T10:00:00Z")
    private val later = Instant.parse("2026-11-25T10:01:00.123456789Z")

    @Test
    fun to_rows_puts_the_user_and_leaves_nothing_pending() {
        val snapshot = ProgressSnapshot(
            listOf(
                EnrollmentSnapshot(Enrollment("graphs", start), listOf(MasteredModule("bfs", later))),
                EnrollmentSnapshot(Enrollment("trees", later), emptyList()),
            ),
        )

        val rows = snapshot.toRows("ana")

        assertEquals(
            listOf(
                EnrollmentEntity("ana", "graphs", start, pending = false),
                EnrollmentEntity("ana", "trees", later, pending = false),
            ),
            rows.enrollments,
        )
        assertEquals(listOf(MasteredModuleEntity("ana", "graphs", "bfs", later, pending = false)), rows.modules)
    }

    @Test
    fun an_empty_snapshot_gives_no_rows() {
        val rows = ProgressSnapshot(emptyList()).toRows("ana")

        assertTrue(rows.enrollments.isEmpty())
        assertTrue(rows.modules.isEmpty())
    }

    @Test
    fun to_snapshot_puts_each_module_inside_its_enrollment() {
        val rows = ProgressRows(
            enrollments = listOf(
                EnrollmentEntity("ana", "graphs", start, pending = false),
                EnrollmentEntity("ana", "trees", start, pending = true),
            ),
            modules = listOf(
                MasteredModuleEntity("ana", "trees", "avl", later, pending = true),
                MasteredModuleEntity("ana", "graphs", "bfs", start, pending = true),
                MasteredModuleEntity("ana", "graphs", "dfs", later, pending = true),
            ),
        )

        assertEquals(
            ProgressSnapshot(
                listOf(
                    EnrollmentSnapshot(
                        Enrollment("graphs", start),
                        listOf(MasteredModule("bfs", start), MasteredModule("dfs", later)),
                    ),
                    EnrollmentSnapshot(Enrollment("trees", start), listOf(MasteredModule("avl", later))),
                ),
            ),
            rows.toSnapshot(),
        )
    }

    @Test
    fun an_enrollment_without_modules_has_an_empty_list() {
        val rows = ProgressRows(listOf(EnrollmentEntity("ana", "graphs", start, pending = true)), emptyList())

        assertEquals(
            ProgressSnapshot(listOf(EnrollmentSnapshot(Enrollment("graphs", start), emptyList()))),
            rows.toSnapshot(),
        )
    }

    // El contrato no admite módulos sueltos. En pending no puede pasar (las dos lecturas van en una
    // transacción y la consulta de inscripciones incluye las que tienen un módulo pendiente).
    @Test
    fun a_module_without_its_enrollment_is_left_out() {
        val rows = ProgressRows(
            enrollments = listOf(EnrollmentEntity("ana", "graphs", start, pending = true)),
            modules = listOf(MasteredModuleEntity("ana", "trees", "avl", later, pending = true)),
        )

        assertEquals(
            ProgressSnapshot(listOf(EnrollmentSnapshot(Enrollment("graphs", start), emptyList()))),
            rows.toSnapshot(),
        )
    }

    @Test
    fun no_rows_give_an_empty_snapshot() {
        assertTrue(ProgressRows(emptyList(), emptyList()).toSnapshot().isEmpty)
    }

    // El usuario y el curso de cada fila, a los dos lados: un módulo de otro usuario con el mismo curso
    // no se mezcla (en pending no pasa: las dos lecturas filtran por usuario).
    @Test
    fun a_module_of_another_user_is_left_out() {
        val rows = ProgressRows(
            enrollments = listOf(EnrollmentEntity("ana", "graphs", start, pending = true)),
            modules = listOf(MasteredModuleEntity("beto", "graphs", "bfs", later, pending = true)),
        )

        assertEquals(
            ProgressSnapshot(listOf(EnrollmentSnapshot(Enrollment("graphs", start), emptyList()))),
            rows.toSnapshot(),
        )
    }
}
