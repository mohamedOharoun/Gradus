package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.model.DeleteAccountForm
import es.ulpgc.pamn.dominio.model.DeleteProblem
import es.ulpgc.pamn.dominio.model.FailureReason

// Aplica la respuesta del servidor al diálogo «Eliminar cuenta». Está en account/ para que model/ no
// dependa de account/, como los demás *ApplyingOutcome.kt.
// - Rejected: la app no envía la contraseña vacía, así que un 400 es un fallo, no algo que corregir.
// - SessionExpired no cambia el formulario: el ViewModel borra la sesión y saca a Bienvenida.
fun DeleteAccountForm.applying(outcome: DeleteAccountOutcome): DeleteAccountForm = when (outcome) {
    DeleteAccountOutcome.Deleted -> succeeded()
    is DeleteAccountOutcome.Rejected -> failed(FailureReason.SERVER_FAILURE)
    DeleteAccountOutcome.WrongPassword -> rejected(DeleteProblem.WRONG_PASSWORD)
    DeleteAccountOutcome.AuthorOfCourses -> rejected(DeleteProblem.AUTHOR_OF_COURSES)
    DeleteAccountOutcome.SessionExpired -> this
    DeleteAccountOutcome.Unreachable -> failed(FailureReason.UNREACHABLE)
    DeleteAccountOutcome.ServerFailure -> failed(FailureReason.SERVER_FAILURE)
}
