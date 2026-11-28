package es.ulpgc.pamn.dominio.submission

// Por qué el servidor no acepta la foto (400 y 413 de POST …/submissions).
enum class PhotoRejection {
    MISSING, // REQUIRED: no llegó como fichero
    NOT_JPEG, // INVALID_PHOTO
    TOO_LARGE, // PHOTO_TOO_LARGE
}

// Todos los desenlaces de entregar una foto, también los fallos de red (docs/tareas/ejercicio-foto.md,
// decisión T). Los fallos son un `Problem`: el estado del paso los guarda y decide si se puede reintentar.
sealed interface SubmitOutcome {

    // 201: entregada y pendiente de revisión.
    data class Submitted(val submission: Submission) : SubmitOutcome

    sealed interface Problem : SubmitOutcome {
        // ¿Tiene sentido volver a enviar la misma foto?
        val canRetry: Boolean
    }

    // 400 o 413: la foto no vale. Reenviarla daría lo mismo: hay que hacer otra.
    data class PhotoRejected(val reason: PhotoRejection) : Problem {
        override val canRetry: Boolean get() = false
    }

    // 404: el curso, la versión o el ejercicio ya no están en el servidor. Reintentar no sirve.
    data object NotFound : Problem {
        override val canRetry: Boolean get() = false
    }

    // 401: hay que volver a iniciar sesión; después, se reintenta con la misma foto (decisión L).
    data object SessionExpired : Problem {
        override val canRetry: Boolean get() = true
    }

    // Sin red, servidor apagado o tiempo agotado (decisión J: aviso y «Reintentar»).
    data object Unreachable : Problem {
        override val canRetry: Boolean get() = true
    }

    // El servidor respondió algo inesperado (5xx o un cuerpo que no se entiende).
    data object ServerFailure : Problem {
        override val canRetry: Boolean get() = true
    }
}
