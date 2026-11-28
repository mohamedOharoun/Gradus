package es.ulpgc.pamn.dominio.course.progress

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

// El acceso al estudio sale del almacén local: no hace falta red (aquí ni existe un gateway).
class OfflineStudyAccessTest {

    @Test
    fun `an enrolled student can study without any network`() = runTest {
        val store = ProgressFixture().enrolledIn("grafos").store
        assertEquals(StudyAccess.Allowed, studyAccess(store.enrolled("grafos").first()))
    }

    @Test
    fun `a student who did not enroll cannot study`() = runTest {
        val store = ProgressFixture().enrolledIn("arboles").store
        assertEquals(StudyAccess.NotEnrolled, studyAccess(store.enrolled("grafos").first()))
    }

    @Test
    fun `without session the student cannot study`() = runTest {
        val store = ProgressFixture().store
        assertEquals(StudyAccess.NotEnrolled, studyAccess(store.enrolled("grafos").first()))
    }
}
