package es.ulpgc.pamn.dominio.course.search

// Lo que se sabe del número de cursos que daría el borrador de la hoja de filtros («Mostrar N cursos»).
// Es un tipo suma: la hoja no puede estar a la vez «con número» y «sin saberlo».
sealed interface DraftCount {
    data class Known(val total: Int) : DraftCount

    // Esperando los 300 ms o la respuesta: todavía no hay número.
    data object Counting : DraftCount

    // Falló el recuento: no hay número y no se reintenta solo.
    data object Unknown : DraftCount
}
