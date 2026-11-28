package es.ulpgc.pamn.dominio.data.notes

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// Las fotos de las notas como ficheros (docs/tareas/notas-camara.md, decisión W): copiar una a la carpeta
// de la app y borrarla. Usa una carpeta de prueba propia: no toca las notas de la app.
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class AndroidPhotoFilesTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val base = File(context.cacheDir, "photo-files-test")
    private val photos = AndroidPhotoFiles(context.contentResolver, base, captures = File(base, "captures"))
    private val bytes = ByteArray(5_000) { (it % 251).toByte() }

    private lateinit var source: File

    @Before
    fun createSource() {
        base.deleteRecursively()
        source = File(context.cacheDir, "foto-de-prueba.jpg").apply { writeBytes(bytes) }
    }

    @After
    fun clean() {
        base.deleteRecursively()
        source.delete()
    }

    @Test
    fun a_photo_is_copied_into_the_notes_folder() = runBlocking {
        val path = photos.import(Uri.fromFile(source).toString())!!

        assertTrue(path.startsWith("notes/"))
        assertArrayEquals(bytes, photos.fileOf(path).readBytes())
    }

    // Cada copia es un fichero nuevo: dos fotos iguales no se pisan.
    @Test
    fun each_copy_is_a_new_file() = runBlocking {
        val first = photos.import(Uri.fromFile(source).toString())
        val second = photos.import(Uri.fromFile(source).toString())
        assertNotEquals(first, second)
    }

    @Test
    fun a_photo_that_cannot_be_read_gives_nothing_and_leaves_nothing() = runBlocking {
        assertNull(photos.import(Uri.fromFile(File(context.cacheDir, "no-existe.jpg")).toString()))
        assertEquals(0, File(base, "notes").listFiles()?.size ?: 0)
    }

    @Test
    fun deleting_removes_the_file() = runBlocking {
        val path = photos.import(Uri.fromFile(source).toString())!!

        photos.delete(path)

        assertFalse(photos.fileOf(path).exists())
    }

    // ── Fotos de la cámara (tarea 5): se hacen en un fichero temporal y, al guardar, pasan a notas ──

    @Test
    fun a_capture_goes_to_a_new_temporary_file() {
        val first = photos.newCapture()
        val second = photos.newCapture()
        assertNotEquals(first, second)
        assertTrue(File(first).parentFile!!.exists())
    }

    @Test
    fun keeping_a_capture_moves_it_into_the_notes_folder() = runBlocking {
        val capture = photos.newCapture()
        File(capture).writeBytes(bytes)

        val path = photos.keepCapture(capture)!!

        assertTrue(path.startsWith("notes/"))
        assertArrayEquals(bytes, photos.fileOf(path).readBytes())
        assertFalse(File(capture).exists())
    }

    @Test
    fun keeping_a_capture_that_is_not_there_gives_nothing() = runBlocking {
        assertNull(photos.keepCapture(photos.newCapture()))
    }

    // «Repetir»: la foto temporal se tira.
    @Test
    fun discarding_a_capture_removes_it() = runBlocking {
        val capture = photos.newCapture()
        File(capture).writeBytes(bytes)

        photos.discardCapture(capture)

        assertFalse(File(capture).exists())
    }

    @Test
    fun discarding_never_removes_anything_outside_the_captures_folder() = runBlocking {
        photos.discardCapture(source.path)
        assertTrue(source.exists())
    }

    // Solo se borra dentro de la carpeta de notas: una ruta rara de la base de datos no borra otra cosa.
    @Test
    fun nothing_outside_the_notes_folder_is_deleted() = runBlocking {
        val outside = File(base, "importante.txt").apply { parentFile?.mkdirs(); writeText("no tocar") }

        photos.delete("notes/../importante.txt")

        assertTrue(outside.exists())
    }
}
