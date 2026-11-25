package es.ulpgc.pamn.dominio.course.progress.http

import es.ulpgc.pamn.dominio.course.progress.Enrollment
import es.ulpgc.pamn.dominio.course.progress.EnrollmentSnapshot
import es.ulpgc.pamn.dominio.course.progress.MasteredModule
import es.ulpgc.pamn.dominio.course.progress.ProgressSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

// Del progreso de la app al cuerpo de POST /api/progress/sync (backend/docs/api-progreso.md).
// Todos los campos son obligatorios en el contrato, también `mastered` aunque esté vacío.
class SyncRequestEncodingTest {

    private val enrolledAt = Instant.parse("2026-11-24T10:00:00Z")
    private val masteredAt = Instant.parse("2026-11-24T10:20:00Z")

    @Test
    fun `an enrollment without mastered modules writes an empty mastered list`() {
        val text = encodeSyncRequest(snapshot(enrollment("c1", enrolledAt)))
        assertTrue(""""mastered":[]""" in text, text)
    }

    @Test
    fun `an empty snapshot writes an empty list of enrollments`() {
        assertEquals("""{"enrollments":[]}""", encodeSyncRequest(ProgressSnapshot(emptyList())))
    }

    @Test
    fun `every field of an enrollment and of its modules is written`() {
        val text = encodeSyncRequest(snapshot(enrollment("c1", enrolledAt, MasteredModule("bfs", masteredAt))))
        val written = enrollmentsOf(text).single()
        assertEquals(setOf("courseId", "enrolledAt", "mastered"), written.keys)
        assertEquals("c1", written.getValue("courseId").jsonPrimitive.content)
        assertEquals("2026-11-24T10:00:00Z", written.getValue("enrolledAt").jsonPrimitive.content)
        val module = written.getValue("mastered").jsonArray.single().jsonObject
        assertEquals(setOf("moduleId", "masteredAt"), module.keys)
        assertEquals("bfs", module.getValue("moduleId").jsonPrimitive.content)
        assertEquals("2026-11-24T10:20:00Z", module.getValue("masteredAt").jsonPrimitive.content)
    }

    @Test
    fun `every module is written inside its own enrollment`() {
        val text = encodeSyncRequest(
            snapshot(
                enrollment("c1", enrolledAt, MasteredModule("a", masteredAt), MasteredModule("b", masteredAt)),
                enrollment("c2", enrolledAt, MasteredModule("z", masteredAt)),
            ),
        )
        val modulesByCourse = enrollmentsOf(text).associate { written ->
            written.getValue("courseId").jsonPrimitive.content to
                written.getValue("mastered").jsonArray.map { it.jsonObject.getValue("moduleId").jsonPrimitive.content }.toSet()
        }
        assertEquals(mapOf("c1" to setOf("a", "b"), "c2" to setOf("z")), modulesByCourse)
    }

    @Test
    fun `dates are written in UTC with a Z`() {
        val moment = Instant.fromEpochSeconds(1_700_000_000)
        val text = encodeSyncRequest(snapshot(enrollment("c1", moment, MasteredModule("m", moment))))
        assertTrue(""""enrolledAt":"2023-11-14T22:13:20Z"""" in text, text)
        assertTrue(""""masteredAt":"2023-11-14T22:13:20Z"""" in text, text)
    }

    @Test
    fun `a date with nanoseconds is written whole and the server rounds it`() {
        val moment = Instant.fromEpochSeconds(1_700_000_000, 123_456_789)
        val text = encodeSyncRequest(snapshot(enrollment("c1", moment)))
        assertTrue(""""enrolledAt":"2023-11-14T22:13:20.123456789Z"""" in text, text)
    }

    @Test
    fun `a module id with quotes and accents survives the round trip`() {
        val tricky = "ñandú \"2\" \\ ✓"
        val text = encodeSyncRequest(snapshot(enrollment("c1", enrolledAt, MasteredModule(tricky, masteredAt))))
        val module = enrollmentsOf(text).single().getValue("mastered").jsonArray.single().jsonObject
        assertEquals(tricky, module.getValue("moduleId").jsonPrimitive.content)
    }

    // ── Ayudas ──

    private fun enrollment(courseId: String, at: Instant, vararg modules: MasteredModule) =
        EnrollmentSnapshot(Enrollment(courseId, at), modules.toList())

    private fun snapshot(vararg enrollments: EnrollmentSnapshot) = ProgressSnapshot(enrollments.toList())

    // Se lee el texto como JSON para no depender del orden de las claves ni de los espacios.
    private fun enrollmentsOf(text: String): List<JsonObject> =
        (Json.parseToJsonElement(text).jsonObject.getValue("enrollments") as JsonArray).map { it.jsonObject }
}
