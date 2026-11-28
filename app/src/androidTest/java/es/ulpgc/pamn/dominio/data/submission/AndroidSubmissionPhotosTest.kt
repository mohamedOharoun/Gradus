package es.ulpgc.pamn.dominio.data.submission

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.ulpgc.pamn.dominio.submission.PhotoSubmission
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
import kotlin.random.Random

// Preparar la foto de una entrega (docs/tareas/ejercicio-foto.md, paso 5a, decisión X): girarla según su
// EXIF, reducirla a 2000 px de lado y guardarla como JPEG en una carpeta temporal. Usa carpetas de prueba
// propias. Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class AndroidSubmissionPhotosTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val folder = File(context.cacheDir, "submission-photos-test")
    private val sources = File(context.cacheDir, "submission-photos-sources")
    private val captures = File(context.cacheDir, "submission-photos-captures")
    private val photos = AndroidSubmissionPhotos(context.contentResolver, folder, captures)

    @Before
    fun clean() {
        folder.deleteRecursively()
        sources.deleteRecursively()
        captures.deleteRecursively()
        sources.mkdirs()
        captures.mkdirs()
    }

    @After
    fun cleanAfter() = clean()

    // ── Ayudas: fotos de prueba ──

    private fun photo(
        name: String,
        width: Int,
        height: Int,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        noise: Boolean = false,
        into: File = sources,
    ): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (noise) {
            val random = Random(7)
            val row = IntArray(width)
            for (y in 0 until height) {
                for (x in 0 until width) row[x] = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
                bitmap.setPixels(row, 0, width, 0, y, width, 1)
            }
        } else {
            bitmap.eraseColor(Color.rgb(250, 248, 240))
        }
        val file = File(into, name)
        file.outputStream().use { bitmap.compress(format, 95, it) }
        bitmap.recycle()
        return file
    }

    private fun uriOf(file: File) = Uri.fromFile(file).toString()

    private fun sizeOf(path: String): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        return bounds.outWidth to bounds.outHeight
    }

    private fun isJpeg(path: String): Boolean {
        val head = File(path).readBytes().take(3)
        return head == listOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    }

    // ── Preparar ──

    @Test
    fun a_large_photo_is_reduced_to_2000_px_on_its_longest_side() = runBlocking {
        val path = photos.prepare(uriOf(photo("grande.jpg", 4000, 3000)))!!
        assertEquals(2000 to 1500, sizeOf(path))
        assertTrue(isJpeg(path))
    }

    @Test
    fun a_photo_that_already_fits_keeps_its_size() = runBlocking {
        assertEquals(800 to 600, sizeOf(photos.prepare(uriOf(photo("pequeña.jpg", 800, 600)))!!))
    }

    // Muchas cámaras guardan la foto tumbada y lo apuntan en el EXIF: se entrega ya derecha.
    @Test
    fun a_photo_lying_down_is_turned_upright() = runBlocking {
        val lying = photo("tumbada.jpg", 4000, 3000)
        ExifInterface(lying.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        assertEquals(1500 to 2000, sizeOf(photos.prepare(uriOf(lying))!!))
    }

    // De la galería puede llegar un PNG: se entrega como JPEG (el servidor solo acepta JPEG).
    @Test
    fun a_png_from_the_gallery_becomes_a_jpeg() = runBlocking {
        val path = photos.prepare(uriOf(photo("captura.png", 1200, 800, Bitmap.CompressFormat.PNG)))!!
        assertTrue(isJpeg(path))
        assertEquals(1200 to 800, sizeOf(path))
    }

    // Ruido: lo que peor comprime un JPEG. Aun así, cabe en lo que acepta el servidor.
    @Test
    fun even_the_hardest_photo_fits_in_what_the_server_accepts() = runBlocking {
        val path = photos.prepare(uriOf(photo("ruido.jpg", 4000, 3000, noise = true)))!!
        assertTrue(File(path).length() <= PhotoSubmission.MAX_BYTES)
    }

    @Test
    fun the_prepared_photo_is_a_new_file_in_its_folder_and_the_source_stays() = runBlocking {
        val source = photo("original.jpg", 800, 600)
        val first = photos.prepare(uriOf(source))!!
        val second = photos.prepare(uriOf(source))!!

        assertNotEquals(first, second)
        assertEquals(folder.canonicalFile, File(first).canonicalFile.parentFile)
        // La foto de la cámara la borra quien la hizo; aquí no se toca.
        assertTrue(source.exists())
    }

    @Test
    fun something_that_is_not_an_image_gives_nothing_and_leaves_nothing() = runBlocking {
        val text = File(sources, "nota.jpg").apply { writeText("no soy una foto") }
        assertNull(photos.prepare(uriOf(text)))
        assertNull(photos.prepare(uriOf(File(sources, "no-existe.jpg"))))
        assertEquals(0, folder.listFiles()?.size ?: 0)
    }

    // ── Fotos de la cámara ──

    private fun capture(width: Int, height: Int): File = photo("captura.jpg", width, height, into = captures)

    // Una vez preparada, la de la cámara ya no hace falta: se tira.
    @Test
    fun a_camera_photo_is_prepared_and_then_thrown_away() = runBlocking {
        val taken = capture(4000, 3000)
        val path = photos.prepareCapture(taken.path)!!
        assertEquals(2000 to 1500, sizeOf(path))
        assertFalse(taken.exists())
    }

    @Test
    fun a_camera_photo_that_is_not_used_is_thrown_away() = runBlocking {
        val taken = capture(800, 600)
        photos.discardCapture(taken.path)
        assertFalse(taken.exists())
    }

    // Solo tira dentro de la carpeta de la cámara.
    @Test
    fun only_camera_photos_are_thrown_away() = runBlocking {
        val elsewhere = photo("otra.jpg", 100, 100)
        photos.discardCapture(elsewhere.path)
        assertTrue(elsewhere.exists())
        assertTrue(photos.prepareCapture(elsewhere.path) != null)
        assertTrue(elsewhere.exists())
    }

    // ── Leer y borrar ──

    @Test
    fun the_prepared_photo_can_be_read_to_send_it() = runBlocking {
        val path = photos.prepare(uriOf(photo("leer.jpg", 800, 600)))!!
        assertArrayEquals(File(path).readBytes(), photos.read(path))
        assertNull(photos.read(File(folder, "no-existe.jpg").path))
    }

    @Test
    fun deleting_removes_the_prepared_photo() = runBlocking {
        val path = photos.prepare(uriOf(photo("borrar.jpg", 800, 600)))!!
        photos.delete(path)
        assertFalse(File(path).exists())
    }

    // Solo borra dentro de su carpeta: una ruta rara no puede borrar nada más.
    @Test
    fun deleting_outside_its_folder_does_nothing() = runBlocking {
        val outside = photo("fuera.jpg", 100, 100)
        photos.delete(outside.path)
        photos.delete(File(folder, "../submission-photos-sources/fuera.jpg").path)
        assertTrue(outside.exists())
    }
}
