package es.ulpgc.pamn.dominio.course.progress

import kotlin.test.Test
import kotlin.test.assertEquals

// Si el estudiante puede estudiar un curso: depende solo de la inscripción.
class StudyAccessTest {

    @Test
    fun `not enrolled has no access`() {
        assertEquals(StudyAccess.NotEnrolled, studyAccess(enrolled = false))
    }

    @Test
    fun `enrolled can study`() {
        assertEquals(StudyAccess.Allowed, studyAccess(enrolled = true))
    }

    @Test
    fun `every access can be handled without else`() {
        // Si se añade una variante a StudyAccess, este when deja de compilar.
        fun label(access: StudyAccess): String = when (access) {
            StudyAccess.Allowed -> "allowed"
            StudyAccess.NotEnrolled -> "not enrolled"
        }
        assertEquals("allowed", label(studyAccess(true)))
        assertEquals("not enrolled", label(studyAccess(false)))
    }
}
