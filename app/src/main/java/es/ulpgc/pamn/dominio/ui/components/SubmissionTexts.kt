package es.ulpgc.pamn.dominio.ui.components

import es.ulpgc.pamn.dominio.model.Submission

// Texto del botón principal de un formulario que se envía al servidor (registro, inicio de sesión…).
// Cada formulario dice cómo se llama su acción; el resto es igual para todos.
// Ejemplo: Submission.Sending.buttonText(idle = "Entrar", sending = "Entrando…") == "Entrando…".
fun Submission.buttonText(idle: String, sending: String): String = when (this) {
    Submission.Sending -> sending
    is Submission.Failed -> "Reintentar"
    Submission.Idle, Submission.Succeeded -> idle
}
