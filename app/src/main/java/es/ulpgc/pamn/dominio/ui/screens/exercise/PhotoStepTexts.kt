package es.ulpgc.pamn.dominio.ui.screens.exercise

import androidx.annotation.StringRes
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.study.PhotoStep
import es.ulpgc.pamn.dominio.course.study.PhotoTask
import es.ulpgc.pamn.dominio.submission.PhotoRejection
import es.ulpgc.pamn.dominio.submission.SubmitOutcome
import es.ulpgc.pamn.dominio.ui.text.UiText

// Textos y botones del paso con foto (docs/tareas/ejercicio-foto.md, paso 5c). Funciones puras, con test
// JVM: la pantalla solo los pinta. Con UiText desde ajustes.md, fase 3, paso 8b.

// Qué ha pasado al entregar, dicho para el alumno.
fun SubmitOutcome.Problem.text(): UiText = UiText.of(
    when (this) {
        SubmitOutcome.Unreachable -> R.string.photo_error_unreachable
        SubmitOutcome.ServerFailure -> R.string.photo_error_server
        SubmitOutcome.SessionExpired -> R.string.photo_error_session_expired
        SubmitOutcome.NotFound -> R.string.photo_error_not_found
        is SubmitOutcome.PhotoRejected -> when (reason) {
            PhotoRejection.MISSING -> R.string.photo_error_missing
            PhotoRejection.NOT_JPEG -> R.string.photo_error_not_jpeg
            PhotoRejection.TOO_LARGE -> R.string.photo_error_too_large
        }
    },
)

data class MainPhotoButton(val text: UiText, val enabled: Boolean)

// El botón principal: «Entregar», «Entregando…», «Reintentar» o, ya entregada, «Continuar».
// `canSubmit`: el curso admite entregas (decisión Z).
fun PhotoStep.mainButton(canSubmit: Boolean): MainPhotoButton = when (val current = task) {
    PhotoTask.Sent -> MainPhotoButton(UiText.of(R.string.action_continue), enabled = true)
    is PhotoTask.Sending -> MainPhotoButton(UiText.of(R.string.photo_submitting), enabled = false)
    is PhotoTask.Failed ->
        if (current.problem.canRetry) MainPhotoButton(UiText.of(R.string.action_retry), enabled = canSubmit)
        else MainPhotoButton(UiText.of(R.string.photo_submit), enabled = false)
    else -> MainPhotoButton(UiText.of(R.string.photo_submit), enabled = canSubmit && canSend)
}

// El secundario: dejarla para más tarde (S), entregar otra vez (I) o, si ya hay una entrega que cuenta,
// seguir sin entregar otra. Mientras se entrega, ninguno.
enum class SecondaryPhotoButton(@StringRes private val id: Int) {
    LATER(R.string.photo_later),
    AGAIN(R.string.photo_again),
    GO_ON(R.string.photo_go_on),
    ;

    val text: UiText get() = UiText.of(id)
}

fun PhotoStep.secondaryButton(): SecondaryPhotoButton? = when {
    task is PhotoTask.Sending -> null
    task == PhotoTask.Sent -> SecondaryPhotoButton.AGAIN
    submission != null -> SecondaryPhotoButton.GO_ON
    else -> SecondaryPhotoButton.LATER
}
