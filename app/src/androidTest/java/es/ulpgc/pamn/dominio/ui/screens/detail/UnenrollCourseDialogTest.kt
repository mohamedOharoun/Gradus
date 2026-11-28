package es.ulpgc.pamn.dominio.ui.screens.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// El diálogo destructivo «¿Darse de baja del curso?» montado en ScreenCourseDetail
// (docs/tareas/ficha-curso-ui.md, Decisión C-5 y tarea C5).
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class UnenrollCourseDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val course = Course(
        id = "g",
        title = "Introducción a los grafos",
        modules = listOf(Module("repr", "Representar un grafo")),
    )

    private fun show(
        dialogState: UnenrollDialogState,
        actions: CourseDetailActions = CourseDetailActions(),
    ) = composeRule.setContentInSpanish {
        GradusTheme {
            ScreenCourseDetail(
                state = CourseDetailState.Ready(
                    course = course,
                    enrolled = true,
                    unenrollDialog = dialogState,
                ),
                actions = actions,
            )
        }
    }

    private fun button(text: String) = composeRule.onNode(hasClickAction() and hasText(text))

    @Test
    fun closed_state_does_not_show_the_dialog() {
        show(UnenrollDialogState.Closed)

        composeRule.onNodeWithText("¿Darse de baja del curso?").assertDoesNotExist()
    }

    @Test
    fun confirming_shows_warning_and_confirm_calls_onConfirmUnenroll() {
        var confirms = 0
        show(UnenrollDialogState.Confirming, CourseDetailActions(onConfirmUnenroll = { confirms++ }))

        composeRule.onNodeWithText("¿Darse de baja del curso?").assertIsDisplayed()
        composeRule.onNodeWithText("Se perderá todo tu progreso en este curso. No se puede deshacer.")
            .assertIsDisplayed()
        button("Darse de baja").assertIsEnabled().performClick()

        assertEquals(1, confirms)
    }

    @Test
    fun confirming_cancel_calls_onDismissUnenrollDialog() {
        var dismisses = 0
        show(UnenrollDialogState.Confirming, CourseDetailActions(onDismissUnenrollDialog = { dismisses++ }))

        button("Cancelar").assertIsEnabled().performClick()

        assertEquals(1, dismisses)
    }

    @Test
    fun sending_disables_both_buttons_and_ignores_taps() {
        var confirms = 0
        var dismisses = 0
        show(
            UnenrollDialogState.Sending,
            CourseDetailActions(
                onConfirmUnenroll = { confirms++ },
                onDismissUnenrollDialog = { dismisses++ },
            ),
        )

        button("Dando de baja…").assertIsDisplayed().assertIsNotEnabled().performClick()
        button("Cancelar").assertIsDisplayed().assertIsNotEnabled().performClick()

        assertEquals(0, confirms)
        assertEquals(0, dismisses)
    }

    @Test
    fun failed_unreachable_shows_error_and_allows_retry_or_cancel() {
        var confirms = 0
        var dismisses = 0
        show(
            UnenrollDialogState.Failed(FailureReason.UNREACHABLE),
            CourseDetailActions(
                onConfirmUnenroll = { confirms++ },
                onDismissUnenrollDialog = { dismisses++ },
            ),
        )

        composeRule.onNodeWithText("No hay conexión con el servidor", substring = true).assertIsDisplayed()
        button("Darse de baja").assertIsEnabled().performClick()
        button("Cancelar").assertIsEnabled().performClick()

        assertEquals(1, confirms)
        assertEquals(1, dismisses)
    }

    @Test
    fun failed_server_failure_shows_error_and_allows_retry_or_cancel() {
        var confirms = 0
        var dismisses = 0
        show(
            UnenrollDialogState.Failed(FailureReason.SERVER_FAILURE),
            CourseDetailActions(
                onConfirmUnenroll = { confirms++ },
                onDismissUnenrollDialog = { dismisses++ },
            ),
        )

        composeRule.onNodeWithText("Algo ha fallado en el servidor", substring = true).assertIsDisplayed()
        button("Darse de baja").assertIsEnabled().performClick()
        button("Cancelar").assertIsEnabled().performClick()

        assertEquals(1, confirms)
        assertEquals(1, dismisses)
    }
}
