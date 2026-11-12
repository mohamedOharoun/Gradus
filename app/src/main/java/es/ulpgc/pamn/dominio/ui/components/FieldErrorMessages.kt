package es.ulpgc.pamn.dominio.ui.components

import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.Validation

// Adaptador entre la lógica y la interfaz: model/ dice qué error hay; aquí se decide cómo se dice.
// Cuando traduzcamos la app, estos textos pasarán a strings.xml.
fun FieldError.toMessage(): String = when (this) {
    FieldError.EMPTY -> "Este campo es obligatorio"
    FieldError.TOO_SHORT -> "Escribe al menos ${Validation.MIN_NAME_LENGTH} caracteres"
    FieldError.INVALID_EMAIL -> "Escribe un correo válido, como nombre@ejemplo.com"
    FieldError.WEAK_PASSWORD -> "La contraseña no cumple los requisitos"
    FieldError.PASSWORDS_DONT_MATCH -> "Las contraseñas no coinciden"
}
