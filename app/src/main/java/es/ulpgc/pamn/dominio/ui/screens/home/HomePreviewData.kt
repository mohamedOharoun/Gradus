package es.ulpgc.pamn.dominio.ui.screens.home

import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Publication
import es.ulpgc.pamn.dominio.course.progress.Enrollment
import es.ulpgc.pamn.dominio.course.progress.MasteredModule
import es.ulpgc.pamn.dominio.home.EnrolledCourse
import es.ulpgc.pamn.dominio.home.HomeCourse
import es.ulpgc.pamn.dominio.home.HomeSummary
import es.ulpgc.pamn.dominio.home.homeSummary
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

// Los cursos de ejemplo del Figma «Inicio», solo para las previews. Pasan por homeSummary (la lógica de
// verdad): el porcentaje, el siguiente módulo y el orden no se escriben a mano.
internal object HomePreviewData {

    private val start = Instant.parse("2026-10-01T10:00:00Z")

    // Un curso de `size` módulos en fila, publicado por `author`, con los `mastered` primeros dominados.
    private fun enrolled(id: String, title: String, author: String, size: Int, mastered: Int, daysAgo: Int): EnrolledCourse {
        val course = Course(
            id = id,
            title = title,
            modules = (1..size).map { Module("m$it", "Módulo $it", requires = if (it == 1) emptyList() else listOf("m${it - 1}")) },
            publication = Publication(Author("a-$id", author), version = 1, publishedAt = "2026-09-01T10:00:00Z"),
        )
        val at = start + (30 - daysAgo).days
        return EnrolledCourse(course, Enrollment(id, start), (1..mastered).map { MasteredModule("m$it", at) })
    }

    val summary: HomeSummary = homeSummary(
        listOf(
            enrolled("estructuras", "Estructuras de datos", "Ana Ruiz", size = 20, mastered = 12, daysAgo = 0),
            enrolled("kotlin", "Kotlin desde cero", "Equipo PAMN", size = 20, mastered = 7, daysAgo = 1),
            enrolled("algebra", "Álgebra lineal", "Luis Pérez", size = 20, mastered = 16, daysAgo = 2),
            enrolled("redes", "Redes de computadores", "Marta Gil", size = 18, mastered = 2, daysAgo = 3),
        ),
    )

    // «Tus cursos» del Figma: los tres de abajo (sin el de «Continúa»).
    val courses: List<HomeCourse> = summary.otherCourses
}
