package es.ulpgc.pamn.dominio.ui.screens.notes

import es.ulpgc.pamn.dominio.data.notes.PhotoFiles
import es.ulpgc.pamn.dominio.notes.InMemoryNotesStore
import es.ulpgc.pamn.dominio.notes.Note
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// «Mis notas» de un módulo (docs/tareas/notas-camara.md, tarea 4): las fotos guardadas y añadir una
// «De la galería». Tiempo simulado: nada corre hasta runCurrent().
@OptIn(ExperimentalCoroutinesApi::class)
class ModuleNotesViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Copia «de mentira»: devuelve la ruta que se le diga (null = no se pudo copiar) y recuerda qué copió.
    private class FakePhotoFiles(var answer: String? = "notes/copia.jpg") : PhotoFiles {
        val imported = mutableListOf<String>()
        var waitFor: CompletableDeferred<Unit>? = null
        override suspend fun import(source: String): String? {
            imported += source
            waitFor?.await()
            return answer
        }
        override suspend fun delete(path: String) {}
        override fun newCapture(): String = "captura.jpg"
        override suspend fun keepCapture(capture: String): String? = answer
        override suspend fun discardCapture(capture: String) {}
    }

    private val notes = InMemoryNotesStore()
    private val photos = FakePhotoFiles()

    private fun TestScope.opened(): ModuleNotesViewModel {
        val viewModel = ModuleNotesViewModel(
            courseId = "g", moduleId = "bfs", moduleTitle = "BFS",
            notes = notes, photos = photos, clock = { 5_000L }, newId = { "id-nueva" },
        )
        runCurrent()
        return viewModel
    }

    private fun note(id: String, at: Long, module: String = "bfs") = Note(id, "g", module, "notes/$id.jpg", null, at)

    // ── Lo que enseña ──

    @Test
    fun `it shows the title of the module`() = runTest {
        assertEquals("BFS", opened().state.moduleTitle)
    }

    @Test
    fun `it shows the notes of its module, the newest first`() = runTest {
        notes.add(note("vieja", at = 1_000))
        notes.add(note("nueva", at = 2_000))
        notes.add(note("otra", at = 3_000, module = "dfs"))

        assertEquals(listOf("nueva", "vieja"), opened().state.notes.map { it.id })
    }

    @Test
    fun `without notes the list is empty`() = runTest {
        assertEquals(emptyList<Note>(), opened().state.notes)
    }

    // ── De la galería ──

    @Test
    fun `a picked photo is copied and saved as a note of the module`() = runTest {
        val viewModel = opened()

        viewModel.onPhotoPicked("content://media/picker/1")
        runCurrent()

        assertEquals(listOf("content://media/picker/1"), photos.imported)
        assertEquals(
            listOf(Note("id-nueva", "g", "bfs", "notes/copia.jpg", caption = null, createdAt = 5_000)),
            notes.notes("g", "bfs").first(),
        )
        assertEquals(1, viewModel.state.notes.size)
    }

    @Test
    fun `while copying the state says so`() = runTest {
        val viewModel = opened()
        photos.waitFor = CompletableDeferred()

        viewModel.onPhotoPicked("content://media/picker/1")
        runCurrent()
        assertTrue(viewModel.state.adding)

        photos.waitFor?.complete(Unit)
        runCurrent()
        assertFalse(viewModel.state.adding)
    }

    @Test
    fun `if it cannot be copied it says so and saves nothing`() = runTest {
        photos.answer = null
        val viewModel = opened()

        viewModel.onPhotoPicked("content://media/picker/1")
        runCurrent()

        assertTrue(viewModel.state.addFailed)
        assertFalse(viewModel.state.adding)
        assertEquals(emptyList<Note>(), notes.notes("g", "bfs").first())
    }

    // Lo vuelve a intentar con otra foto: el aviso anterior ya no aplica.
    @Test
    fun `picking another photo clears the failure`() = runTest {
        photos.answer = null
        val viewModel = opened()
        viewModel.onPhotoPicked("content://media/picker/1")
        runCurrent()

        photos.answer = "notes/copia.jpg"
        viewModel.onPhotoPicked("content://media/picker/2")
        runCurrent()

        assertFalse(viewModel.state.addFailed)
    }

    // Mientras se copia una no se empieza otra (dos toques rápidos en la galería).
    @Test
    fun `while copying another pick is ignored`() = runTest {
        val viewModel = opened()
        photos.waitFor = CompletableDeferred()

        viewModel.onPhotoPicked("content://media/picker/1")
        runCurrent()
        viewModel.onPhotoPicked("content://media/picker/2")
        photos.waitFor?.complete(Unit)
        runCurrent()

        assertEquals(listOf("content://media/picker/1"), photos.imported)
    }

    @Test
    fun `at first nothing is being added and nothing failed`() = runTest {
        val state = opened().state
        assertFalse(state.adding)
        assertFalse(state.addFailed)
        assertNull(state.notes.firstOrNull())
    }
}
