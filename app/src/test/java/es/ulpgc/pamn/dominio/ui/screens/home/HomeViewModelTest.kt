package es.ulpgc.pamn.dominio.ui.screens.home

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.CourseOutcome
import es.ulpgc.pamn.dominio.course.FakeCourseGateway
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.course.CourseGateway
import es.ulpgc.pamn.dominio.course.search.CourseQuery
import es.ulpgc.pamn.dominio.course.SearchOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

// La pantalla Inicio (docs/tareas/inicio.md, paso 3): junta la sesión, las inscripciones y cada curso.
// Tiempo simulado: nada corre hasta runCurrent().
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class TestClock(var time: Instant = Instant.parse("2026-11-28T10:00:00Z")) : Clock {
        override fun now(): Instant = time
    }

    private val graphs = Course("grafos", "Grafos", listOf(Module("repr", "Representar"), Module("bfs", "BFS", requires = listOf("repr"))))
    private val kotlin = Course("kotlin", "Kotlin desde cero", listOf(Module("vars", "Variables")))

    private val sessions = InMemorySessionStore()
    private val clock = TestClock()
    private val store = InMemoryProgressStore(sessions, clock)

    private suspend fun logInAs(name: String) = sessions.save(
        Session("t", "2099-01-01T00:00:00Z", Account("ana", name, "ana@ulpgc.es", "2026-01-01T00:00:00Z")),
    )

    private fun TestScope.opened(gateway: FakeCourseGateway = FakeCourseGateway(listOf(graphs, kotlin))): HomeViewModel {
        val viewModel = HomeViewModel(sessions, store, gateway)
        runCurrent()
        return viewModel
    }

    private val HomeViewModel.ui: HomeUiState get() = (state as HomeScreenState.Ready).ui

    // ── Al abrir ──

    @Test
    fun it_is_loading_until_it_knows_the_courses() = runTest {
        logInAs("Ana Ruiz")
        val viewModel = HomeViewModel(sessions, store, FakeCourseGateway())
        assertEquals(HomeScreenState.Loading, viewModel.state)
    }

    @Test
    fun the_header_greets_with_the_first_name_and_shows_the_initials() = runTest {
        logInAs("Ana Ruiz")
        val ui = opened().ui
        assertEquals("Ana", ui.name)
        assertEquals("AR", ui.initials)
    }

    @Test
    fun without_enrollments_there_are_no_courses() = runTest {
        logInAs("Ana Ruiz")
        val ui = opened().ui
        assertTrue(ui.summary.courses.isEmpty())
        assertNull(ui.summary.continueWith)
    }

    // Sin sesión (no debería pasar en Inicio), no se rompe: sin nombre y sin cursos.
    @Test
    fun without_a_session_there_is_no_name_and_no_courses() = runTest {
        val ui = opened().ui
        assertNull(ui.name)
        assertTrue(ui.summary.courses.isEmpty())
    }

    // ── Los cursos ──

    @Test
    fun the_enrolled_courses_are_shown_most_recent_first() = runTest {
        logInAs("Ana Ruiz")
        store.enroll("grafos")
        clock.time += 1.hours
        store.enroll("kotlin")

        val ui = opened().ui

        assertEquals(listOf("kotlin", "grafos"), ui.summary.courses.map { it.course.id })
        assertEquals("kotlin", ui.summary.continueWith?.course?.id)
    }

    // Al volver de estudiar, Inicio ya lo sabe: escucha el almacén.
    @Test
    fun mastering_a_module_updates_its_course() = runTest {
        logInAs("Ana Ruiz")
        store.enroll("grafos")
        val viewModel = opened()

        store.markMastered("grafos", "repr")
        runCurrent()

        val course = viewModel.ui.summary.courses.single()
        assertEquals(50, course.percent)
        assertEquals("bfs", course.next?.id)
    }

    @Test
    fun enrolling_in_a_course_adds_it() = runTest {
        logInAs("Ana Ruiz")
        val viewModel = opened()

        store.enroll("kotlin")
        runCurrent()

        assertEquals(listOf("kotlin"), viewModel.ui.summary.courses.map { it.course.id })
    }

    // Un cambio de progreso no vuelve a descargar los cursos que ya tiene.
    @Test
    fun a_course_is_downloaded_only_once() = runTest {
        logInAs("Ana Ruiz")
        store.enroll("grafos")
        val gateway = FakeCourseGateway(listOf(graphs))
        opened(gateway)

        store.markMastered("grafos", "repr")
        runCurrent()

        assertEquals(listOf("grafos"), gateway.requestedCourses)
    }

    // ── Cursos que no se pueden cargar ──

    // Un curso que no se puede descargar (y sin copia) se queda fuera; los demás se ven.
    @Test
    fun a_course_that_cannot_be_loaded_is_left_out_and_counted() = runTest {
        logInAs("Ana Ruiz")
        store.enroll("grafos")
        store.enroll("kotlin")
        val gateway = FakeCourseGateway(courseAnswer = { id -> if (id == "grafos") CourseOutcome.Found(graphs) else CourseOutcome.Unreachable })

        val ui = opened(gateway).ui

        assertEquals(listOf("grafos"), ui.summary.courses.map { it.course.id })
        assertEquals(1, ui.unavailable)
    }

    // El que falló se vuelve a pedir en el siguiente cambio (puede que ya haya red).
    @Test
    fun a_course_that_failed_is_asked_again_on_the_next_change() = runTest {
        logInAs("Ana Ruiz")
        store.enroll("grafos")
        var online = false
        val byId = mapOf("grafos" to graphs, "kotlin" to kotlin)
        val gateway = FakeCourseGateway(courseAnswer = { id -> if (online) CourseOutcome.Found(byId.getValue(id)) else CourseOutcome.Unreachable })
        val viewModel = opened(gateway)
        assertEquals(1, viewModel.ui.unavailable)

        online = true
        store.enroll("kotlin")
        runCurrent()

        assertEquals(setOf("grafos", "kotlin"), viewModel.ui.summary.courses.map { it.course.id }.toSet())
        assertEquals(0, viewModel.ui.unavailable)
        assertEquals(listOf("grafos", "grafos", "kotlin"), gateway.requestedCourses.sorted())
    }

    // ── Descargas lentas ──

    // Un gateway que no responde hasta que el test lo dice (complete): así se ve qué pasa mientras tanto.
    private class SlowGateway : CourseGateway {
        val requested = mutableListOf<String>()
        private val answers = mutableMapOf<String, CompletableDeferred<CourseOutcome>>()
        override suspend fun search(query: CourseQuery, page: Int): SearchOutcome = SearchOutcome.Unreachable
        override suspend fun course(id: String): CourseOutcome {
            requested += id
            return answers.getOrPut(id) { CompletableDeferred() }.await()
        }
        fun complete(course: Course) = answers.getOrPut(course.id) { CompletableDeferred() }.complete(CourseOutcome.Found(course))
    }

    // Se piden todos a la vez: el segundo se pide antes de que responda el primero.
    @Test
    fun the_courses_are_downloaded_at_the_same_time() = runTest {
        logInAs("Ana Ruiz")
        store.enroll("grafos")
        store.enroll("kotlin")
        val gateway = SlowGateway()
        val viewModel = HomeViewModel(sessions, store, gateway)
        runCurrent()

        assertEquals(setOf("grafos", "kotlin"), gateway.requested.toSet())
        assertEquals(HomeScreenState.Loading, viewModel.state)

        gateway.complete(graphs)
        gateway.complete(kotlin)
        runCurrent()
        assertEquals(2, viewModel.ui.summary.courses.size)
    }

    // Un cambio en mitad de una descarga: se deja esa y se empieza con la lista nueva (collectLatest), así
    // que se pinta lo último y no una lista vieja.
    @Test
    fun a_change_while_downloading_shows_the_latest_list() = runTest {
        logInAs("Ana Ruiz")
        store.enroll("grafos")
        val gateway = SlowGateway()
        val viewModel = HomeViewModel(sessions, store, gateway)
        runCurrent()

        store.enroll("kotlin")
        runCurrent()
        gateway.complete(graphs)
        gateway.complete(kotlin)
        runCurrent()

        assertEquals(setOf("grafos", "kotlin"), viewModel.ui.summary.courses.map { it.course.id }.toSet())
        assertEquals(0, viewModel.ui.unavailable)
    }

    // ── Sin cursos (paso 4d) ──

    // El aviso «Todavía no estás en ningún curso» solo sale si de verdad no hay ninguno.
    @Test
    fun without_enrollments_the_home_is_empty() = runTest {
        logInAs("Ana Ruiz")
        assertTrue(opened().ui.isEmpty)
    }

    @Test
    fun with_a_course_the_home_is_not_empty() = runTest {
        logInAs("Ana Ruiz")
        store.enroll("grafos")
        assertEquals(false, opened().ui.isEmpty)
    }

    // Inscrito en cursos que no se pueden cargar (sin red): no es «sin cursos», es «no se han podido cargar».
    @Test
    fun courses_that_could_not_be_loaded_are_not_an_empty_home() = runTest {
        logInAs("Ana Ruiz")
        store.enroll("grafos")
        val ui = opened(FakeCourseGateway(courseAnswer = { CourseOutcome.Unreachable })).ui
        assertEquals(false, ui.isEmpty)
        assertEquals(1, ui.unavailable)
    }

    // ── El saludo al volver a Inicio (p. ej. de «Editar perfil») ──

    // Editar perfil guarda la sesión con el nombre nuevo; al volver, Inicio la relee (onResume).
    @Test
    fun coming_back_after_a_name_change_greets_with_the_new_name() = runTest {
        logInAs("Ana Ruiz")
        val viewModel = opened()

        logInAs("Beatriz Gil")
        viewModel.onResume()
        runCurrent()

        assertEquals("Beatriz", viewModel.ui.name)
        assertEquals("BG", viewModel.ui.initials)
    }

    // Releer la sesión no toca los cursos ni los vuelve a descargar.
    @Test
    fun coming_back_keeps_the_courses_without_downloading_them_again() = runTest {
        logInAs("Ana Ruiz")
        store.enroll("grafos")
        val gateway = FakeCourseGateway(listOf(graphs))
        val viewModel = opened(gateway)

        viewModel.onResume()
        runCurrent()

        assertEquals(listOf("grafos"), viewModel.ui.summary.courses.map { it.course.id })
        assertEquals(listOf("grafos"), gateway.requestedCourses)
    }

    // Lo siguiente que llegue (dominar un módulo) usa ya el nombre nuevo, no el del principio.
    @Test
    fun the_new_name_stays_after_a_progress_change() = runTest {
        logInAs("Ana Ruiz")
        store.enroll("grafos")
        val viewModel = opened()
        logInAs("Beatriz Gil")
        viewModel.onResume()
        runCurrent()

        store.markMastered("grafos", "repr")
        runCurrent()

        assertEquals("Beatriz", viewModel.ui.name)
        assertEquals(50, viewModel.ui.summary.courses.single().percent)
    }

    // Si vuelve antes de tener los cursos, no se rompe: al llegar salen ya con el nombre nuevo.
    @Test
    fun coming_back_while_loading_uses_the_new_name_when_ready() = runTest {
        logInAs("Ana Ruiz")
        store.enroll("grafos")
        val gateway = SlowGateway()
        val viewModel = HomeViewModel(sessions, store, gateway)
        runCurrent()

        logInAs("Beatriz Gil")
        viewModel.onResume()
        runCurrent()
        assertEquals(HomeScreenState.Loading, viewModel.state)

        gateway.complete(graphs)
        runCurrent()
        assertEquals("Beatriz", viewModel.ui.name)
    }
}

