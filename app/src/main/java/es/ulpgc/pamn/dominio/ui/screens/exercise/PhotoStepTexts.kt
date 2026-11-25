package es.ulpgc.pamn.dominio.ui.screens.exercise

import es.ulpgc.pamn.dominio.course.study.PhotoStep
import es.ulpgc.pamn.dominio.course.study.PhotoTask
import es.ulpgc.pamn.dominio.submission.PhotoRejection
import es.ulpgc.pamn.dominio.submission.SubmitOutcome

// Textos y botones del paso con foto (docs/tareas/ejercicio-foto.md, paso 5c). Funciones puras, con test
// JVM: la pantalla solo los pinta. Los textos van en el código hasta que haya idiomas (AGENTS.md §6).

// Qué ha pasado al entregar, dicho para el alumno.
fun SubmitOutcome.Problem.text(): String = when (this) {
    SubmitOutcome.Unreachable -> "Sin conexión. Comprueba la red y vuelve a intentarlo."
    SubmitOutcome.ServerFailure -> "El servidor no ha podido recibir la foto. Vuelve a intentarlo en un momento."
    SubmitOutcome.SessionExpired -> "Tu sesión ha caducado. Vuelve a iniciar sesión para entregar."
    SubmitOutcome.NotFound -> "Este ejercicio ya no está en el servidor: el curso ha cambiado. Déjalo para más tarde."
    is SubmitOutcome.PhotoRejected -> when (reason) {
        PhotoRejection.MISSING -> "La foto ya no está en el móvil. Haz otra."
        PhotoRejection.NOT_JPEG -> "Esa imagen no se puede entregar. Prueba con otra foto."
        PhotoRejection.TOO_LARGE -> "La foto es demasiado grande. Prueba con otra."
    }
}

data class MainPhotoButton(val text: String, val enabled: Boolean)

// El botón principal: «Entregar», «Entregando…», «Reintentar» o, ya entregada, «Continuar».
// `canSubmit`: el curso admite entregas (decisión Z).
fun PhotoStep.mainButton(canSubmit: Boolean): MainPhotoButton = when (val current = task) {
    PhotoTask.Sent -> MainPhotoButton("Continuar", enabled = true)
    is PhotoTask.Sending -> MainPhotoButton("Entregando…", enabled = false)
    is PhotoTask.Failed ->
        if (current.problem.canRetry) MainPhotoButton("Reintentar", enabled = canSubmit) else MainPhotoButton("Entregar", enabled = false)
    else -> MainPhotoButton("Entregar", enabled = canSubmit && canSend)
}

// El secundario: dejarla para más tarde (S), entregar otra vez (I) o, si ya hay una entrega que cuenta,
// seguir sin entregar otra. Mientras se entrega, ninguno.
enum class SecondaryPhotoButton(val text: String) {
    LATER("Entregar más tarde"),
    AGAIN("Entregar otra vez"),
    GO_ON("Continuar sin entregar otra"),
}

fun PhotoStep.secondaryButton(): SecondaryPhotoButton? = when {
    task is PhotoTask.Sending -> null
    task == PhotoTask.Sent -> SecondaryPhotoButton.AGAIN
    submission != null -> SecondaryPhotoButton.GO_ON
    else -> SecondaryPhotoButton.LATER
}
