package es.ulpgc.pamn.dominio.course.search

// Un filtro que el usuario tiene puesto: es lo que se dibuja como chip quitable («#grafos ✕»)
// y lo que cuenta el distintivo «Filtros 2». El texto de la búsqueda NO es un filtro activo.
//
// Es una sealed interface: en la fase 2 se añadirán el creador y la fecha, y el compilador
// avisará de cada `when` que haya que completar.
sealed interface ActiveFilter {
    data class Tag(val name: String) : ActiveFilter
}
