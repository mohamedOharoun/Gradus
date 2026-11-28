package es.ulpgc.pamn.dominio.ui.screens.notes

import es.ulpgc.pamn.dominio.data.notes.PhotoFiles
import es.ulpgc.pamn.dominio.notes.InMemoryNotesStore
import es.ulpgc.pamn.dominio.notes.Note
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
import org.junit.Before
import org.junit.Test

// Ver una nota y eliminarla (docs/tareas/notas-camara.md, tarea 6, boceto 4 y decisión Z).
// Tiempo simulado: nada corre hasta runCurrent().
@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewerViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakePhotoFiles : PhotoFiles {
        val deleted = mutableListOf<String>()
        override suspend fun import(source: String): String? = null
        override suspend fun delete(path: String) {
            deleted += path
        }
        override fun newCapture(): String = ""
        override suspend fun keepCapture(capture: String): String? = null
        override suspend fun discardCapture(capture: String) {}
    }

    private val note = Note("n1", "g", "bfs", "notes/n1.jpg", "Dijkstra", 1_000)
    private val notes = InMemoryNotesStore(listOf(note))
    private val photos = FakePhotoFiles()

    private fun TestScope.opened(id: String = "n1"): NoteViewerViewModel {
        val viewModel = NoteViewerViewModel(id, notes, photos)
        runCurrent()
        return viewModel
    }

    @Test
    fun `it starts loading`() {
        assertEquals(NoteViewerState.Loading, NoteViewerViewModel("n1", notes, photos).state)
    }

    @Test
    fun `it shows the note`() = runTest {
        assertEquals(NoteViewerState.Showing(note), opened().state)
    }

    // Borrada desde otro sitio (o un enlace viejo): no hay nada que enseñar, se vuelve atrás.
    @Test
    fun `a note that is not there is gone`() = runTest {
        assertEquals(NoteViewerState.Gone, opened("otra").state)
    }

    // ── Eliminar (decisión Z) ──

    @Test
    fun `delete first asks for confirmation`() = runTest {
        val viewModel = opened()
        viewModel.onDelete()
        assertEquals(NoteViewerState.Showing(note, confirmingDelete = true), viewModel.state)
    }

    @Test
    fun `cancelling keeps the note`() = runTest {
        val viewModel = opened()
        viewModel.onDelete()

        viewModel.onDeleteCancel()
        runCurrent()

        assertEquals(NoteViewerState.Showing(note), viewModel.state)
        assertEquals(listOf(note), notes.notes("g", "bfs").first())
    }

    @Test
    fun `confirming deletes the note and its photo and is gone`() = runTest {
        val viewModel = opened()
        viewModel.onDelete()

        viewModel.onDeleteConfirm()
        runCurrent()

        assertEquals(emptyList<Note>(), notes.notes("g", "bfs").first())
        assertEquals(listOf("notes/n1.jpg"), photos.deleted)
        assertEquals(NoteViewerState.Gone, viewModel.state)
    }

    // Sin pasar por la confirmación no se borra nada.
    @Test
    fun `confirming without asking first does nothing`() = runTest {
        val viewModel = opened()

        viewModel.onDeleteConfirm()
        runCurrent()

        assertEquals(listOf(note), notes.notes("g", "bfs").first())
    }

    @Test
    fun `confirming twice deletes once`() = runTest {
        val viewModel = opened()
        viewModel.onDelete()

        viewModel.onDeleteConfirm()
        viewModel.onDeleteConfirm()
        runCurrent()

        assertEquals(listOf("notes/n1.jpg"), photos.deleted)
    }

    @Test
    fun `while deleting the state says so`() = runTest {
        val viewModel = opened()
        viewModel.onDelete()

        viewModel.onDeleteConfirm()

        assertEquals(NoteViewerState.Showing(note, confirmingDelete = true, deleting = true), viewModel.state)
    }
}
