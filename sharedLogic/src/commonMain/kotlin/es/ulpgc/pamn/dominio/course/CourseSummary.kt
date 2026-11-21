package es.ulpgc.pamn.dominio.course

data class Author(val id: String, val name: String)

// Los datos de una versión publicada de un curso, sin su contenido: lo que llega en cada resultado
// de la búsqueda (backend/docs/api-cursos.md, «Metadatos de una versión»).
data class CourseSummary(
    val id: String,
    val version: Int,
    val author: Author,
    val publishedAt: String,
    val title: String,
    val description: String,
    val language: String,
    val tags: List<String>,
    val moduleCount: Int,
)

// Una página de resultados de la búsqueda. `page` empieza en 0; `total` cuenta todos los resultados.
data class CoursePage(
    val items: List<CourseSummary>,
    val page: Int,
    val size: Int,
    val total: Int,
) {
    // ¿Quedan resultados después de esta página? (Para el botón «Cargar más».)
    val hasMore: Boolean get() = (page + 1) * size < total
}
