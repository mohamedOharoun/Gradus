package es.ulpgc.pamn.dominio.course.search

// Un filtro que el usuario tiene puesto: es lo que se dibuja como chip quitable («#grafos ✕»,
// «por Ana ✕», «Último año ✕») y lo que cuenta el distintivo «Filtros 3». El texto de la búsqueda
// NO es un filtro activo.
//
// Es una sealed interface: el compilador obliga a que cada `when` trate todos los casos, así que si
// se añade otro filtro avisará de cada sitio que haya que completar.
sealed interface ActiveFilter {
    data class Tag(val name: String) : ActiveFilter

    // El nombre va con los espacios ordenados, como se ve en el chip («por Ana Ruiz»).
    data class Author(val name: String) : ActiveFilter

    // Nunca es `ANY`: «Cualquiera» no filtra, así que no tiene chip.
    data class Published(val within: PublishedWithin) : ActiveFilter
}
