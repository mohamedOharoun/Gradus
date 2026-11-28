package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.LoginForm
import es.ulpgc.pamn.dominio.model.RegisterForm
import es.ulpgc.pamn.dominio.model.RegisterStep
import es.ulpgc.pamn.dominio.model.Submission
import es.ulpgc.pamn.dominio.ui.screens.login.LoginActions
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterActions
import es.ulpgc.pamn.dominio.ui.testing.setContentInEnglish
import es.ulpgc.pamn.dominio.ui.testing.setContentInSpanish
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Bienvenida, Inicio de sesión y Registro en los dos idiomas (docs/tareas/ajustes.md, fase 3, paso 6).
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class AccountScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── Bienvenida ──

    @Test
    fun welcome_in_spanish() {
        composeRule.setContentInSpanish { GradusTheme { ScreenWelcome({}, {}, {}) } }
        composeRule.onNodeWithText("Aprende paso a paso, sin saltarte la base.").assertIsDisplayed()
        composeRule.onNodeWithText("Crear cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("Iniciar sesión").assertIsDisplayed()
    }

    @Test
    fun welcome_in_english() {
        composeRule.setContentInEnglish { GradusTheme { ScreenWelcome({}, {}, {}) } }
        composeRule.onNodeWithText("Learn step by step, without skipping the basics.").assertIsDisplayed()
        composeRule.onNodeWithText("Create account").assertIsDisplayed()
        composeRule.onNodeWithText("Log in").assertIsDisplayed()
        composeRule.onNodeWithText("Explore courses without an account").assertIsDisplayed()
    }

    // ── Inicio de sesión ──

    @Test
    fun login_errors_in_english() {
        composeRule.setContentInEnglish {
            GradusTheme { ScreenLogin(LoginForm(email = "ana@correo", isSubmitted = true), LoginActions()) }
        }
        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
        composeRule.onNodeWithText("Enter a valid email, like name@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("This field is required").assertIsDisplayed()
        // El título de arriba y el botón: los dos «Log in».
        composeRule.onAllNodesWithText("Log in").assertCountEquals(2)
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    // Sin conexión: el aviso encima del botón y el mismo botón dice «Reintentar».
    @Test
    fun login_without_connection_in_english() {
        val failed = LoginForm(email = "ana@ulpgc.es", password = "x", submission = Submission.Failed(FailureReason.UNREACHABLE))
        composeRule.setContentInEnglish { GradusTheme { ScreenLogin(failed, LoginActions()) } }
        composeRule.onNodeWithText("Can't reach the server. Check your connection and try again.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun login_without_connection_in_spanish() {
        val failed = LoginForm(email = "ana@ulpgc.es", password = "x", submission = Submission.Failed(FailureReason.UNREACHABLE))
        composeRule.setContentInSpanish { GradusTheme { ScreenLogin(failed, LoginActions()) } }
        composeRule.onNodeWithText("No hay conexión con el servidor. Comprueba tu red e inténtalo de nuevo.").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").assertIsDisplayed()
    }

    // ── Registro ──

    // TalkBack lee el indicador de pasos como una frase, también traducida.
    @Test
    fun the_register_steps_are_read_in_each_language() {
        composeRule.setContentInEnglish { GradusTheme { ScreenRegister(RegisterForm(), RegisterActions()) } }
        composeRule.onNodeWithContentDescription("Step 1 of 3: Name").assertIsDisplayed()
        composeRule.onNodeWithText("What's your name?").assertIsDisplayed()
    }

    @Test
    fun the_register_steps_in_spanish() {
        composeRule.setContentInSpanish { GradusTheme { ScreenRegister(RegisterForm(), RegisterActions()) } }
        composeRule.onNodeWithContentDescription("Paso 1 de 3: Nombre").assertIsDisplayed()
        composeRule.onNodeWithText("¿Cómo te llamas?").assertIsDisplayed()
    }

    @Test
    fun the_password_step_in_english() {
        val form = RegisterForm(name = "Ana", email = "ana@ulpgc.es", step = RegisterStep.PASSWORD, password = "abc", confirmation = "abd")
        composeRule.setContentInEnglish { GradusTheme { ScreenRegister(form, RegisterActions()) } }
        composeRule.onNodeWithText("Create a password").assertIsDisplayed()
        composeRule.onNodeWithText("At least 8 characters").assertExists()
        composeRule.onNodeWithText("Repeat the password").assertExists()
        composeRule.onNodeWithText("The passwords do not match").assertExists()
        composeRule.onNodeWithText("I accept the terms of use and the privacy policy").assertExists()
        composeRule.onNodeWithContentDescription("Step 3 of 3: Password").assertIsDisplayed()
    }
}
