package es.ulpgc.pamn.dominio.data.publication

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import es.ulpgc.pamn.dominio.course.publication.CourseDraftPreview
import es.ulpgc.pamn.dominio.course.publication.MAX_COURSE_BYTES
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

// Adaptador de almacenamiento local de ficheros JSON de borradores (docs/tareas/subida-cursos.md,
// decisión D). Guarda los ficheros en `<baseDir>/course_drafts/<draftId>.json` con streaming acotado
// a MAX_COURSE_BYTES y protección frente a path traversal.
class CourseDraftFiles(
    private val baseDir: File,
    private val openStream: (String) -> InputStream?,
    private val resolveDisplayName: (String) -> String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CourseDraftStorage {

    private val folder = File(baseDir, FOLDER)

    override suspend fun import(draftId: String, sourceUri: String): DraftImportResult =
        withContext(ioDispatcher) {
            val target = resolveSafeTarget(draftId) ?: return@withContext DraftImportResult.Unreadable
            val relativePath = "$FOLDER/$draftId$JSON_EXTENSION"

            try {
                folder.mkdirs()
                val input = openStream(sourceUri) ?: run {
                    target.delete()
                    return@withContext DraftImportResult.Unreadable
                }
                val fitsInLimit = input.use { stream -> copyBounded(stream, target) }
                if (!fitsInLimit) {
                    target.delete()
                    return@withContext DraftImportResult.TooLarge
                }
                val displayName = runCatching { resolveDisplayName(sourceUri) }.getOrDefault("")
                val preview = CourseDraftPreview.fromBytes(target.readBytes(), displayName)
                DraftImportResult.Imported(filePath = relativePath, preview = preview)
            } catch (_: IOException) {
                target.delete()
                DraftImportResult.Unreadable
            } catch (_: SecurityException) {
                target.delete()
                DraftImportResult.Unreadable
            } catch (_: IllegalArgumentException) {
                target.delete()
                DraftImportResult.Unreadable
            }
        }

    override suspend fun read(filePath: String): ByteArray? = withContext(ioDispatcher) {
        val file = resolveSafeExisting(filePath) ?: return@withContext null
        if (!file.isFile || file.length() > MAX_COURSE_BYTES) return@withContext null
        try {
            file.readBytes()
        } catch (_: IOException) {
            null
        }
    }

    override suspend fun delete(filePath: String) = withContext(ioDispatcher) {
        deleteInternal(filePath)
    }

    override suspend fun deleteAll(filePaths: Iterable<String>) = withContext(ioDispatcher) {
        filePaths.forEach(::deleteInternal)
    }

    private fun deleteInternal(filePath: String) {
        val file = resolveSafeExisting(filePath) ?: return
        file.delete()
    }

    private fun copyBounded(input: InputStream, target: File): Boolean {
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        var totalBytes = 0L
        target.outputStream().use { output ->
            while (true) {
                val maxToRead = minOf(buffer.size.toLong(), MAX_COURSE_BYTES - totalBytes + 1).toInt()
                val read = input.read(buffer, 0, maxToRead)
                if (read < 0) return true
                if (read == 0) continue
                totalBytes += read
                if (totalBytes > MAX_COURSE_BYTES) return false
                output.write(buffer, 0, read)
            }
        }
    }

    private fun resolveSafeTarget(draftId: String): File? {
        if (draftId.isBlank() || '/' in draftId || '\\' in draftId || ".." in draftId) {
            return null
        }
        val candidate = File(folder, "$draftId$JSON_EXTENSION")
        return candidate.takeIf(::isDirectChildOfFolder)
    }

    private fun resolveSafeExisting(filePath: String): File? {
        if (filePath.isBlank() || File(filePath).isAbsolute) return null
        val candidate = File(baseDir, filePath)
        return candidate.takeIf(::isDirectChildOfFolder)
    }

    private fun isDirectChildOfFolder(candidate: File): Boolean = try {
        candidate.canonicalFile.parentFile == folder.canonicalFile
    } catch (_: IOException) {
        false
    }

    companion object {
        private const val FOLDER = "course_drafts"
        private const val JSON_EXTENSION = ".json"
        private const val COPY_BUFFER_BYTES = 8 * 1024

        // Factory Method para producción en Android.
        fun android(context: Context): CourseDraftFiles =
            android(context.contentResolver, context.filesDir)

        // Factory Method reutilizable en tests instrumentados con una carpeta temporal.
        internal fun android(resolver: ContentResolver, baseDir: File): CourseDraftFiles =
            CourseDraftFiles(
                baseDir = baseDir,
                openStream = { uri -> resolver.openInputStream(Uri.parse(uri)) },
                resolveDisplayName = { uri -> queryDisplayName(resolver, uri) },
            )

        private fun queryDisplayName(resolver: ContentResolver, rawUri: String): String {
            val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return ""
            val queried = runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (columnIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(columnIndex)) {
                            cursor.getString(columnIndex)
                        } else {
                            null
                        }
                    }
            }.getOrNull()
            return queried?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment.orEmpty()
        }
    }
}
