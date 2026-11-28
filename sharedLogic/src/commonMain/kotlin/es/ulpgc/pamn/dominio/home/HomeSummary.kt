package es.ulpgc.pamn.dominio.home

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.ModuleState
import es.ulpgc.pamn.dominio.course.learningOrder
import es.ulpgc.pamn.dominio.course.masteredCount
import es.ulpgc.pamn.dominio.course.progress.Enrollment
import es.ulpgc.pamn.dominio.course.progress.MasteredModule
import es.ulpgc.pamn.dominio.course.stateOf
import kotlin.time.Instant

// Lo que enseña la pantalla Inicio (docs/tareas/inicio.md, paso 1): «Continúa donde lo dejaste» y
// «Tus cursos». Funciones puras: entran los cursos inscritos y lo dominado, sale lo que hay que pintar.

// Un curso en el que está inscrito el estudiante, con su inscripción y lo que ha dominado.
data class EnrolledCourse(val course: Course, val enrollment: Enrollment, val mastered: List<MasteredModule>)

// Una fila de «Tus cursos» (y la tarjeta de «Continúa»): cuánto se lleva y qué toca después.
data class HomeCourse(
    val course: Course,
    val masteredCount: Int,
    val total: Int,
    // De 0 a 100, redondeado hacia abajo (decisión E).
    val percent: Int,
    // El primer módulo desbloqueado sin dominar, en el orden del camino; null si no queda ninguno.
    val next: Module?,
    // Lo más reciente entre la inscripción y el último módulo dominado (decisión D).
    val lastActivity: Instant,
) {
    val isFinished: Boolean get() = total > 0 && masteredCount == total
}

// `continueWith`: el curso más reciente que aún tiene algo que estudiar; null si no hay ninguno.
// `courses`: todos los inscritos, en orden.
data class HomeSummary(val continueWith: HomeCourse?, val courses: List<HomeCourse>) {
    // «Tus cursos»: todos menos el de «Continúa», que ya sale arriba (decisión J, como el Figma).
    val otherCourses: List<HomeCourse> get() = courses.filterNot { it == continueWith }
}

fun homeSummary(enrolled: List<EnrolledCourse>): HomeSummary {
    val courses = enrolled.map { it.toHomeCourse() }.sortedWith(byActivity)
    return HomeSummary(continueWith = courses.firstOrNull { it.next != null }, courses = courses)
}

// Más reciente primero; con la misma actividad, por título (sin tildes ni mayúsculas) y por id, para que
// el orden sea siempre el mismo.
private val byActivity: Comparator<HomeCourse> =
    compareByDescending<HomeCourse> { it.lastActivity }
        .thenBy { it.course.title.sortKey() }
        .thenBy { it.course.id }

private fun EnrolledCourse.toHomeCourse(): HomeCourse {
    // Decisión F: masteredCount solo cuenta los módulos que siguen existiendo en el curso.
    val ids = mastered.map { it.moduleId }.toSet()
    val done = course.masteredCount(ids)
    val total = course.modules.size
    return HomeCourse(
        course = course,
        masteredCount = done,
        total = total,
        percent = if (total == 0) 0 else done * 100 / total,
        next = course.learningOrder().firstOrNull { course.stateOf(it.id, ids) == ModuleState.FRONTIER },
        lastActivity = (mastered.map { it.masteredAt } + enrollment.enrolledAt).max(),
    )
}

// «Álgebra» junto a «algoritmos» y antes de «Kotlin». En commonMain no hay Collator (es de Java): se
// quitan a mano las tildes y diéresis del español y se pasa a minúsculas. La «ñ» se queda como está.
private fun String.sortKey(): String = lowercase().map { ACCENTS[it] ?: it }.joinToString("")

private val ACCENTS: Map<Char, Char> = mapOf(
    'á' to 'a', 'à' to 'a', 'ä' to 'a', 'â' to 'a',
    'é' to 'e', 'è' to 'e', 'ë' to 'e', 'ê' to 'e',
    'í' to 'i', 'ì' to 'i', 'ï' to 'i', 'î' to 'i',
    'ó' to 'o', 'ò' to 'o', 'ö' to 'o', 'ô' to 'o',
    'ú' to 'u', 'ù' to 'u', 'ü' to 'u', 'û' to 'u',
)
