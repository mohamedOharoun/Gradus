package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Qué enseña el botón de inscribirse en cada estado, cuándo se puede pulsar y cuánto mide.
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class EnrollButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var taps = 0

    private fun show(state: EnrollmentState) {
        taps = 0
        composeRule.setContentInSpanish { GradusTheme { EnrollButton(state = state, onEnroll = { taps++ }) } }
    }

    @Test
    fun not_enrolled_shows_enroll_and_taps_call_onEnroll() {
        show(EnrollmentState.NotEnrolled)

        composeRule.onNodeWithText("Inscribirme").assertIsEnabled().performClick()

        assertEquals(1, taps)
    }

    @Test
    fun enrolling_shows_enrolling_and_cannot_be_tapped() {
        show(EnrollmentState.Enrolling)

        composeRule.onNodeWithText("Inscribiendo…").assertIsNotEnabled().performClick()

        assertEquals(0, taps)
    }

    @Test
    fun enrolled_shows_enrolled_and_is_disabled() {
        show(EnrollmentState.Enrolled)

        composeRule.onNodeWithText("Inscrito").assertIsNotEnabled().performClick()

        assertEquals(0, taps)
    }

    // Mientras no se sabe, se ve igual que antes de inscribirse pero sin poder pulsarlo.
    @Test
    fun unknown_shows_enroll_but_is_disabled() {
        show(EnrollmentState.Unknown)

        composeRule.onNodeWithText("Inscribirme").assertIsNotEnabled().performClick()

        assertEquals(0, taps)
    }

    // --- Zona táctil de 48 dp como mínimo, en cada estado ---

    @Test
    fun the_touch_target_is_at_least_48dp_when_unknown() = assertTouchTarget(EnrollmentState.Unknown, "Inscribirme")

    @Test
    fun the_touch_target_is_at_least_48dp_when_not_enrolled() = assertTouchTarget(EnrollmentState.NotEnrolled, "Inscribirme")

    @Test
    fun the_touch_target_is_at_least_48dp_when_enrolling() = assertTouchTarget(EnrollmentState.Enrolling, "Inscribiendo…")

    @Test
    fun the_touch_target_is_at_least_48dp_when_enrolled() = assertTouchTarget(EnrollmentState.Enrolled, "Inscrito")

    @Test
    fun in_english_it_uses_the_translated_labels() {
        composeRule.setContentInEnglish { GradusTheme { EnrollButton(state = EnrollmentState.NotEnrolled, onEnroll = {}) } }
        composeRule.onNodeWithText("Enroll").assertIsEnabled()
    }

    private fun assertTouchTarget(state: EnrollmentState, text: String) {
        show(state)
        composeRule.onNodeWithText(text).assertHeightIsAtLeast(48.dp)
    }
}
