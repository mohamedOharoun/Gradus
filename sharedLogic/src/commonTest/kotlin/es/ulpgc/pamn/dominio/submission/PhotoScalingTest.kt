package es.ulpgc.pamn.dominio.submission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// Cuánto reducir una foto antes de entregarla (docs/tareas/ejercicio-foto.md, paso 5a, decisiones F y X):
// el lado mayor, como mucho 2000 px, sin deformarla y sin agrandarla nunca.
class PhotoScalingTest {

    @Test
    fun `the largest side is 2000 px`() {
        assertEquals(2000, PhotoScaling.MAX_SIDE)
    }

    // Una foto de móvil típica (4000 × 3000, horizontal).
    @Test
    fun `a landscape photo is reduced to 2000 px wide keeping its proportions`() {
        assertEquals(PixelSize(2000, 1500), PhotoScaling.fit(PixelSize(4000, 3000)))
    }

    @Test
    fun `a portrait photo is reduced to 2000 px tall`() {
        assertEquals(PixelSize(1500, 2000), PhotoScaling.fit(PixelSize(3000, 4000)))
    }

    @Test
    fun `a square photo is reduced on both sides`() {
        assertEquals(PixelSize(2000, 2000), PhotoScaling.fit(PixelSize(4096, 4096)))
    }

    // Agrandar no añade detalle y solo haría la foto más pesada.
    @Test
    fun `a photo that already fits is not enlarged`() {
        assertEquals(PixelSize(1280, 960), PhotoScaling.fit(PixelSize(1280, 960)))
        assertEquals(PixelSize(2000, 1000), PhotoScaling.fit(PixelSize(2000, 1000)))
    }

    // 4032 × 3024 (12 MP): 3024 × 2000 / 4032 = 1500.
    @Test
    fun `the short side is rounded to the nearest pixel`() {
        assertEquals(PixelSize(2000, 1500), PhotoScaling.fit(PixelSize(4032, 3024)))
        assertEquals(PixelSize(2000, 667), PhotoScaling.fit(PixelSize(3000, 1000)))
    }

    // Una tira muy estrecha no se queda en 0 px.
    @Test
    fun `a very thin photo keeps at least one pixel`() {
        assertEquals(PixelSize(2000, 1), PhotoScaling.fit(PixelSize(10_000, 1)))
    }

    // ── Leer la foto ya reducida (BitmapFactory solo reduce en potencias de 2) ──

    // Se reduce al leer lo más posible sin bajar de 2000 px; el resto, al escalar. Así no se carga en
    // memoria una foto de 48 MP entera, y tampoco se pierde detalle.
    @Test
    fun `a photo is read halved as long as it stays over 2000 px`() {
        assertEquals(2, PhotoScaling.sampleSize(PixelSize(4000, 3000)))
        assertEquals(4, PhotoScaling.sampleSize(PixelSize(8000, 6000)))
        assertEquals(2, PhotoScaling.sampleSize(PixelSize(7999, 6000)))
    }

    @Test
    fun `a photo that is not even twice the maximum is read whole`() {
        assertEquals(1, PhotoScaling.sampleSize(PixelSize(3999, 3000)))
        assertEquals(1, PhotoScaling.sampleSize(PixelSize(1280, 960)))
    }

    @Test
    fun `a size has positive sides`() {
        assertFailsWith<IllegalArgumentException> { PixelSize(0, 10) }
        assertFailsWith<IllegalArgumentException> { PixelSize(10, -1) }
    }
}
