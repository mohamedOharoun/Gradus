package es.ulpgc.pamn.dominio.data.submission

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import es.ulpgc.pamn.dominio.submission.PhotoScaling
import es.ulpgc.pamn.dominio.submission.PhotoSubmission
import es.ulpgc.pamn.dominio.submission.PixelSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID

// Adaptador con Bitmap y ficheros de Android, sin librerías de imágenes. `folder` es la carpeta de las fotos
// preparadas y `captures`, la de la cámara (las dos en la caché: Android puede vaciarla, y son temporales).
// Todo va en Dispatchers.IO.
// Cuánto reducir lo deciden las funciones puras de PhotoScaling (sharedLogic).
class AndroidSubmissionPhotos(
    private val resolver: ContentResolver,
    private val folder: File,
    private val captures: File,
) : SubmissionPhotos {

    override suspend fun prepare(source: String): String? = withContext(Dispatchers.IO) {
        val original = readSource(source) ?: return@withContext null
        val upright = decodeUpright(original) ?: return@withContext null
        val target = File(folder, "${UUID.randomUUID()}.jpg")
        try {
            folder.mkdirs()
            target.writeBytes(compressToFit(upright))
            target.path
        } catch (e: IOException) {
            target.delete() // no dejar un fichero a medias
            null
        } finally {
            upright.recycle()
        }
    }

    override suspend fun prepareCapture(capture: String): String? {
        val path = prepare(Uri.fromFile(File(capture)).toString())
        discardCapture(capture)
        return path
    }

    override suspend fun discardCapture(capture: String) = withContext(Dispatchers.IO) {
        val file = File(capture)
        if (file.isIn(captures)) file.delete()
        Unit
    }

    override suspend fun read(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.isIn(folder) && file.exists()) file.readBytes() else null
    }

    override suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.isIn(folder)) file.delete()
        Unit
    }

    // canonicalFile resuelve «..»: una ruta rara no puede salir de la carpeta.
    private fun File.isIn(directory: File) = canonicalFile.parentFile == directory.canonicalFile

    // La foto entera en memoria (una de móvil ocupa unos pocos MB): así se lee dos veces (tamaño y EXIF)
    // sin volver a abrir la URI, que puede no permitirlo.
    private fun readSource(source: String): ByteArray? = try {
        resolver.openInputStream(Uri.parse(source))?.use { it.readBytes() }
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null // la URI ya no da permiso (p. ej. caducó)
    }

    // La foto derecha y ya con su tamaño final; null si no es una imagen.
    private fun decodeUpright(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val size = PixelSize(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply { inSampleSize = PhotoScaling.sampleSize(size) }
        val sampled = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        // El tamaño final se calcula sobre el original: girarla no cambia su lado mayor.
        val final = PhotoScaling.fit(size)
        val matrix = Matrix().apply {
            postScale(final.width.toFloat() / sampled.width, final.height.toFloat() / sampled.height)
            postRotate(rotationOf(bytes))
        }
        val result = Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, matrix, true)
        if (result !== sampled) sampled.recycle()
        return result
    }

    // Muchas cámaras guardan la foto tumbada y apuntan en su EXIF cuánto girarla. BitmapFactory no lo mira.
    private fun rotationOf(bytes: ByteArray): Float = try {
        when (ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } catch (e: IOException) {
        0f // sin EXIF legible (un PNG, por ejemplo): tal cual
    }

    // JPEG con la mejor calidad que quepa en lo que acepta el servidor. A 2000 px casi siempre basta la
    // primera; una foto muy «ruidosa» puede necesitar bajarla. Si ni la última cabe, se entrega esa y el
    // gateway la rechaza como demasiado grande sin enviarla.
    private fun compressToFit(bitmap: Bitmap): ByteArray {
        var bytes = ByteArray(0)
        for (quality in QUALITIES) {
            bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
            if (bytes.size <= PhotoSubmission.MAX_BYTES) break
        }
        return bytes
    }

    private companion object {
        val QUALITIES = listOf(85, 70, 55, 40)
    }
}
