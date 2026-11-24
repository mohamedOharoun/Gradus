package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.ProfileForm
import es.ulpgc.pamn.dominio.model.Submission
import kotlin.test.Test
import kotlin.test.assertEquals

// La respuesta del servidor al cambiar el nombre, aplicada al formulario de «Editar perfil».
class ProfileApplyingOutcomeTest {

    private val sending = ProfileForm(original = "Ana").withName("Ana Ruiz").sending()
    private val renamed = Account("u1", "Ana Ruiz", "ana@ulpgc.es", "2026-11-17T10:00:00Z")

    @Test
    fun `renamed is saved`() {
        assertEquals(Submission.Succeeded, sending.applying(RenameOutcome.Renamed(renamed)).submission)
    }

    @Test
    fun `the errors of the name are shown under it`() {
        mapOf("REQUIRED" to FieldError.EMPTY, "TOO_SHORT" to FieldError.TOO_SHORT, "TOO_LONG" to FieldError.TOO_LONG)
            .forEach { (code, error) ->
                val form = sending.applying(RenameOutcome.Rejected(listOf(ServerFieldError("name", code))))
                assertEquals(error, form.nameError, code)
                assertEquals(Submission.Idle, form.submission, code)
            }
    }

    // Un 400 que la app no entiende es un fallo de la app o del servidor, no del usuario.
    @Test
    fun `a rejection the app does not understand is a server failure`() {
        val form = sending.applying(RenameOutcome.Rejected(listOf(ServerFieldError("name", "SOMETHING_NEW"))))
        assertEquals(Submission.Failed(FailureReason.SERVER_FAILURE), form.submission)
    }

    // La sesión ya no vale: el ViewModel saca a Bienvenida, el formulario no cambia.
    @Test
    fun `an expired session leaves the form as it was`() {
        assertEquals(sending, sending.applying(RenameOutcome.SessionExpired))
    }

    @Test
    fun `without connection it can be retried`() {
        assertEquals(Submission.Failed(FailureReason.UNREACHABLE), sending.applying(RenameOutcome.Unreachable).submission)
    }

    @Test
    fun `a server failure can be retried`() {
        assertEquals(Submission.Failed(FailureReason.SERVER_FAILURE), sending.applying(RenameOutcome.ServerFailure).submission)
    }
}
