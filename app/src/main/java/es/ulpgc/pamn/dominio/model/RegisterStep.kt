package es.ulpgc.pamn.dominio.model

// Pasos del registro, en orden. El orden de declaración es el orden de la pantalla.
enum class RegisterStep {
    NAME,
    EMAIL,
    PASSWORD;

    val next: RegisterStep? get() = entries.getOrNull(ordinal + 1)
    val previous: RegisterStep? get() = entries.getOrNull(ordinal - 1)
}
