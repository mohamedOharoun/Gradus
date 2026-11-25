package es.ulpgc.pamn.dominio.course.study

import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.submission.SubmitOutcome
import es.ulpgc.pamn.dominio.submission.Submission

// En qué punto está la foto de una tarea (docs/tareas/ejercicio-foto.md, paso 4b). `photo` es la ruta
// del fichero temporal en el móvil: aquí es solo un texto, la app sabe leerlo y borrarlo.
sealed interface PhotoTask {
    data object Empty : PhotoTask

    data class Ready(val photo: String) : PhotoTask

    data class Sending(val photo: String) : PhotoTask

    // La foto se conserva para reintentar (decisión J).
    data class Failed(val photo: String, val problem: SubmitOutcome.Problem) : PhotoTask

    // Recién entregada: se ve la solución y «Entregar otra vez». El fichero ya no hace falta.
    data object Sent : PhotoTask
}

// Una tarea con foto mientras se estudia. Máquina de estados inmutable, como ExerciseState: cada
// acción devuelve un estado nuevo y lo que no tiene sentido (cambiar la foto mientras se entrega,
// una respuesta que llega tarde…) devuelve el mismo. `submission` es la última entrega que cuenta
// (decisión I); `later`, si se dejó para más tarde (decisión S).
data class PhotoStep(
    val photo: Step.Photo,
    val task: PhotoTask = PhotoTask.Empty,
    val submission: Submission? = null,
    val later: Boolean = false,
) {
    // Se puede seguir si ya hay una entrega (aunque se esté haciendo otra) o se dejó para más tarde.
    val isDone: Boolean get() = later || submission != null

    val canSend: Boolean
        get() = task is PhotoTask.Ready || (task is PhotoTask.Failed && task.problem.canRetry)

    // Mientras se entrega, no: la respuesta llegaría a un paso que ya no se ve.
    val canLeaveForLater: Boolean get() = task !is PhotoTask.Sending && task !is PhotoTask.Sent

    // Decisión C: la solución se enseña al entregar.
    val showsSolution: Boolean get() = task is PhotoTask.Sent

    // El fichero de la foto mientras hace falta; cuando deja de estar aquí, la app lo borra (decisión K).
    val photoPath: String?
        get() = when (task) {
            is PhotoTask.Ready -> task.photo
            is PhotoTask.Sending -> task.photo
            is PhotoTask.Failed -> task.photo
            PhotoTask.Empty, PhotoTask.Sent -> null
        }

    // Una foto nueva (cámara o galería): sustituye a la que hubiera, salvo mientras se entrega o ya entregada.
    fun take(path: String): PhotoStep = when (task) {
        PhotoTask.Empty, is PhotoTask.Ready, is PhotoTask.Failed -> copy(task = PhotoTask.Ready(path))
        is PhotoTask.Sending, PhotoTask.Sent -> this
    }

    fun discard(): PhotoStep = when (task) {
        is PhotoTask.Ready, is PhotoTask.Failed -> copy(task = PhotoTask.Empty)
        else -> this
    }

    // «Entregar» o «Reintentar». Quien llama envía la foto y después avisa con finished().
    fun send(): PhotoStep {
        val path = photoPath ?: return this
        return if (canSend) copy(task = PhotoTask.Sending(path)) else this
    }

    // La respuesta del servidor. Solo cuenta la de un envío en curso.
    fun finished(outcome: SubmitOutcome): PhotoStep {
        val sending = task as? PhotoTask.Sending ?: return this
        return when (outcome) {
            is SubmitOutcome.Submitted -> copy(task = PhotoTask.Sent, submission = outcome.submission, later = false)
            is SubmitOutcome.Problem -> copy(task = PhotoTask.Failed(sending.photo, outcome))
        }
    }

    // «Entregar otra vez»: sin foto, pero la entrega anterior sigue contando.
    fun again(): PhotoStep = if (task is PhotoTask.Sent) copy(task = PhotoTask.Empty) else this

    // Se sale del paso sin entregar: la foto deja de hacer falta y la app la borra (decisión K).
    fun leaveForLater(): PhotoStep = if (canLeaveForLater) copy(later = true, task = PhotoTask.Empty) else this
}
