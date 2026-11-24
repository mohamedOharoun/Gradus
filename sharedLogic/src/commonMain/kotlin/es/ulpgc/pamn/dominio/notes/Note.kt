package es.ulpgc.pamn.dominio.notes

import es.ulpgc.pamn.dominio.model.Validation

// Una foto de los apuntes, guardada junto a un módulo de un curso (docs/tareas/notas-camara.md).
// - `file`: la ruta de la foto dentro de la carpeta privada de la app (decisión W). En la base de
//   datos solo va esto; la foto es un fichero aparte.
// - `caption`: el pie opcional (decisión Y); null si no tiene.
// - `createdAt`: cuándo se hizo, en milisegundos desde el 1-1-1970 (UTC). Es lo que da el reloj de
//   cualquier plataforma; la app lo convierte en «24 nov».
data class Note(
    val id: String,
    val courseId: String,
    val moduleId: String,
    val file: String,
    val caption: String?,
    val createdAt: Long,
)

const val MAX_CAPTION_LENGTH = 100

// Cómo queda un pie escrito por el usuario.
sealed interface CaptionCheck {
    // Se puede guardar. null = sin pie (estaba vacío o solo tenía espacios).
    data class Valid(val caption: String?) : CaptionCheck

    // Más de MAX_CAPTION_LENGTH caracteres.
    data object TooLong : CaptionCheck
}

private val whitespace = Regex("\\s+")

// Espacios de sobra fuera (también saltos de línea: el pie es una línea). Se cuentan caracteres, no
// unidades de Java: un emoji es uno (Validation.lengthOf, como en el nombre del registro).
fun checkCaption(raw: String): CaptionCheck {
    val tidy = raw.trim().replace(whitespace, " ")
    return when {
        tidy.isEmpty() -> CaptionCheck.Valid(null)
        Validation.lengthOf(tidy) > MAX_CAPTION_LENGTH -> CaptionCheck.TooLong
        else -> CaptionCheck.Valid(tidy)
    }
}
