package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.account.ApiErrorCode.INVALID_EMAIL
import es.ulpgc.pamn.dominio.account.ApiErrorCode.REQUIRED
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.LoginField
import es.ulpgc.pamn.dominio.model.LoginForm
import es.ulpgc.pamn.dominio.model.LoginProblem

// Aplica la respuesta del servidor al formulario de inicio de sesión. Está en account/ (y no dentro
// de LoginForm) para que model/ no dependa de account/, como ApplyingOutcome.kt en el registro.
fun LoginForm.applying(outcome: LogInOutcome): LoginForm = when (outcome) {
    is LogInOutcome.LoggedIn -> succeeded()
    // Un 400 sin ningún error de campo que entendamos es un fallo de la app o del servidor, no del usuario.
    is LogInOutcome.Rejected -> outcome.errors.firstLoginProblem()?.let(::rejected)
        ?: failed(FailureReason.SERVER_FAILURE)
    LogInOutcome.InvalidCredentials -> rejected(LoginProblem(field = null, error = FieldError.INVALID_CREDENTIALS))
    LogInOutcome.Unreachable -> failed(FailureReason.UNREACHABLE)
    LogInOutcome.ServerFailure -> failed(FailureReason.SERVER_FAILURE)
}

// Tabla de traducción (campo, código) → problema, como la del registro (RegisterProblem.kt).
// El backend solo puede rechazar en un 400 un correo vacío o mal escrito y una contraseña vacía.
private val translations: Map<Pair<String, ApiErrorCode>, LoginProblem> = mapOf(
    ("email" to REQUIRED) to LoginProblem(LoginField.EMAIL, FieldError.EMPTY),
    ("email" to INVALID_EMAIL) to LoginProblem(LoginField.EMAIL, FieldError.INVALID_EMAIL),
    ("password" to REQUIRED) to LoginProblem(LoginField.PASSWORD, FieldError.EMPTY),
)

// El del primer campo de la pantalla (el correo va antes que la contraseña).
private fun List<ServerFieldError>.firstLoginProblem(): LoginProblem? =
    mapNotNull { translations[it.field to it.apiCode] }.minByOrNull { it.field?.ordinal ?: -1 }
