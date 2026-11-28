package es.ulpgc.pamn.dominio.ui.screens.course

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseNoticeMessagesTest {

    @Test
    fun `the not enrolled notice asks to enroll`() {
        assertEquals(UiText.of(R.string.course_not_enrolled), CourseNotice.NotEnrolled.toMessage())
    }
}
