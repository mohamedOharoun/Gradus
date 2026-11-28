package es.ulpgc.pamn.dominio.course

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Referencia de comportamiento del puerto CourseCopyStore (en memoria).
class InMemoryCourseCopyStoreTest {

    @Test
    fun `loading an unknown course returns null`() = runTest {
        val store = InMemoryCourseCopyStore()

        assertNull(store.load("desconocido"))
    }

    @Test
    fun `saving a copy allows loading it back`() = runTest {
        val store = InMemoryCourseCopyStore()

        store.save("c1", """{"id":"c1","content":{}}""")

        assertEquals("""{"id":"c1","content":{}}""", store.load("c1"))
    }

    @Test
    fun `saving again for the same course overwrites the previous copy`() = runTest {
        val store = InMemoryCourseCopyStore()

        store.save("c1", """{"id":"c1","version":1}""")
        store.save("c1", """{"id":"c1","version":2}""")

        assertEquals("""{"id":"c1","version":2}""", store.load("c1"))
    }

    @Test
    fun `copies of different courses are stored independently`() = runTest {
        val store = InMemoryCourseCopyStore()

        store.save("c1", "copia-1")
        store.save("c2", "copia-2")
        store.save("c1", "copia-1-nueva")

        assertEquals("copia-1-nueva", store.load("c1"))
        assertEquals("copia-2", store.load("c2"))
        assertNull(store.load("c3"))
    }
}
