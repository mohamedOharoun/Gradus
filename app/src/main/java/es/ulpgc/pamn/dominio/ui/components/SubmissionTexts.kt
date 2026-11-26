package es.ulpgc.pamn.dominio.ui.components

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.model.Submission
import es.ulpgc.pamn.dominio.ui.text.UiText

// Texto del botón principal de un formulario que se envía al servidor (registro, inicio de sesión…).
// Cada formulario dice cómo se llama su acción; el resto es igual para todos.
// Ejemplo: Submission.Sending.buttonText(idle = «Entrar», sending = «Entrando…») es «Entrando…».
fun Submission.buttonText(idle: UiText, sending: UiText): UiText = when (this) {
    Submission.Sending -> sending
    is Submission.Failed -> UiText.of(R.string.action_retry)
    Submission.Idle, Submission.Succeeded -> idle
}
