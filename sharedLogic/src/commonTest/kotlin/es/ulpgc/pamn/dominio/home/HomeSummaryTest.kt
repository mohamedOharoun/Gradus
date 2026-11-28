package es.ulpgc.pamn.dominio.home

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.progress.Enrollment
import es.ulpgc.pamn.dominio.course.progress.MasteredModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

// Lo que enseña la pantalla Inicio (docs/tareas/inicio.md, paso 1): «Continúa donde lo dejaste» y
// «Tus cursos», calculados a partir de los cursos inscritos y lo dominado. Función pura, sin reloj.
class HomeSummaryTest {

    private val day1: Instant = Instant.parse("2026-11-20T10:00:00Z")

    // Conjuntos → Relaciones → (Grafos, Árboles); Grafos → BFS (el mismo grafo que ModuleStateTest).
    private val structures = Course(
        id = "ed",
        title = "Estructuras de datos",
        modules = listOf(
            Module("conjuntos", "Conjuntos"),
            Module("relaciones", "Relaciones", requires = listOf("conjuntos")),
            Module("grafos", "Grafos", requires = listOf("relaciones")),
            Module("arboles", "Árboles", requires = listOf("relaciones")),
            Module("bfs", "BFS", requires = listOf("grafos")),
        ),
    )

    // Un curso de `size` módulos en fila (m1 → m2 → …), para los porcentajes.
    private fun chain(id: String, title: String = id, size: Int): Course = Course(
        id = id,
        title = title,
        modules = (1..size).map { Module("m$it", "Módulo $it", requires = if (it == 1) emptyList() else listOf("m${it - 1}")) },
    )

    private fun enrolled(course: Course, at: Instant = day1, mastered: Map<String, Instant> = emptyMap()) = EnrolledCourse(
        course = course,
        enrollment = Enrollment(course.id, at),
        mastered = mastered.map { (id, time) -> MasteredModule(id, time) },
    )

    // Los `count` primeros módulos de una cadena, dominados en `at`.
    private fun firstOf(count: Int, at: Instant = day1): Map<String, Instant> = (1..count).associate { "m$it" to at }

    private fun only(course: EnrolledCourse): HomeCourse = homeSummary(listOf(course)).courses.single()

    // ── Sin cursos ──

    @Test
    fun `without courses there is nothing to continue and no list`() {
        val summary = homeSummary(emptyList())
        assertNull(summary.continueWith)
        assertTrue(summary.courses.isEmpty())
    }

    // ── Cuánto se lleva ──

    @Test
    fun `a course counts its mastered modules out of all`() {
        val course = only(enrolled(chain("k", size = 20), mastered = firstOf(7)))
        assertEquals(7, course.masteredCount)
        assertEquals(20, course.total)
        assertEquals(35, course.percent)
    }

    // Decisión E: hacia abajo, para que un curso no diga 100 % sin estar terminado.
    @Test
    fun `the percent rounds down`() {
        assertEquals(66, only(enrolled(chain("c", size = 3), mastered = firstOf(2))).percent)
        assertEquals(33, only(enrolled(chain("c", size = 3), mastered = firstOf(1))).percent)
        assertEquals(99, only(enrolled(chain("c", size = 200), mastered = firstOf(199))).percent)
    }

    @Test
    fun `a course just started is at zero percent`() {
        assertEquals(0, only(enrolled(chain("c", size = 4))).percent)
    }

    // Decisión F: si el autor quitó un módulo que ya estaba dominado, no cuenta.
    @Test
    fun `mastered modules that no longer exist do not count`() {
        val course = only(enrolled(chain("c", size = 4), mastered = firstOf(1) + ("borrado" to day1)))
        assertEquals(1, course.masteredCount)
        assertEquals(25, course.percent)
    }

    // ── El siguiente módulo ──

    @Test
    fun `with nothing mastered the next module is the first one`() {
        assertEquals("conjuntos", only(enrolled(structures)).next?.id)
    }

    // El siguiente es uno desbloqueado: aunque Árboles venga antes en la lista del autor, si falta
    // Relaciones no se puede estudiar.
    @Test
    fun `the next module is unlocked and not mastered`() {
        val course = only(enrolled(structures, mastered = mapOf("conjuntos" to day1)))
        assertEquals("relaciones", course.next?.id)
    }

    // Con dos desbloqueados a la vez, el primero en el orden del camino (el de «Qué vas a aprender»).
    @Test
    fun `between two unlocked modules the next follows the path order`() {
        val course = only(enrolled(structures, mastered = mapOf("conjuntos" to day1, "relaciones" to day1)))
        assertEquals("grafos", course.next?.id)
    }

    @Test
    fun `a finished course has no next module and is at one hundred percent`() {
        val course = only(enrolled(chain("c", size = 3), mastered = firstOf(3)))
        assertNull(course.next)
        assertEquals(100, course.percent)
        assertTrue(course.isFinished)
    }

    // Decisión E: sin módulos no hay nada que estudiar, pero tampoco está «terminado».
    @Test
    fun `a course without modules is zero of zero and has no next`() {
        val course = only(enrolled(Course("vacio", "Vacío", modules = emptyList())))
        assertEquals(0, course.total)
        assertEquals(0, course.percent)
        assertNull(course.next)
    }

    // ── El orden de «Tus cursos» (decisión D) ──

    @Test
    fun `the most recently enrolled course goes first`() {
        val older = enrolled(chain("viejo", size = 2), at = day1)
        val newer = enrolled(chain("nuevo", size = 2), at = day1 + 2.days)
        assertEquals(listOf("nuevo", "viejo"), homeSummary(listOf(older, newer)).courses.map { it.course.id })
    }

    // Dominar un módulo cuenta como actividad: el curso sube aunque se inscribiera antes.
    @Test
    fun `mastering a module moves its course up`() {
        val studied = enrolled(chain("estudiado", size = 3), at = day1, mastered = firstOf(1, at = day1 + 5.days))
        val enrolledLater = enrolled(chain("reciente", size = 3), at = day1 + 1.days)
        val summary = homeSummary(listOf(enrolledLater, studied))
        assertEquals(listOf("estudiado", "reciente"), summary.courses.map { it.course.id })
        assertEquals(day1 + 5.days, summary.courses.first().lastActivity)
    }

    // La actividad es la fecha más reciente, no la del primer módulo dominado.
    @Test
    fun `the last activity is the latest mastered module`() {
        val course = only(enrolled(chain("c", size = 3), mastered = mapOf("m1" to day1 + 1.hours, "m2" to day1 + 3.hours)))
        assertEquals(day1 + 3.hours, course.lastActivity)
    }

    @Test
    fun `with the same activity courses go by title`() {
        val b = enrolled(chain("1", title = "Redes", size = 2))
        val a = enrolled(chain("2", title = "Álgebra", size = 2))
        val k = enrolled(chain("3", title = "Kotlin", size = 2))
        assertEquals(listOf("Álgebra", "Kotlin", "Redes"), homeSummary(listOf(b, a, k)).courses.map { it.course.title })
    }

    // Mismo título y misma fecha: por id, para que el orden no cambie de una vez a otra.
    @Test
    fun `with the same activity and title courses go by id`() {
        val second = enrolled(chain("b", title = "Grafos", size = 2))
        val first = enrolled(chain("a", title = "Grafos", size = 2))
        assertEquals(listOf("a", "b"), homeSummary(listOf(second, first)).courses.map { it.course.id })
    }

    // ── «Continúa donde lo dejaste» ──

    @Test
    fun `continue with the most recent course`() {
        val older = enrolled(chain("viejo", size = 2), at = day1)
        val newer = enrolled(chain("nuevo", size = 2), at = day1 + 1.days)
        assertEquals("nuevo", homeSummary(listOf(older, newer)).continueWith?.course?.id)
    }

    // Un curso terminado no se «continúa»: se salta al siguiente más reciente, pero sigue en la lista.
    @Test
    fun `a finished course is skipped but stays in the list`() {
        val finished = enrolled(chain("terminado", size = 2), mastered = firstOf(2, at = day1 + 3.days))
        val ongoing = enrolled(chain("en-curso", size = 2), at = day1 + 1.days)
        val summary = homeSummary(listOf(finished, ongoing))
        assertEquals("en-curso", summary.continueWith?.course?.id)
        assertEquals(listOf("terminado", "en-curso"), summary.courses.map { it.course.id })
    }

    @Test
    fun `with every course finished there is nothing to continue`() {
        val summary = homeSummary(listOf(enrolled(chain("c", size = 2), mastered = firstOf(2))))
        assertNull(summary.continueWith)
        assertEquals(1, summary.courses.size)
    }

    @Test
    fun `a course without modules is not offered to continue`() {
        val summary = homeSummary(listOf(enrolled(Course("vacio", "Vacío", modules = emptyList()), at = day1 + 1.days), enrolled(chain("c", size = 2))))
        assertEquals("c", summary.continueWith?.course?.id)
    }

    // ── «Tus cursos» sin el de «Continúa» (decisión J, como el Figma) ──

    @Test
    fun `the other courses leave out the one to continue`() {
        val older = enrolled(chain("viejo", size = 2), at = day1)
        val newer = enrolled(chain("nuevo", size = 2), at = day1 + 1.days)
        val summary = homeSummary(listOf(older, newer))
        assertEquals(listOf("viejo"), summary.otherCourses.map { it.course.id })
    }

    // Con un solo curso, sale en «Continúa» y no queda ninguno más.
    @Test
    fun `with a single course there are no other courses`() {
        assertTrue(homeSummary(listOf(enrolled(chain("c", size = 2)))).otherCourses.isEmpty())
    }

    // Sin nada que continuar (todos terminados), «Tus cursos» los tiene todos.
    @Test
    fun `with nothing to continue the other courses are all of them`() {
        val summary = homeSummary(listOf(enrolled(chain("c", size = 2), mastered = firstOf(2))))
        assertEquals(listOf("c"), summary.otherCourses.map { it.course.id })
    }

    // «courses» sigue teniendo todos: Inicio lo usa para saber si hay alguno.
    @Test
    fun `all courses are still there`() {
        val summary = homeSummary(listOf(enrolled(chain("c", size = 2))))
        assertEquals(listOf("c"), summary.courses.map { it.course.id })
    }
}

