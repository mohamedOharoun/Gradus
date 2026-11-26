package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.account.ApiErrorCode.MISSING_DIGIT
import es.ulpgc.pamn.dominio.account.ApiErrorCode.MISSING_LETTER
import es.ulpgc.pamn.dominio.account.ApiErrorCode.MISSING_SPECIAL_CHARACTER
import es.ulpgc.pamn.dominio.account.ApiErrorCode.REQUIRED
import es.ulpgc.pamn.dominio.account.ApiErrorCode.TOO_SHORT
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.PasswordChangeField
import es.ulpgc.pamn.dominio.model.PasswordForm
import es.ulpgc.pamn.dominio.model.PasswordProblem

// Puente entre el formulario «Cambiar contraseña» (model/) y el servidor (account/). Está aquí, y no
// en PasswordForm, para que model/ no dependa de account/ (como toCredentials del inicio de sesión).

// Lo que se envía: las contraseñas tal cual se escribieron.
fun PasswordForm.toChange(): PasswordChange = PasswordChange(currentPassword = current, newPassword = next)

// SessionExpired no cambia el formulario: la sesión ya no vale y el ViewModel saca a Bienvenida.
fun PasswordForm.applying(outcome: ChangePasswordOutcome): PasswordForm = when (outcome) {
    ChangePasswordOutcome.Changed -> succeeded()
    // Un 400 sin ningún error que entendamos es un fallo de la app o del servidor, no del usuario.
    is ChangePasswordOutcome.Rejected -> outcome.errors.firstPasswordProblem()?.let(::rejected)
        ?: failed(FailureReason.SERVER_FAILURE)
    ChangePasswordOutcome.WrongPassword -> rejected(PasswordProblem.WrongCurrent)
    ChangePasswordOutcome.SessionExpired -> this
    ChangePasswordOutcome.Unreachable -> failed(FailureReason.UNREACHABLE)
    ChangePasswordOutcome.ServerFailure -> failed(FailureReason.SERVER_FAILURE)
}

// Tabla de traducción (campo, código) → problema. Cualquier regla de la nueva que no se cumpla es
// «no cumple los requisitos», como en el registro: la lista en vivo ya dice cuál falta.
private val translations: Map<Pair<String, ApiErrorCode>, PasswordProblem.InField> =
    mapOf(("currentPassword" to REQUIRED) to PasswordProblem.InField(PasswordChangeField.CURRENT, FieldError.EMPTY)) +
        mapOf(("newPassword" to REQUIRED) to PasswordProblem.InField(PasswordChangeField.NEW, FieldError.EMPTY)) +
        listOf(TOO_SHORT, MISSING_LETTER, MISSING_DIGIT, MISSING_SPECIAL_CHARACTER).associate { code ->
            ("newPassword" to code) to PasswordProblem.InField(PasswordChangeField.NEW, FieldError.WEAK_PASSWORD)
        }

// El del primer campo de la pantalla (la actual va antes que la nueva).
private fun List<ServerFieldError>.firstPasswordProblem(): PasswordProblem.InField? =
    mapNotNull { translations[it.field to it.apiCode] }.minByOrNull { it.field.ordinal }
