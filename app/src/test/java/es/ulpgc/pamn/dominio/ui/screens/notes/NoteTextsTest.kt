package es.ulpgc.pamn.dominio.ui.screens.notes

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

// Los textos de las notas (docs/tareas/notas-camara.md, tarea 4): la fecha bajo cada foto y lo que lee
// TalkBack. La zona horaria y el «hoy» se pasan por parámetro para que el test no dependa del reloj.
class NoteTextsTest {

    private val canarias: ZoneId = ZoneId.of("Atlantic/Canary")
    private val today = LocalDate.of(2026, 11, 24)

    private fun millisOf(date: LocalDate, hour: Int = 12, zone: ZoneId = canarias) =
        date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    // ── La fecha bajo la foto ──

    @Test
    fun `this year it says the day and the short month`() {
        assertEquals("24 nov", noteDateText(millisOf(today), today, canarias))
        assertEquals("3 mar", noteDateText(millisOf(LocalDate.of(2026, 3, 3)), today, canarias))
    }

    @Test
    fun `another year it also says the year`() {
        assertEquals("20 dic 2025", noteDateText(millisOf(LocalDate.of(2025, 12, 20)), today, canarias))
    }

    // La hora guardada es UTC: lo que cuenta es el día en la zona del móvil.
    @Test
    fun `the day is the one of the phone's time zone`() {
        val lateUtc = LocalDate.of(2026, 11, 24).atTime(23, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("24 nov", noteDateText(lateUtc, today, canarias))
        assertEquals("25 nov", noteDateText(lateUtc, today, ZoneId.of("Europe/Madrid")))
    }

    // Todos los meses, para que ninguno salga mal abreviado.
    @Test
    fun `every month has its short name`() {
        val names = (1..12).map { noteDateText(millisOf(LocalDate.of(2026, it, 1)), today, canarias).substringAfter(" ") }
        assertEquals(listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic"), names)
    }

    // ── Lo que lee TalkBack ──

    @Test
    fun `talkback hears the full date`() {
        assertEquals("Nota del 24 de noviembre", noteDescription(millisOf(today), null, today, canarias))
    }

    @Test
    fun `talkback hears the year when it is another one`() {
        assertEquals("Nota del 20 de diciembre de 2025", noteDescription(millisOf(LocalDate.of(2025, 12, 20)), null, today, canarias))
    }

    // Decisión Y: el pie es lo que dice de qué va la foto.
    @Test
    fun `talkback hears the caption after the date`() {
        assertEquals(
            "Nota del 24 de noviembre: Dijkstra paso a paso",
            noteDescription(millisOf(today), "Dijkstra paso a paso", today, canarias),
        )
    }

    // ── La fecha bajo la foto al verla (tarea 6) ──

    @Test
    fun `the full date says the whole month`() {
        assertEquals("24 de noviembre", noteFullDate(millisOf(today), today, canarias))
        assertEquals("20 de diciembre de 2025", noteFullDate(millisOf(LocalDate.of(2025, 12, 20)), today, canarias))
    }

    // ── El título ──

    @Test
    fun `the title says whose notes they are`() {
        assertEquals("Notas · BFS", notesTitle("BFS"))
    }
}
