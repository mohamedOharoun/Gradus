package es.ulpgc.pamn.dominio.course.progress

// Qué pasó al pedir la baja de un curso con la sesión guardada: no había ninguna, o se preguntó
// al servidor y hubo un desenlace. Espejo de SessionSyncResult (decisión B-1).
sealed interface UnenrollResult {
    // No hay sesión guardada: no se llamó al servidor ni al almacén local.
    data object NoSession : UnenrollResult

    // Se pidió la baja con la sesión guardada; `outcome` es el desenlace de ProgressGateway.
    data class Finished(val outcome: UnenrollOutcome) : UnenrollResult
}
