package es.ulpgc.pamn.dominio.course.progress.http

import es.ulpgc.pamn.dominio.course.progress.Enrollment
import es.ulpgc.pamn.dominio.course.progress.EnrollmentSnapshot
import es.ulpgc.pamn.dominio.course.progress.MasteredModule
import es.ulpgc.pamn.dominio.course.progress.SkippedProgress
import es.ulpgc.pamn.dominio.course.progress.SyncResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

// Del cuerpo 200 de POST /api/progress/sync al modelo de la app. Estricto con lo obligatorio,
// tolerante con lo desconocido; un cuerpo que no se puede leer da null, nunca una excepción.
class SyncResponseDecodingTest {

    // ── Lo que llega bien ──

    @Test
    fun `the remote progress keeps its enrollments and their modules`() {
        val response = decode(
            """{"enrollments": [{"courseId": "c1", "enrolledAt": "2026-11-24T10:00:00Z",
                 "mastered": [{"moduleId": "bfs", "masteredAt": "2026-11-24T10:20:00Z"}]}],
                "skipped": []}""",
        )
        assertEquals(
            listOf(
                EnrollmentSnapshot(
                    Enrollment("c1", Instant.parse("2026-11-24T10:00:00Z")),
                    listOf(MasteredModule("bfs", Instant.parse("2026-11-24T10:20:00Z"))),
                ),
            ),
            response.remote.enrollments,
        )
    }

    @Test
    fun `a response with nothing in it is readable`() {
        val response = decode("""{"enrollments": [], "skipped": []}""")
        assertEquals(emptyList(), response.remote.enrollments)
        assertEquals(emptyList(), response.skipped)
    }

    @Test
    fun `dates without decimals are read`() = assertDateRead("2026-11-24T10:00:00Z", 0)

    @Test
    fun `dates with three decimals are read`() = assertDateRead("2026-11-24T10:00:00.500Z", 500_000_000)

    @Test
    fun `dates with six decimals are read`() = assertDateRead("2026-11-24T10:00:00.123457Z", 123_457_000)

    @Test
    fun `a course id is kept exactly as it arrives`() {
        val response = decode(enrollmentsBody("""{"courseId": "0B8F-ABC", "enrolledAt": "2026-11-24T10:00:00Z", "mastered": []}"""))
        assertEquals("0B8F-ABC", response.remote.enrollments.single().enrollment.courseId)
    }

    // ── Lo omitido (skipped) ──

    @Test
    fun `a skipped entry without module id is the whole enrollment`() {
        val response = decode(skippedBody("""{"courseId": "9a1c", "code": "COURSE_NOT_FOUND"}"""))
        assertEquals(listOf<SkippedProgress>(SkippedProgress.WholeEnrollment("9a1c", "COURSE_NOT_FOUND")), response.skipped)
    }

    // Decisión B-9: skipped es solo informativo (B-8), así que un null no debe romper la sincronización.
    @Test
    fun `a null moduleId in skipped is the whole enrollment`() {
        val response = decode(skippedBody("""{"courseId": "9a1c", "moduleId": null, "code": "COURSE_NOT_FOUND"}"""))
        assertEquals(listOf<SkippedProgress>(SkippedProgress.WholeEnrollment("9a1c", "COURSE_NOT_FOUND")), response.skipped)
    }

    @Test
    fun `a skipped entry with module id is only that module`() {
        val response = decode(skippedBody("""{"courseId": "c1", "moduleId": "bfs", "code": "REQUIRED"}"""))
        assertEquals(listOf<SkippedProgress>(SkippedProgress.SingleModule("c1", "bfs", "REQUIRED")), response.skipped)
    }

    @Test
    fun `a blank module id in skipped is kept as it arrives`() {
        val response = decode(skippedBody("""{"courseId": "c1", "moduleId": " ", "code": "REQUIRED"}"""))
        assertEquals(listOf<SkippedProgress>(SkippedProgress.SingleModule("c1", " ", "REQUIRED")), response.skipped)
    }

    @Test
    fun `an empty module id in skipped is kept as it arrives`() {
        val response = decode(skippedBody("""{"courseId": "c1", "moduleId": "", "code": "REQUIRED"}"""))
        assertEquals(listOf<SkippedProgress>(SkippedProgress.SingleModule("c1", "", "REQUIRED")), response.skipped)
    }

    @Test
    fun `a code the app does not know is kept as text`() {
        val response = decode(skippedBody("""{"courseId": "c1", "code": "SOMETHING_NEW"}"""))
        assertEquals("SOMETHING_NEW", response.skipped.single().code)
    }

    @Test
    fun `skipped entries of both kinds are all kept`() {
        val response = decode(
            skippedBody(
                """{"courseId": "a", "code": "REQUIRED", "moduleId": " "},
                   {"courseId": "b", "code": "COURSE_NOT_FOUND"}""",
            ),
        )
        assertEquals(
            setOf<SkippedProgress>(
                SkippedProgress.SingleModule("a", " ", "REQUIRED"),
                SkippedProgress.WholeEnrollment("b", "COURSE_NOT_FOUND"),
            ),
            response.skipped.toSet(),
        )
    }

    // ── Campos desconocidos ──

    @Test
    fun `unknown fields are ignored at every level`() {
        val response = decode(
            """{"extra": 1, "enrollments": [{"courseId": "c1", "enrolledAt": "2026-11-24T10:00:00Z", "x": [],
                 "mastered": [{"moduleId": "m", "masteredAt": "2026-11-24T10:20:00Z", "y": null}]}],
                "skipped": [{"courseId": "c2", "code": "REQUIRED", "z": {"a": 1}}]}""",
        )
        assertEquals(1, response.remote.enrollments.size)
        assertEquals(1, response.remote.enrollments.single().mastered.size)
        assertEquals(listOf<SkippedProgress>(SkippedProgress.WholeEnrollment("c2", "REQUIRED")), response.skipped)
    }

    // ── Lo obligatorio que falta o viene mal: no se puede leer ──

    @Test
    fun `a response without enrollments cannot be read`() = assertUnreadable("""{"skipped": []}""")

    @Test
    fun `a response without skipped cannot be read`() = assertUnreadable("""{"enrollments": []}""")

    @Test
    fun `an enrollment without courseId cannot be read`() =
        assertUnreadable(enrollmentsBody("""{"enrolledAt": "2026-11-24T10:00:00Z", "mastered": []}"""))

    @Test
    fun `an enrollment without enrolledAt cannot be read`() =
        assertUnreadable(enrollmentsBody("""{"courseId": "c1", "mastered": []}"""))

    @Test
    fun `an enrollment without mastered cannot be read`() =
        assertUnreadable(enrollmentsBody("""{"courseId": "c1", "enrolledAt": "2026-11-24T10:00:00Z"}"""))

    @Test
    fun `a mastered module without moduleId cannot be read`() =
        assertUnreadable(masteredBody("""{"masteredAt": "2026-11-24T10:20:00Z"}"""))

    @Test
    fun `a mastered module without masteredAt cannot be read`() =
        assertUnreadable(masteredBody("""{"moduleId": "m"}"""))

    @Test
    fun `a skipped entry without courseId cannot be read`() =
        assertUnreadable(skippedBody("""{"code": "REQUIRED"}"""))

    @Test
    fun `a skipped entry without code cannot be read`() =
        assertUnreadable(skippedBody("""{"courseId": "c1"}"""))

    @Test
    fun `a required field with null cannot be read`() {
        assertUnreadable("""{"enrollments": null, "skipped": []}""")
        assertUnreadable(enrollmentsBody("""{"courseId": null, "enrolledAt": "2026-11-24T10:00:00Z", "mastered": []}"""))
        assertUnreadable(skippedBody("""{"courseId": "c1", "code": null}"""))
    }

    @Test
    fun `a field with the wrong type cannot be read`() {
        assertUnreadable("""{"enrollments": {}, "skipped": []}""")
        assertUnreadable(enrollmentsBody("""{"courseId": 7, "enrolledAt": "2026-11-24T10:00:00Z", "mastered": []}"""))
        assertUnreadable(enrollmentsBody("""{"courseId": "c1", "enrolledAt": "2026-11-24T10:00:00Z", "mastered": "no"}"""))
        assertUnreadable(skippedBody("""{"courseId": "c1", "moduleId": 3, "code": "REQUIRED"}"""))
    }

    @Test
    fun `an invalid date in an enrollment cannot be read`() {
        for (date in INVALID_DATES) {
            assertUnreadable(enrollmentsBody("""{"courseId": "c1", "enrolledAt": "$date", "mastered": []}"""), date)
        }
    }

    @Test
    fun `an invalid date in a mastered module cannot be read`() {
        for (date in INVALID_DATES) {
            assertUnreadable(masteredBody("""{"moduleId": "m", "masteredAt": "$date"}"""), date)
        }
    }

    @Test
    fun `a body that is not JSON cannot be read`() {
        assertUnreadable("""{"enrollments": [""")
        assertUnreadable("")
        assertUnreadable("<html>Error</html>")
        assertUnreadable("Content Too Large")
    }

    // ── Ayudas ──

    private fun decode(body: String): SyncResponse {
        val response = decodeSyncResponse(body)
        assertNotNull(response, body)
        return response
    }

    private fun assertUnreadable(body: String, message: String? = null) =
        assertNull(decodeSyncResponse(body), message ?: body)

    private fun assertDateRead(text: String, nanos: Int) {
        val response = decode(enrollmentsBody("""{"courseId": "c1", "enrolledAt": "$text", "mastered": []}"""))
        val expected = Instant.fromEpochSeconds(Instant.parse("2026-11-24T10:00:00Z").epochSeconds, nanos)
        assertEquals(expected, response.remote.enrollments.single().enrollment.enrolledAt)
    }

    private fun enrollmentsBody(vararg enrollments: String) =
        """{"enrollments": [${enrollments.joinToString(",")}], "skipped": []}"""

    private fun masteredBody(module: String) = enrollmentsBody(
        """{"courseId": "c1", "enrolledAt": "2026-11-24T10:00:00Z", "mastered": [$module]}""",
    )

    private fun skippedBody(vararg entries: String) =
        """{"enrollments": [], "skipped": [${entries.joinToString(",")}]}"""

    private companion object {
        val INVALID_DATES = listOf("ayer", "", "2026-13-01T10:00:00Z", "2026-11-24", "2026-11-24 10:00:00Z", "2026-11-24T10:00:00")
    }
}
