package es.ulpgc.pamn.dominio.course.progress.http

import es.ulpgc.pamn.dominio.account.http.json
import es.ulpgc.pamn.dominio.course.progress.ProgressSnapshot
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// La petición de POST /api/progress/sync: método, ruta, sesión y cuerpo (bytes UTF-8 sin BOM).
class HttpProgressGatewayRequestTest {

    private suspend fun sentWith(snapshot: ProgressSnapshot): HttpRequestData {
        var sent: HttpRequestData? = null
        progressGatewayAnswering { sent = it; json(HttpStatusCode.OK, SYNC_OK_JSON) }.sync(testSession, snapshot)
        return sent!!
    }

    private fun HttpRequestData.bodyBytes(): ByteArray {
        val content = body
        assertIs<OutgoingContent.ByteArrayContent>(content)
        return content.bytes()
    }

    @Test
    fun `it posts to the sync route`() = runTest {
        val sent = sentWith(noPending)
        assertEquals(HttpMethod.Post, sent.method)
        assertEquals("/api/progress/sync", sent.url.encodedPath)
    }

    @Test
    fun `it sends the session token as a bearer`() = runTest {
        val sent = sentWith(noPending)
        assertEquals("Bearer $TEST_TOKEN", sent.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `the body is declared as application json`() = runTest {
        val sent = sentWith(noPending)
        assertEquals(ContentType.Application.Json, sent.body.contentType)
    }

    // Los bytes enviados son exactamente los del mapper, en UTF-8 (ñ y tilde ocupan dos bytes).
    @Test
    fun `the body bytes are the UTF-8 of the encoded request`() = runTest {
        val snapshot = snapshotOf("0b8f6d2e", "señal-áéí")
        val sent = sentWith(snapshot)
        assertEquals(encodeSyncRequest(snapshot), sent.bodyBytes().decodeToString())
        assertTrue(sent.bodyBytes().toList().containsAll("ñ".encodeToByteArray().toList()))
        assertTrue(sent.bodyBytes().size > encodeSyncRequest(snapshot).length)
    }

    @Test
    fun `the body has no BOM`() = runTest {
        val bytes = sentWith(snapshotOf("0b8f6d2e", "bfs")).bodyBytes()
        assertTrue(bytes.size >= 3)
        assertTrue(bytes.take(3) != listOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        assertEquals('{'.code.toByte(), bytes.first())
    }

    // Si ContentNegotiation lo serializara otra vez, empezaría por «"» (un texto JSON dentro de otro).
    @Test
    fun `the body is not serialized a second time`() = runTest {
        val text = sentWith(snapshotOf("0b8f6d2e", "bfs")).bodyBytes().decodeToString()
        assertTrue(text.startsWith("{\"enrollments\""))
    }

    @Test
    fun `an enrollment with no modules is sent with an empty mastered list`() = runTest {
        val text = sentWith(snapshotOf("0b8f6d2e")).bodyBytes().decodeToString()
        assertTrue("\"mastered\":[]" in text)
    }

    @Test
    fun `with nothing pending it sends an empty enrollments list`() = runTest {
        assertEquals("""{"enrollments":[]}""", sentWith(noPending).bodyBytes().decodeToString())
    }
}
