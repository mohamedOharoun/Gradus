package es.ulpgc.pamn.dominio.course.publication

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class InMemoryCourseDraftContentStoreTest {

    @Test
    fun `read returns null when file is missing`() = runTest {
        val store = InMemoryCourseDraftContentStore()
        assertNull(store.read("course_drafts/missing.json"))
    }

    @Test
    fun `read returns initial and put bytes with defensive copies`() = runTest {
        val initialBytes = byteArrayOf(1, 2, 3)
        val store = InMemoryCourseDraftContentStore(mapOf("a.json" to initialBytes))
        initialBytes[0] = 99

        val putBytes = byteArrayOf(4, 5, 6)
        store.put("b.json", putBytes)
        putBytes[0] = 88

        val readA = store.read("a.json")
        assertContentEquals(byteArrayOf(1, 2, 3), readA)
        readA!![0] = 77
        assertContentEquals(byteArrayOf(1, 2, 3), store.read("a.json"))

        val readB = store.read("b.json")
        assertContentEquals(byteArrayOf(4, 5, 6), readB)
        readB!![0] = 66
        assertContentEquals(byteArrayOf(4, 5, 6), store.read("b.json"))
    }

    @Test
    fun `delete removes stored bytes and is idempotent when missing`() = runTest {
        val store = InMemoryCourseDraftContentStore(mapOf("a.json" to byteArrayOf(1, 2)))

        store.delete("a.json")
        store.delete("a.json")
        store.delete("missing.json")

        assertNull(store.read("a.json"))
    }
}
