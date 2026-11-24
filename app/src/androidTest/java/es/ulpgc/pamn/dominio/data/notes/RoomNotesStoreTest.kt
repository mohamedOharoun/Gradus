package es.ulpgc.pamn.dominio.data.notes

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.ulpgc.pamn.dominio.notes.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

// Las notas guardadas con Room (docs/tareas/notas-camara.md, tarea 2). Las mismas reglas que
// InMemoryNotesStoreTest, ahora con SQLite de verdad. Base de datos en memoria: cada test empieza vacío
// y no toca la de la app. Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class RoomNotesStoreTest {

    private lateinit var database: GradusDatabase
    private lateinit var store: RoomNotesStore

    @Before
    fun open() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, GradusDatabase::class.java).build()
        store = RoomNotesStore(database.notes())
    }

    @After
    fun close() {
        database.close()
    }

    private fun note(id: String, module: String = "bfs", at: Long = 1_000, course: String = "grafos", caption: String? = null) =
        Note(id = id, courseId = course, moduleId = module, file = "notes/$id.jpg", caption = caption, createdAt = at)

    @Test
    fun at_first_a_module_has_no_notes() = runBlocking {
        assertEquals(emptyList<Note>(), store.notes("grafos", "bfs").first())
    }

    // Todos los campos vuelven igual, también el pie y la fecha.
    @Test
    fun an_added_note_comes_back_whole() = runBlocking {
        val n = note("n1", caption = "Dijkstra paso a paso", at = 1_795_460_244_000)
        store.add(n)
        assertEquals(listOf(n), store.notes("grafos", "bfs").first())
    }

    @Test
    fun a_note_without_caption_comes_back_without_caption() = runBlocking {
        store.add(note("n1", caption = null))
        assertNull(store.notes("grafos", "bfs").first().single().caption)
    }

    @Test
    fun the_newest_note_goes_first() = runBlocking {
        val old = note("n1", at = 1_000)
        val new = note("n2", at = 2_000)
        store.add(old)
        store.add(new)
        assertEquals(listOf(new, old), store.notes("grafos", "bfs").first())
    }

    // La misma regla que newestFirst en sharedLogic: a igual fecha, por id de mayor a menor.
    @Test
    fun notes_taken_at_the_same_moment_keep_a_fixed_order() = runBlocking {
        val a = note("a", at = 1_000)
        val b = note("b", at = 1_000)
        store.add(a)
        store.add(b)
        assertEquals(listOf(b, a), store.notes("grafos", "bfs").first())
    }

    @Test
    fun notes_of_other_modules_and_courses_are_not_mixed() = runBlocking {
        val mine = note("n1")
        store.add(mine)
        store.add(note("n2", module = "dfs"))
        store.add(note("n3", course = "redes"))
        assertEquals(listOf(mine), store.notes("grafos", "bfs").first())
    }

    // Room avisa a quien escucha cuando cambia la tabla: «Mis notas · N» se actualiza solo.
    @Test
    fun who_listens_gets_each_change() = runBlocking {
        val n = note("n1")
        val seen = CopyOnWriteArrayList<List<Note>>()
        val listening = launch(Dispatchers.Default) { store.notes("grafos", "bfs").take(2).toList(seen) }
        // Se añade la nota cuando ya ha llegado la primera lista (vacía); si no, se perdería el «antes».
        withTimeout(5_000) { while (seen.isEmpty()) delay(10) }
        store.add(n)
        withTimeout(5_000) { listening.join() }
        assertEquals(listOf(emptyList(), listOf(n)), seen.toList())
    }

    @Test
    fun it_counts_the_notes_of_each_module_of_a_course() = runBlocking {
        store.add(note("n1", module = "bfs"))
        store.add(note("n2", module = "bfs"))
        store.add(note("n3", module = "dfs"))
        store.add(note("n4", course = "redes", module = "bfs"))
        assertEquals(mapOf("bfs" to 2, "dfs" to 1), store.counts("grafos").first())
    }

    @Test
    fun a_note_is_found_by_its_id() = runBlocking {
        val n = note("n1")
        store.add(n)
        assertEquals(n, store.find("n1"))
        assertNull(store.find("otra"))
    }

    @Test
    fun deleting_a_note_gives_it_back_and_it_is_gone() = runBlocking {
        val n = note("n1")
        store.add(n)
        assertEquals(n, store.delete("n1"))
        assertEquals(emptyList<Note>(), store.notes("grafos", "bfs").first())
        assertEquals(emptyMap<String, Int>(), store.counts("grafos").first())
    }

    @Test
    fun deleting_a_note_that_is_not_there_gives_nothing() = runBlocking {
        assertNull(store.delete("otra"))
    }
}
