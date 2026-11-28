package es.ulpgc.pamn.dominio.course.progress

// Lo que el servidor no guardó de una sincronización (`skipped` del contrato). Es solo informativo:
// el móvil lo conserva y lo quita de su cola igualmente (B-8). `code` es texto porque la app no
// depende de los códigos (COURSE_NOT_FOUND, REQUIRED…); `courseId` va tal como llegó.
sealed interface SkippedProgress {
    val courseId: String
    val code: String

    // Sin `moduleId` en el contrato: se omitió la inscripción entera con sus módulos.
    data class WholeEnrollment(override val courseId: String, override val code: String) : SkippedProgress

    // Con `moduleId` (tal cual, también «» o « »): se omitió solo ese módulo.
    data class SingleModule(
        override val courseId: String,
        val moduleId: String,
        override val code: String,
    ) : SkippedProgress
}
