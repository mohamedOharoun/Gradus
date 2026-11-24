package es.ulpgc.pamn.dominio.data.notes

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

// Las fotos de las notas como ficheros (docs/tareas/notas-camara.md, decisión W). Puerto pequeño para que
// los ViewModel se prueben en el ordenador con uno falso, sin ficheros de verdad.
// Las rutas son relativas («notes/1a2b….jpg»): es lo que se guarda en la base de datos.
interface PhotoFiles {

    // Copia la foto de `source` (una URI como texto: de la galería o de la cámara) a la carpeta de notas
    // y devuelve su ruta; null si no se pudo leer o escribir.
    suspend fun import(source: String): String?

    // Borra la foto. Si no existe, o la ruta sale de la carpeta de notas, no hace nada.
    suspend fun delete(path: String)

    // ── Fotos de la cámara (tarea 5) ──
    // La cámara escribe en un fichero temporal; al «Guardar» pasa a la carpeta de notas y al «Repetir»
    // se tira. Así una foto que no se quiere nunca llega a las notas.

    // Ruta absoluta de un fichero temporal nuevo donde la cámara puede escribir la foto.
    fun newCapture(): String

    // Pasa la foto temporal a la carpeta de notas y devuelve su ruta guardada; null si no existe o falla.
    suspend fun keepCapture(capture: String): String?

    // Tira la foto temporal («Repetir» o salir sin guardar). Solo borra dentro de la carpeta temporal.
    suspend fun discardCapture(capture: String)
}

// Adaptador con ficheros de Android. `base` es la carpeta privada de la app (`filesDir`) y `captures`, la de
// las fotos temporales de la cámara (en la caché); en los tests, carpetas de prueba. Todo el trabajo con
// ficheros va en Dispatchers.IO: nunca en el hilo de la pantalla.
class AndroidPhotoFiles(
    private val resolver: ContentResolver,
    private val base: File,
    private val captures: File,
) : PhotoFiles {

    private val folder = File(base, FOLDER)

    // El fichero de una ruta guardada (para enseñar la foto).
    fun fileOf(path: String): File = File(base, path)

    override suspend fun import(source: String): String? = withContext(Dispatchers.IO) {
        val path = "$FOLDER/${UUID.randomUUID()}.jpg"
        val target = fileOf(path)
        try {
            folder.mkdirs()
            val input = resolver.openInputStream(Uri.parse(source)) ?: return@withContext null
            input.use { from -> target.outputStream().use { to -> from.copyTo(to) } }
            path
        } catch (e: IOException) {
            target.delete() // no dejar una copia a medias
            null
        } catch (e: SecurityException) {
            target.delete() // la URI ya no da permiso (p. ej. caducó)
            null
        }
    }

    override suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        val file = fileOf(path).canonicalFile
        // canonicalFile resuelve «..»: así una ruta rara no puede borrar nada fuera de la carpeta de notas.
        if (file.parentFile == folder.canonicalFile) file.delete()
        Unit
    }

    override fun newCapture(): String {
        captures.mkdirs()
        return File(captures, "${UUID.randomUUID()}.jpg").path
    }

    override suspend fun keepCapture(capture: String): String? = withContext(Dispatchers.IO) {
        val from = File(capture)
        if (!from.exists()) return@withContext null
        val path = "$FOLDER/${UUID.randomUUID()}.jpg"
        val target = fileOf(path)
        try {
            folder.mkdirs()
            // Copiar y borrar (y no mover): la caché y la carpeta de la app pueden estar en sitios distintos.
            from.copyTo(target)
            from.delete()
            path
        } catch (e: IOException) {
            target.delete()
            null
        }
    }

    override suspend fun discardCapture(capture: String) = withContext(Dispatchers.IO) {
        val file = File(capture).canonicalFile
        if (file.parentFile == captures.canonicalFile) file.delete()
        Unit
    }

    private companion object {
        const val FOLDER = "notes"
    }
}
