package es.ulpgc.pamn.dominio.data.publication

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.ulpgc.pamn.dominio.course.publication.CourseDraftPreview
import es.ulpgc.pamn.dominio.course.publication.MAX_COURSE_BYTES
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// Almacenamiento de ficheros JSON de borradores con ContentResolver real de Android
// (docs/tareas/subida-cursos.md, subpaso 3a-1). Usa una carpeta temporal en cacheDir.
@RunWith(AndroidJUnit4::class)
class CourseDraftFilesAndroidTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val tempDir = File(context.cacheDir, "course-draft-files-test")
    private val sourceDir = File(context.cacheDir, "course-draft-sources-test")
    private val storage = CourseDraftFiles.android(context.contentResolver, tempDir)

    @Before
    fun setUp() {
        tempDir.deleteRecursively()
        sourceDir.deleteRecursively()
        sourceDir.mkdirs()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
        sourceDir.deleteRecursively()
    }

    @Test
    fun imports_valid_json_file_and_reads_its_bytes() = runBlocking {
        val bytes = """{"title":"Cálculo I","modules":[{"id":"m1"},{"id":"m2"},{"id":"m3"}]}""".toByteArray()
        val source = File(sourceDir, "calculo.json").apply { writeBytes(bytes) }

        val result = storage.import("draft-1", Uri.fromFile(source).toString())

        assertEquals(
            DraftImportResult.Imported(
                filePath = "course_drafts/draft-1.json",
                preview = CourseDraftPreview(title = "Cálculo I", moduleCount = 3),
            ),
            result,
        )
        assertArrayEquals(bytes, storage.read("course_drafts/draft-1.json"))
    }

    @Test
    fun imports_json_without_title_using_file_name_as_fallback_title() = runBlocking {
        val bytes = """{"modules":[{"id":"m1"}]}""".toByteArray()
        val source = File(sourceDir, "introduccion-redes.json").apply { writeBytes(bytes) }

        val result = storage.import("draft-no-title", Uri.fromFile(source).toString())

        assertEquals(
            DraftImportResult.Imported(
                filePath = "course_drafts/draft-no-title.json",
                preview = CourseDraftPreview(title = "introduccion-redes", moduleCount = 1),
            ),
            result,
        )
    }

    @Test
    fun deletes_imported_draft_file() = runBlocking {
        val source = File(sourceDir, "curso.json").apply { writeText("""{"title":"Sistemas"}""") }
        val imported = storage.import("draft-del", Uri.fromFile(source).toString()) as DraftImportResult.Imported

        storage.delete(imported.filePath)

        assertNull(storage.read(imported.filePath))
        assertFalse(File(tempDir, imported.filePath).exists())
    }

    @Test
    fun rejects_file_larger_than_max_course_bytes_and_cleans_up() = runBlocking {
        val source = File(sourceDir, "grande.json").apply {
            writeBytes(ByteArray(MAX_COURSE_BYTES + 1) { 'a'.code.toByte() })
        }

        val result = storage.import("draft-big", Uri.fromFile(source).toString())

        assertEquals(DraftImportResult.TooLarge, result)
        assertFalse(File(tempDir, "course_drafts/draft-big.json").exists())
    }

    @Test
    fun returns_unreadable_for_missing_uri() = runBlocking {
        val missing = File(sourceDir, "no-existe.json")

        val result = storage.import("draft-missing", Uri.fromFile(missing).toString())

        assertEquals(DraftImportResult.Unreadable, result)
        assertTrue(File(tempDir, "course_drafts").listFiles().isNullOrEmpty())
    }
}
