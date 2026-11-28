package es.ulpgc.pamn.dominio.account

import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.ProfileForm

// Aplica la respuesta del servidor al formulario «Editar perfil». Está en account/ (y no dentro de
// ProfileForm) para que model/ no dependa de account/, como LogInApplyingOutcome.kt.
// SessionExpired no cambia el formulario: la sesión ya no vale y el ViewModel saca a Bienvenida.
fun ProfileForm.applying(outcome: RenameOutcome): ProfileForm = when (outcome) {
    is RenameOutcome.Renamed -> succeeded()
    // Un 400 sin ningún error que entendamos es un fallo de la app o del servidor, no del usuario.
    is RenameOutcome.Rejected -> outcome.errors.firstNotNullOfOrNull { nameErrors[it.field to it.apiCode] }
        ?.let(::rejected) ?: failed(FailureReason.SERVER_FAILURE)
    RenameOutcome.SessionExpired -> this
    RenameOutcome.Unreachable -> failed(FailureReason.UNREACHABLE)
    RenameOutcome.ServerFailure -> failed(FailureReason.SERVER_FAILURE)
}

// Tabla de traducción (campo, código) → error, como la del inicio de sesión.
private val nameErrors: Map<Pair<String, ApiErrorCode>, FieldError> = mapOf(
    ("name" to ApiErrorCode.REQUIRED) to FieldError.EMPTY,
    ("name" to ApiErrorCode.TOO_SHORT) to FieldError.TOO_SHORT,
    ("name" to ApiErrorCode.TOO_LONG) to FieldError.TOO_LONG,
)
