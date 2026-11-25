package es.ulpgc.pamn.dominio.course.progress

// Si el estudiante puede estudiar el curso. Sin textos: la interfaz pone el mensaje.
sealed interface StudyAccess {
    data object Allowed : StudyAccess
    data object NotEnrolled : StudyAccess
}

// Función pura: solo depende de si hay inscripción (B-6), no del estado del módulo.
fun studyAccess(enrolled: Boolean): StudyAccess =
    if (enrolled) StudyAccess.Allowed else StudyAccess.NotEnrolled
