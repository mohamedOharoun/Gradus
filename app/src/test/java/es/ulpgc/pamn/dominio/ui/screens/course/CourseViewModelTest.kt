package es.ulpgc.pamn.dominio.ui.screens.course

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.FakeCourseGateway
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.progress.InMemoryProgressStore
import es.ulpgc.pamn.dominio.notes.InMemoryNotesStore
import es.ulpgc.pamn.dominio.notes.Note
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

// El camino de un curso y la hoja de cada módulo (docs/tareas/notas-camara.md, tarea 3, decisión S):
// con inscripción, tocar un módulo desbloqueado abre su hoja (Estudiar / Repasar y «Mis notas · N»)
// en lugar de entrar a estudiar. Tiempo simulado: nada corre hasta runCurrent().
@OptIn(ExperimentalCoroutinesApi::class)
class CourseViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // repr → bfs → dfs: al principio, repr es la frontera y bfs y dfs están bloqueados.
    private val graphs = Course(
        "g", "Grafos",
        listOf(Module("repr", "Representar"), Module("bfs", "BFS", requires = listOf("repr")), Module("dfs", "DFS", requires = listOf("bfs"))),
    )
    private lateinit var progress: InMemoryProgressStore
    private val notes = InMemoryNotesStore()

    private fun note(id: String, module: String) = Note(id, "g", module, "notes/$id.jpg", null, 1_000)

    private suspend fun enrolledStore(): InMemoryProgressStore =
        loggedInStore().also { it.enroll("g") }

    private suspend fun TestScope.opened(store: InMemoryProgressStore? = null): CourseViewModel {
        progress = store ?: enrolledStore()
        val viewModel = CourseViewModel("g", FakeCourseGateway(listOf(graphs)), progress, progress, notes)
        runCurrent()
        return viewModel
    }

    private val CourseViewModel.ui: CourseUiState get() = (state as CourseScreenState.Ready).ui

    // ── Tocar un módulo ──

    @Test
    fun `touching a locked module says what is missing and opens nothing`() = runTest {
        val viewModel = opened()

        viewModel.onModuleClick("bfs")

        assertTrue(viewModel.ui.notice is CourseNotice.Locked)
        assertNull(viewModel.ui.sheet)
        assertNull(viewModel.ui.practiceToOpen)
    }

    // Decisión S: ya no entra directamente a estudiar.
    @Test
    fun `touching a frontier module opens its sheet`() = runTest {
        val viewModel = opened()

        viewModel.onModuleClick("repr")

        assertEquals("repr", viewModel.ui.sheet)
        assertNull(viewModel.ui.practiceToOpen)
    }

    @Test
    fun `touching a mastered module opens its sheet too`() = runTest {
        val store = enrolledStore().also { it.markMastered("g", "repr") }
        val viewModel = opened(store)

        viewModel.onModuleClick("repr")

        assertEquals("repr", viewModel.ui.sheet)
    }

    @Test
    fun `touching a module that does not exist does nothing`() = runTest {
        val viewModel = opened()
        val before = viewModel.ui

        viewModel.onModuleClick("nope")

        assertEquals(before, viewModel.ui)
    }

    // ── Desde la hoja ──

    @Test
    fun `study closes the sheet and opens the practice of its module`() = runTest {
        val viewModel = opened()
        viewModel.onModuleClick("repr")

        viewModel.onStudy()

        assertNull(viewModel.ui.sheet)
        assertEquals("repr", viewModel.ui.practiceToOpen)
    }

    @Test
    fun `my notes closes the sheet and opens the notes of its module`() = runTest {
        val viewModel = opened()
        viewModel.onModuleClick("repr")

        viewModel.onOpenNotes()

        assertNull(viewModel.ui.sheet)
        assertEquals("repr", viewModel.ui.notesToOpen)
    }

    // Eventos: el NavHost navega y los borra, para no volver a abrir la pantalla al girar el móvil.
    @Test
    fun `once opened the events are cleared`() = runTest {
        val viewModel = opened()
        viewModel.onModuleClick("repr")
        viewModel.onOpenNotes()

        viewModel.onNotesOpened()

        assertNull(viewModel.ui.notesToOpen)
    }

    @Test
    fun `dismissing the sheet only closes it`() = runTest {
        val viewModel = opened()
        viewModel.onModuleClick("repr")

        viewModel.onSheetDismiss()

        assertNull(viewModel.ui.sheet)
        assertNull(viewModel.ui.practiceToOpen)
        assertNull(viewModel.ui.notesToOpen)
    }

    // Sin hoja abierta (p. ej. dos toques rápidos) no se abre nada.
    @Test
    fun `without a sheet study and notes do nothing`() = runTest {
        val viewModel = opened()

        viewModel.onStudy()
        viewModel.onOpenNotes()

        assertNull(viewModel.ui.practiceToOpen)
        assertNull(viewModel.ui.notesToOpen)
    }

    // ── Cuántas notas tiene cada módulo («Mis notas · N») ──

    @Test
    fun `it knows how many notes each module has`() = runTest {
        notes.add(note("n1", "repr"))
        notes.add(note("n2", "repr"))
        val viewModel = opened()

        assertEquals(mapOf("repr" to 2), viewModel.ui.noteCounts)
    }

    // Al guardar una foto y volver, «Mis notas · N» ya está al día.
    @Test
    fun `the counts update by themselves`() = runTest {
        val viewModel = opened()

        notes.add(note("n1", "repr"))
        runCurrent()

        assertEquals(mapOf("repr" to 1), viewModel.ui.noteCounts)
    }

    // El progreso sigue llegando igual que antes, y no cierra la hoja abierta.
    @Test
    fun `mastering a module updates the path and keeps the sheet`() = runTest {
        val viewModel = opened()
        viewModel.onModuleClick("repr")

        progress.markMastered("g", "repr")
        runCurrent()

        assertEquals(setOf("repr"), viewModel.ui.mastered)
        assertEquals("repr", viewModel.ui.sheet)
    }
}
