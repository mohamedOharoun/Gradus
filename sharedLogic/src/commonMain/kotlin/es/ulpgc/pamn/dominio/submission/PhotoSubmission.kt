package es.ulpgc.pamn.dominio.submission

// Lo que se entrega: la foto (JPEG) y a qué ejercicio de qué versión del curso va (docs/tareas/ejercicio-foto.md,
// decisión M: la versión que estudia el alumno). Clase normal y no data class: un ByteArray se compara por
// identidad, así que equals, hashCode y toString se escriben a mano (como Jpeg en el backend).
class PhotoSubmission(
    val courseId: String,
    val courseVersion: Int,
    val exerciseId: String,
    jpeg: ByteArray,
) {
    // Copia al entrar: cambiar el array de fuera no cambia la entrega.
    private val bytes = jpeg.copyOf()

    // Copia al salir, por lo mismo.
    val jpeg: ByteArray get() = bytes.copyOf()

    val size: Int get() = bytes.size

    // Más que esto, el servidor no la acepta (413). Se mira antes de enviarla: ver HttpSubmissionGateway.
    val isTooLarge: Boolean get() = size > MAX_BYTES

    init {
        require(courseId.isNotBlank()) { "Falta el curso de la entrega." }
        require(courseVersion >= 1) { "La versión del curso empieza en 1." }
        require(exerciseId.isNotBlank()) { "Falta el ejercicio de la entrega." }
        // Si es un JPEG lo decide el servidor (por sus primeros bytes); aquí, que haya algo que enviar.
        require(bytes.isNotEmpty()) { "No hay foto que entregar." }
    }

    override fun equals(other: Any?): Boolean =
        other is PhotoSubmission && courseId == other.courseId && courseVersion == other.courseVersion &&
            exerciseId == other.exerciseId && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int =
        listOf(courseId, courseVersion, exerciseId, bytes.contentHashCode()).hashCode()

    companion object {
        // El mismo máximo que el servidor (Jpeg.MAX_BYTES en el backend, decisión F). La app reduce la
        // foto a 2000 px de lado antes de entregarla, así que normalmente queda muy por debajo.
        const val MAX_BYTES = 2_000_000
    }

    // Sin los bytes: una foto de 2 MB no debe acabar en los logs.
    override fun toString() =
        "PhotoSubmission(courseId=$courseId, courseVersion=$courseVersion, exerciseId=$exerciseId, jpeg=$size bytes)"
}
