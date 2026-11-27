package es.ulpgc.pamn.dominio.data.publication

import es.ulpgc.pamn.dominio.course.publication.CourseDraftPreview
import es.ulpgc.pamn.dominio.course.publication.MAX_COURSE_BYTES
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class CourseDraftFilesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun TestScope.createFiles(
        openStream: (String) -> InputStream? = { null },
        resolveDisplayName: (String) -> String = { "curso.json" },
    ): CourseDraftFiles = CourseDraftFiles(
        baseDir = tempFolder.root,
        openStream = openStream,
        resolveDisplayName = resolveDisplayName,
        ioDispatcher = StandardTestDispatcher(testScheduler),
    )

    private fun draftFolder(): File = File(tempFolder.root, "course_drafts")

    @Test
    fun `imports valid json and extracts title and module count`() = runTest {
        val json = """{"title":"Álgebra Lineal","modules":[{"id":"m1"},{"id":"m2"}]}""".toByteArray()
        val files = createFiles(
            openStream = { ByteArrayInputStream(json) },
            resolveDisplayName = { "ignored.json" },
        )

        val result = files.import("draft-1", "content://picker/1")

        assertEquals(
            DraftImportResult.Imported(
                filePath = "course_drafts/draft-1.json",
                preview = CourseDraftPreview(title = "Álgebra Lineal", moduleCount = 2),
            ),
            result,
        )
        assertArrayEquals(json, files.read("course_drafts/draft-1.json"))
    }

    @Test
    fun `falls back to display name without extension when json has no title`() = runTest {
        val invalidJson = "{not valid json".toByteArray()
        val files = createFiles(
            openStream = { ByteArrayInputStream(invalidJson) },
            resolveDisplayName = { "mi-borrador.json" },
        )

        val result = files.import("draft-2", "content://picker/2")

        assertEquals(
            DraftImportResult.Imported(
                filePath = "course_drafts/draft-2.json",
                preview = CourseDraftPreview(title = "mi-borrador", moduleCount = null),
            ),
            result,
        )
        assertArrayEquals(invalidJson, files.read("course_drafts/draft-2.json"))
    }

    @Test
    fun `accepts a file of exact MAX_COURSE_BYTES`() = runTest {
        val exactBytes = ByteArray(MAX_COURSE_BYTES) { ' '.code.toByte() }
        val files = createFiles(
            openStream = { ByteArrayInputStream(exactBytes) },
            resolveDisplayName = { "limite-exacto.json" },
        )

        val result = files.import("exact", "content://picker/exact")

        assertTrue(result is DraftImportResult.Imported)
        val imported = result as DraftImportResult.Imported
        assertEquals("course_drafts/exact.json", imported.filePath)
        assertEquals("limite-exacto", imported.preview.title)
        assertArrayEquals(exactBytes, files.read(imported.filePath))
    }

    @Test
    fun `rejects a file one byte over MAX_COURSE_BYTES and deletes partial file`() = runTest {
        val oversized = ByteArray(MAX_COURSE_BYTES + 1) { 'a'.code.toByte() }
        val files = createFiles(openStream = { ByteArrayInputStream(oversized) })

        val result = files.import("too-big", "content://picker/too-big")

        assertEquals(DraftImportResult.TooLarge, result)
        assertFalse(File(draftFolder(), "too-big.json").exists())
    }

    @Test
    fun `stops reading a huge stream early without consuming it all`() = runTest {
        val tenMiB = 10 * 1024 * 1024
        val countingStream = CountingStream(totalBytes = tenMiB)
        val files = createFiles(openStream = { countingStream })

        val result = files.import("huge", "content://picker/huge")

        assertEquals(DraftImportResult.TooLarge, result)
        assertTrue(countingStream.closed)
        assertTrue(
            "expected read to stop near MAX_COURSE_BYTES, read ${countingStream.bytesRead}",
            countingStream.bytesRead <= MAX_COURSE_BYTES + 8 * 1024,
        )
        assertFalse(File(draftFolder(), "huge.json").exists())
    }

    @Test
    fun `returns Unreadable when stream is null`() = runTest {
        val files = createFiles(openStream = { null })

        val result = files.import("missing", "content://picker/missing")

        assertEquals(DraftImportResult.Unreadable, result)
        assertFalse(File(draftFolder(), "missing.json").exists())
    }

    @Test
    fun `returns Unreadable and removes partial file on IOException mid-copy`() = runTest {
        val failingStream = FailingAfterBytesStream(bytesBeforeFailure = 4_096) {
            IOException("disk disconnected")
        }
        val files = createFiles(openStream = { failingStream })

        val result = files.import("io-fail", "content://picker/io-fail")

        assertEquals(DraftImportResult.Unreadable, result)
        assertFalse(File(draftFolder(), "io-fail.json").exists())
    }

    @Test
    fun `returns Unreadable and removes partial file on SecurityException`() = runTest {
        val failingStream = FailingAfterBytesStream(bytesBeforeFailure = 1_024) {
            SecurityException("permission revoked")
        }
        val files = createFiles(openStream = { failingStream })

        val result = files.import("sec-fail", "content://picker/sec-fail")

        assertEquals(DraftImportResult.Unreadable, result)
        assertFalse(File(draftFolder(), "sec-fail.json").exists())
    }

    @Test
    fun `returns Unreadable on IllegalArgumentException`() = runTest {
        val files = createFiles(openStream = { throw IllegalArgumentException("bad uri") })

        val result = files.import("bad-uri", "not-a-uri")

        assertEquals(DraftImportResult.Unreadable, result)
        assertFalse(File(draftFolder(), "bad-uri.json").exists())
    }

    @Test
    fun `read returns null when file is missing or exceeds MAX_COURSE_BYTES on disk`() = runTest {
        val files = createFiles()
        assertNull(files.read("course_drafts/nope.json"))

        draftFolder().mkdirs()
        File(draftFolder(), "oversized-on-disk.json").writeBytes(ByteArray(MAX_COURSE_BYTES + 1))

        assertNull(files.read("course_drafts/oversized-on-disk.json"))
    }

    @Test
    fun `delete removes existing file and is idempotent when missing`() = runTest {
        val bytes = """{"title":"Redes"}""".toByteArray()
        val files = createFiles(openStream = { ByteArrayInputStream(bytes) })
        val imported = files.import("d1", "content://picker/1") as DraftImportResult.Imported

        files.delete(imported.filePath)
        files.delete(imported.filePath)

        assertNull(files.read(imported.filePath))
        assertFalse(File(tempFolder.root, imported.filePath).exists())
    }

    @Test
    fun `deleteAll removes all given draft files and ignores absent ones`() = runTest {
        val files = createFiles(openStream = { ByteArrayInputStream("{}".toByteArray()) })
        val first = (files.import("d1", "content://1") as DraftImportResult.Imported).filePath
        val second = (files.import("d2", "content://2") as DraftImportResult.Imported).filePath

        files.deleteAll(listOf(first, "course_drafts/missing.json", second))

        assertFalse(File(tempFolder.root, first).exists())
        assertFalse(File(tempFolder.root, second).exists())
    }

    @Test
    fun `import rejects blank, traversal, and invalid draft ids`() = runTest {
        val files = createFiles(openStream = { ByteArrayInputStream("{}".toByteArray()) })
        val badIds = listOf("", "   ", "../escape", "sub/file", "sub\\file", "a..b", "bad\u0000id")

        badIds.forEach { badId ->
            assertEquals("expected Unreadable for '$badId'", DraftImportResult.Unreadable, files.import(badId, "content://1"))
        }
        assertEquals(0, draftFolder().listFiles()?.size ?: 0)
    }

    @Test
    fun `read and delete ignore blank, absolute, traversal, and invalid paths`() = runTest {
        val outside = File(tempFolder.root, "secret.txt").apply { writeText("keep") }
        val nestedDir = File(draftFolder(), "sub").apply { mkdirs() }
        val nested = File(nestedDir, "nested.json").apply { writeText("{}") }
        val files = createFiles()
        val badPaths = listOf(
            "",
            "   ",
            outside.absolutePath,
            "course_drafts/../secret.txt",
            "../secret.txt",
            "course_drafts/sub/nested.json",
            "course_drafts",
            "course_drafts/bad\u0000.json",
        )

        badPaths.forEach { badPath ->
            assertNull("read should reject '$badPath'", files.read(badPath))
            files.delete(badPath)
        }
        files.deleteAll(badPaths)

        assertTrue(outside.exists())
        assertTrue(nested.exists())
    }

    private class CountingStream(private val totalBytes: Int) : InputStream() {
        var bytesRead: Int = 0
            private set
        var closed: Boolean = false
            private set

        override fun read(): Int {
            if (bytesRead >= totalBytes) return -1
            bytesRead++
            return 'x'.code
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val remaining = totalBytes - bytesRead
            if (remaining <= 0) return -1
            val count = minOf(len, remaining)
            b.fill('x'.code.toByte(), off, off + count)
            bytesRead += count
            return count
        }

        override fun close() {
            closed = true
        }
    }

    private class FailingAfterBytesStream(
        bytesBeforeFailure: Int,
        private val failure: () -> Throwable,
    ) : InputStream() {
        private var remainingBeforeFailure = bytesBeforeFailure

        override fun read(): Int {
            if (remainingBeforeFailure <= 0) throw failure()
            remainingBeforeFailure--
            return 'a'.code
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (remainingBeforeFailure <= 0) throw failure()
            val count = minOf(len, remainingBeforeFailure)
            b.fill('a'.code.toByte(), off, off + count)
            remainingBeforeFailure -= count
            return count
        }
    }
}
