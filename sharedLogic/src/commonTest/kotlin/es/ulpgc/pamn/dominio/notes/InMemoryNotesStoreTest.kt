package es.ulpgc.pamn.dominio.notes

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Las notas guardadas (docs/tareas/notas-camara.md, tarea 1). Las mismas reglas valdrán para Room.
class InMemoryNotesStoreTest {

    private fun note(id: String, module: String = "bfs", at: Long = 1_000, course: String = "grafos", caption: String? = null) =
        Note(id = id, courseId = course, moduleId = module, file = "notes/$id.jpg", caption = caption, createdAt = at)

    private val store = InMemoryNotesStore()

    // ── Las de un módulo ──

    @Test
    fun `at first a module has no notes`() = runTest {
        assertEquals(emptyList(), store.notes("grafos", "bfs").first())
    }

    @Test
    fun `an added note is in its module`() = runTest {
        val n = note("n1")
        store.add(n)
        assertEquals(listOf(n), store.notes("grafos", "bfs").first())
    }

    // Boceto 2: la más reciente primero.
    @Test
    fun `the newest note goes first`() = runTest {
        val old = note("n1", at = 1_000)
        val new = note("n2", at = 2_000)
        store.add(old)
        store.add(new)
        assertEquals(listOf(new, old), store.notes("grafos", "bfs").first())
    }

    // Dos fotos en el mismo milisegundo: el orden no puede cambiar de una vez a otra.
    @Test
    fun `notes taken at the same moment keep a fixed order`() = runTest {
        val a = note("a", at = 1_000)
        val b = note("b", at = 1_000)
        store.add(a)
        store.add(b)
        assertEquals(listOf(b, a), store.notes("grafos", "bfs").first())
    }

    @Test
    fun `notes of other modules and courses are not mixed`() = runTest {
        val mine = note("n1")
        store.add(mine)
        store.add(note("n2", module = "dfs"))
        store.add(note("n3", course = "redes"))
        assertEquals(listOf(mine), store.notes("grafos", "bfs").first())
    }

    // El camino escucha: al guardar una foto, «Mis notas · N» se actualiza solo.
    @Test
    fun `who listens gets each change`() = runTest {
        val seen = mutableListOf<List<Note>>()
        val listening = launch { store.notes("grafos", "bfs").take(2).toList(seen) }
        testScheduler.runCurrent()
        val n = note("n1")
        store.add(n)
        listening.join()
        assertEquals(listOf(emptyList(), listOf(n)), seen)
    }

    // Para las previews: empezar con notas ya guardadas.
    @Test
    fun `it can start with saved notes`() = runTest {
        val n = note("n1")
        assertEquals(listOf(n), InMemoryNotesStore(listOf(n)).notes("grafos", "bfs").first())
    }

    // ── Cuántas hay por módulo (para «Mis notas · 3») ──

    @Test
    fun `it counts the notes of each module of a course`() = runTest {
        store.add(note("n1", module = "bfs"))
        store.add(note("n2", module = "bfs"))
        store.add(note("n3", module = "dfs"))
        store.add(note("n4", course = "redes", module = "bfs"))
        assertEquals(mapOf("bfs" to 2, "dfs" to 1), store.counts("grafos").first())
    }

    @Test
    fun `a module without notes is not in the counts`() = runTest {
        assertEquals(emptyMap(), store.counts("grafos").first())
    }

    // ── Buscar y eliminar ──

    @Test
    fun `a note is found by its id`() = runTest {
        val n = note("n1", caption = "Dijkstra")
        store.add(n)
        assertEquals(n, store.find("n1"))
        assertNull(store.find("otra"))
    }

    // Devuelve la nota borrada: quien llama borra su fichero (decisión Z).
    @Test
    fun `deleting a note gives it back and it is gone`() = runTest {
        val n = note("n1")
        store.add(n)
        assertEquals(n, store.delete("n1"))
        assertEquals(emptyList(), store.notes("grafos", "bfs").first())
        assertEquals(emptyMap(), store.counts("grafos").first())
    }

    @Test
    fun `deleting a note that is not there gives nothing`() = runTest {
        assertNull(store.delete("otra"))
    }
}
