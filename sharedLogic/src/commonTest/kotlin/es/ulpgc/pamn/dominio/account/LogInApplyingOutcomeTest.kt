package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.LoginField
import es.ulpgc.pamn.dominio.model.LoginForm
import es.ulpgc.pamn.dominio.model.LoginProblem
import es.ulpgc.pamn.dominio.model.Submission
import kotlin.test.Test
import kotlin.test.assertEquals

// Cómo cambia el formulario de inicio de sesión con cada respuesta del servidor.
class LogInApplyingOutcomeTest {

    private val sending = LoginForm(email = "ana@ulpgc.es", password = "Gradus2026!").sending()
    private val session = Session("t", "2026-12-18T10:00:00Z", Account("id", "Ana", "ana@ulpgc.es", "2026-11-18T10:00:00Z"))

    @Test
    fun `logged in succeeds`() {
        assertEquals(Submission.Succeeded, sending.applying(LogInOutcome.LoggedIn(session)).submission)
    }

    @Test
    fun `invalid credentials shows the general error`() {
        val form = sending.applying(LogInOutcome.InvalidCredentials)
        assertEquals(LoginProblem(field = null, error = FieldError.INVALID_CREDENTIALS), form.serverProblem)
        assertEquals(Submission.Idle, form.submission)
    }

    @Test
    fun `a rejected email goes under the email field`() {
        val form = sending.applying(LogInOutcome.Rejected(listOf(ServerFieldError("email", "INVALID_EMAIL"))))
        assertEquals(LoginProblem(LoginField.EMAIL, FieldError.INVALID_EMAIL), form.serverProblem)
    }

    @Test
    fun `a required email goes under the email field`() {
        val form = sending.applying(LogInOutcome.Rejected(listOf(ServerFieldError("email", "REQUIRED"))))
        assertEquals(LoginProblem(LoginField.EMAIL, FieldError.EMPTY), form.serverProblem)
    }

    @Test
    fun `a required password goes under the password field`() {
        val form = sending.applying(LogInOutcome.Rejected(listOf(ServerFieldError("password", "REQUIRED"))))
        assertEquals(LoginProblem(LoginField.PASSWORD, FieldError.EMPTY), form.serverProblem)
    }

    @Test
    fun `with several errors the email one is shown first`() {
        val errors = listOf(ServerFieldError("password", "REQUIRED"), ServerFieldError("email", "INVALID_EMAIL"))
        assertEquals(LoginField.EMAIL, sending.applying(LogInOutcome.Rejected(errors)).serverProblem?.field)
    }

    // Un 400 que la app no entiende es un fallo de la app o del servidor, no del usuario.
    @Test
    fun `a rejection with no known field error is a server failure`() {
        val form = sending.applying(LogInOutcome.Rejected(listOf(ServerFieldError("body", "MALFORMED_BODY"))))
        assertEquals(Submission.Failed(FailureReason.SERVER_FAILURE), form.submission)
    }

    @Test
    fun `unreachable fails as unreachable`() {
        assertEquals(Submission.Failed(FailureReason.UNREACHABLE), sending.applying(LogInOutcome.Unreachable).submission)
    }

    @Test
    fun `a server failure fails as a server failure`() {
        assertEquals(Submission.Failed(FailureReason.SERVER_FAILURE), sending.applying(LogInOutcome.ServerFailure).submission)
    }
}
