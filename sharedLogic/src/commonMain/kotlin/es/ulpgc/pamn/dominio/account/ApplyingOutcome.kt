package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.RegisterForm
import es.ulpgc.pamn.dominio.model.RegisterProblem
import es.ulpgc.pamn.dominio.model.RegisterStep

// Aplica la respuesta del servidor al formulario. Está en account/ (y no dentro de RegisterForm)
// para que model/ no dependa de account/: el formulario no sabe que existe un servidor.
fun RegisterForm.applying(outcome: RegistrationOutcome): RegisterForm = when (outcome) {
    is RegistrationOutcome.Registered -> succeeded()
    // Un 400 sin ningún error de campo que entendamos es un fallo de la app o del servidor, no del usuario.
    is RegistrationOutcome.Rejected -> outcome.errors.firstRegisterProblem()?.let(::rejected)
        ?: failed(FailureReason.SERVER_FAILURE)
    RegistrationOutcome.EmailTaken -> rejected(RegisterProblem(RegisterStep.EMAIL, FieldError.EMAIL_TAKEN))
    RegistrationOutcome.Unreachable -> failed(FailureReason.UNREACHABLE)
    RegistrationOutcome.ServerFailure -> failed(FailureReason.SERVER_FAILURE)
}
