package es.ulpgc.pamn.dominio.ui.components

import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.Validation

// Adaptador entre la lógica y la interfaz: model/ dice qué error hay; aquí se decide cómo se dice.
// Cuando traduzcamos la app, estos textos pasarán a strings.xml.
fun FieldError.toMessage(): String = when (this) {
    FieldError.EMPTY -> "Este campo es obligatorio"
    FieldError.TOO_SHORT -> "Escribe al menos ${Validation.MIN_NAME_LENGTH} caracteres"
    FieldError.TOO_LONG -> "Es demasiado largo"
    FieldError.INVALID_EMAIL -> "Escribe un correo válido, como nombre@ejemplo.com"
    FieldError.WEAK_PASSWORD -> "La contraseña no cumple los requisitos"
    FieldError.PASSWORDS_DONT_MATCH -> "Las contraseñas no coinciden"
    FieldError.EMAIL_TAKEN -> "Ya existe una cuenta con este correo. ¿Quieres iniciar sesión?"
    FieldError.TERMS_NOT_ACCEPTED -> "Acepta los términos para crear la cuenta"
}

// Por qué no se ha podido enviar un formulario. Los datos se conservan: basta con reintentar.
fun FailureReason.toMessage(): String = when (this) {
    FailureReason.UNREACHABLE -> "No hay conexión con el servidor. Comprueba tu red e inténtalo de nuevo."
    FailureReason.SERVER_FAILURE -> "Algo ha fallado en el servidor. Inténtalo de nuevo en unos minutos."
}
