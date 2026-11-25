package es.ulpgc.pamn.dominio.submission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// Lo que se entrega: la foto y a qué ejercicio de qué versión del curso va (docs/tareas/ejercicio-foto.md,
// paso 4c, decisión M).
class PhotoSubmissionTest {

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2, 3)

    private fun submission(bytes: ByteArray = jpeg, version: Int = 1) =
        PhotoSubmission(courseId = "c1", courseVersion = version, exerciseId = "repr-foto", jpeg = bytes)

    // Un ByteArray se compara por identidad: sin equals propio, dos entregas iguales serían distintas.
    @Test
    fun `two submissions with the same photo are equal`() {
        assertEquals(submission(), submission(jpeg.copyOf()))
        assertEquals(submission().hashCode(), submission(jpeg.copyOf()).hashCode())
    }

    @Test
    fun `a different photo makes a different submission`() {
        assertNotEquals(submission(), submission(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 9)))
    }

    // Cambiar el array de fuera no cambia lo que se va a entregar.
    @Test
    fun `changing the original array does not change the photo`() {
        val bytes = jpeg.copyOf()
        val submission = submission(bytes)
        bytes[3] = 99
        assertEquals(1, submission.jpeg[3])
    }

    @Test
    fun `changing the array it gives back does not change the photo either`() {
        val submission = submission()
        submission.jpeg[3] = 99
        assertEquals(1, submission.jpeg[3])
    }

    // Una foto de 2 MB no debe acabar en los logs.
    @Test
    fun `its text says the size of the photo, not its bytes`() {
        val text = submission().toString()
        assertTrue("6 bytes" in text, text)
        assertTrue("repr-foto" in text, text)
        assertFalse("-1" in text, text)
    }

    // El mismo máximo que el servidor (Jpeg.MAX_BYTES en el backend).
    @Test
    fun `the maximum is the one of the server`() {
        assertEquals(2_000_000, PhotoSubmission.MAX_BYTES)
    }

    @Test
    fun `it knows when the photo is over the maximum`() {
        assertFalse(submission(ByteArray(PhotoSubmission.MAX_BYTES)).isTooLarge)
        assertTrue(submission(ByteArray(PhotoSubmission.MAX_BYTES + 1)).isTooLarge)
    }

    @Test
    fun `the version is at least 1`() {
        assertFailsWith<IllegalArgumentException> { submission(version = 0) }
    }

    @Test
    fun `the course and the exercise are not blank`() {
        assertFailsWith<IllegalArgumentException> { PhotoSubmission(" ", 1, "repr-foto", jpeg) }
        assertFailsWith<IllegalArgumentException> { PhotoSubmission("c1", 1, "", jpeg) }
    }

    // Comprobar que es un JPEG es cosa del servidor (Jpeg.check): aquí solo que hay algo que enviar.
    @Test
    fun `there is a photo to send`() {
        assertFailsWith<IllegalArgumentException> { submission(ByteArray(0)) }
    }
}
