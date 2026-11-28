package es.ulpgc.pamn.dominio.ui.components

// Lo que sabe el botón de inscribirse. Un solo valor (y no dos booleanos) para que no pueda haber
// combinaciones imposibles, como «inscrito e inscribiéndose».
sealed interface EnrollmentState {
    // Todavía no se sabe si el estudiante está inscrito (el almacén no ha respondido).
    data object Unknown : EnrollmentState
    data object NotEnrolled : EnrollmentState
    data object Enrolling : EnrollmentState
    data object Enrolled : EnrollmentState
}
