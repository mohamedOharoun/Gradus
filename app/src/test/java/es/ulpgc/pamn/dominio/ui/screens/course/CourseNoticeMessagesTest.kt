package es.ulpgc.pamn.dominio.ui.screens.course

import org.junit.Assert.assertEquals
import org.junit.Test

class CourseNoticeMessagesTest {

    @Test
    fun `the not enrolled notice asks to enroll`() {
        assertEquals("Inscríbete para empezar a estudiar", CourseNotice.NotEnrolled.toMessage())
    }
}
