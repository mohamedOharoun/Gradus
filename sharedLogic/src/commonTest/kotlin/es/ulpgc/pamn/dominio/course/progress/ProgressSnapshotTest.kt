package es.ulpgc.pamn.dominio.course.progress

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// isEmpty dice si hay algo que enviar (la sincronización no llama a la red si está vacío).
class ProgressSnapshotTest {

    @Test
    fun `a snapshot without enrollments is empty`() {
        assertTrue(snapshot().isEmpty)
    }

    @Test
    fun `a snapshot with an enrollment is not empty`() {
        assertFalse(snapshot(enrollment("grafos", START)).isEmpty)
    }
}
