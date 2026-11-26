package es.ulpgc.pamn.dominio.ui.screens.enrollment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.ui.components.EnrollmentState
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// La página de relleno del curso: qué botones enseña según EnrollmentState y qué avisa al tocarlos.
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class ScreenCourseOverviewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var enrolls = 0
    private var paths = 0
    private var backs = 0

    private fun show(state: EnrollmentState) = composeRule.setContentInSpanish {
        GradusTheme {
            ScreenCourseOverview(
                enrollment = state,
                onEnroll = { enrolls++ },
                onGoToPath = { paths++ },
                onBack = { backs++ },
            )
        }
    }

    private fun enroll() = composeRule.onNodeWithText("Inscribirme")
    private fun path() = composeRule.onNodeWithText("Ir al camino")

    @Test
    fun without_enrollment_both_buttons_are_there_and_active() {
        show(EnrollmentState.NotEnrolled)

        enroll().assertIsDisplayed().assertIsEnabled()
        path().assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun enrolled_shows_a_disabled_enrolled_button_and_an_active_path_button() {
        show(EnrollmentState.Enrolled)

        composeRule.onNodeWithText("Inscrito").assertIsDisplayed().assertIsNotEnabled()
        path().assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun while_unknown_the_path_button_is_active_and_the_enroll_button_is_not() {
        show(EnrollmentState.Unknown)

        path().assertIsEnabled()
        enroll().assertIsNotEnabled()
    }

    @Test
    fun while_enrolling_the_path_button_is_active_and_the_enroll_button_is_not() {
        show(EnrollmentState.Enrolling)

        path().assertIsEnabled()
        composeRule.onNodeWithText("Inscribiendo…").assertIsNotEnabled()
    }

    @Test
    fun each_button_reports_with_its_own_lambda() {
        show(EnrollmentState.NotEnrolled)

        enroll().performClick()
        assertEquals(listOf(1, 0, 0), listOf(enrolls, paths, backs))

        path().performClick()
        assertEquals(listOf(1, 1, 0), listOf(enrolls, paths, backs))

        composeRule.onNodeWithContentDescription("Volver").performClick()
        assertEquals(listOf(1, 1, 1), listOf(enrolls, paths, backs))
    }

    @Test
    fun the_path_button_reports_in_every_state() {
        var state by mutableStateOf<EnrollmentState>(EnrollmentState.Unknown)
        composeRule.setContentInSpanish {
            GradusTheme {
                ScreenCourseOverview(
                    enrollment = state,
                    onEnroll = { enrolls++ },
                    onGoToPath = { paths++ },
                    onBack = { backs++ },
                )
            }
        }

        listOf(
            EnrollmentState.Unknown,
            EnrollmentState.NotEnrolled,
            EnrollmentState.Enrolling,
            EnrollmentState.Enrolled,
        ).forEachIndexed { index, next ->
            composeRule.runOnIdle { state = next }

            path().performClick()

            assertEquals("estado $next", index + 1, paths)
        }
    }

    @Test
    fun tapping_enroll_does_not_go_to_the_path() {
        show(EnrollmentState.NotEnrolled)

        enroll().performClick()

        assertEquals(0, paths)
    }

    @Test
    fun after_enrolling_the_path_button_stays_active_and_nothing_navigates_by_itself() {
        var state by mutableStateOf<EnrollmentState>(EnrollmentState.NotEnrolled)
        composeRule.setContentInSpanish {
            GradusTheme {
                ScreenCourseOverview(
                    enrollment = state,
                    onEnroll = { enrolls++ },
                    onGoToPath = { paths++ },
                    onBack = { backs++ },
                )
            }
        }

        composeRule.runOnIdle { state = EnrollmentState.Enrolled }

        path().assertIsEnabled()
        assertEquals(0, paths)
    }

    @Test
    fun the_generic_text_is_shown() {
        show(EnrollmentState.NotEnrolled)

        composeRule.onNodeWithText(
            "Inscríbete para guardar tu progreso y estudiar sus módulos. El camino se puede ver sin inscribirte.",
        ).assertIsDisplayed()
    }

    @Test
    fun the_buttons_and_the_back_button_are_at_least_48dp() {
        show(EnrollmentState.NotEnrolled)

        enroll().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        path().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("Volver").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun the_path_button_is_at_least_48dp_when_enrolled_too() {
        show(EnrollmentState.Enrolled)

        path().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun in_english_it_shows_the_translated_copy() {
        composeRule.setContentInEnglish {
            GradusTheme {
                ScreenCourseOverview(
                    enrollment = EnrollmentState.NotEnrolled,
                    onEnroll = {},
                    onGoToPath = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Course").assertIsDisplayed()
        composeRule.onNodeWithText("Enroll").assertIsDisplayed()
        composeRule.onNodeWithText("Go to path").assertIsDisplayed()
    }
}
