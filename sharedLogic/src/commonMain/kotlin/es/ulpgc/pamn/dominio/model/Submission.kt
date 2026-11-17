package es.ulpgc.pamn.dominio.model

// En qué punto está el envío del registro al servidor.
sealed interface Submission {

    // Se puede enviar (si el formulario está listo)? Solo mientras no se envía ni se ha terminado.
    val allowsSending: Boolean

    // Todavía no se ha enviado, o el servidor ha rechazado un dato y hay que corregirlo.
    data object Idle : Submission {
        override val allowsSending = true
    }

    // Esperando la respuesta: el botón se desactiva para no crear la cuenta dos veces.
    data object Sending : Submission {
        override val allowsSending = false
    }

    // La cuenta se ha creado: ya no se puede volver a enviar.
    data object Succeeded : Submission {
        override val allowsSending = false
    }

    // No se ha podido completar; los datos se conservan y se puede reintentar.
    data class Failed(val reason: FailureReason) : Submission {
        override val allowsSending = true
    }
}

// Por qué ha fallado un envío. La interfaz decide el texto de cada caso.
enum class FailureReason {
    // Sin conexión, servidor apagado o tiempo agotado.
    UNREACHABLE,

    // El servidor ha respondido algo inesperado.
    SERVER_FAILURE,
}
