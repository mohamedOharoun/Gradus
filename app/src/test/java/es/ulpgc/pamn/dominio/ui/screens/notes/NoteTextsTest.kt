package es.ulpgc.pamn.dominio.ui.screens.notes

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.text.UiText
import es.ulpgc.pamn.dominio.ui.text.longMonth
import es.ulpgc.pamn.dominio.ui.text.shortMonth
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

// Los textos de las notas (docs/tareas/notas-camara.md, tarea 4; con UiText desde ajustes.md, paso 8c): la
// fecha bajo cada foto y lo que lee TalkBack. Cada idioma pone el orden en strings.xml («24 nov» / «Nov 24»).
// Los meses se prueban en MonthsTest. La zona horaria y el «hoy» se pasan por parámetro para que el test no dependa del reloj.
class NoteTextsTest {

    private val canarias: ZoneId = ZoneId.of("Atlantic/Canary")
    private val today = LocalDate.of(2026, 11, 24)

    private fun millisOf(date: LocalDate, hour: Int = 12, zone: ZoneId = canarias) =
        date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun short(month: Int) = shortMonth(month)
    private fun long(month: Int) = longMonth(month)

    // ── La fecha bajo la foto ──

    @Test
    fun `this year it says the day and the short month`() {
        assertEquals(UiText.of(R.string.note_date_short, 24, short(11)), noteDateText(millisOf(today), today, canarias))
        assertEquals(UiText.of(R.string.note_date_short, 3, short(3)), noteDateText(millisOf(LocalDate.of(2026, 3, 3)), today, canarias))
    }

    @Test
    fun `another year it also says the year`() {
        assertEquals(
            UiText.of(R.string.note_date_short_year, 20, short(12), 2025),
            noteDateText(millisOf(LocalDate.of(2025, 12, 20)), today, canarias),
        )
    }

    // La hora guardada es UTC: lo que cuenta es el día en la zona del móvil.
    @Test
    fun `the day is the one of the phone's time zone`() {
        val lateUtc = LocalDate.of(2026, 11, 24).atTime(23, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(UiText.of(R.string.note_date_short, 24, short(11)), noteDateText(lateUtc, today, canarias))
        assertEquals(UiText.of(R.string.note_date_short, 25, short(11)), noteDateText(lateUtc, today, ZoneId.of("Europe/Madrid")))
    }

    // ── La fecha al ver una nota (tarea 6) ──

    @Test
    fun `the full date says the whole month`() {
        assertEquals(UiText.of(R.string.note_date_full, 24, long(11)), noteFullDate(millisOf(today), today, canarias))
        assertEquals(
            UiText.of(R.string.note_date_full_year, 20, long(12), 2025),
            noteFullDate(millisOf(LocalDate.of(2025, 12, 20)), today, canarias),
        )
    }

    // ── Lo que lee TalkBack ──

    @Test
    fun `talkback hears the full date`() {
        assertEquals(
            UiText.of(R.string.note_description, UiText.of(R.string.note_date_full, 24, long(11))),
            noteDescription(millisOf(today), null, today, canarias),
        )
    }

    // Decisión Y: el pie es lo que dice de qué va la foto. Lo escribe el alumno: no se traduce.
    @Test
    fun `talkback hears the caption after the date`() {
        assertEquals(
            UiText.of(R.string.note_description_caption, UiText.of(R.string.note_date_full, 24, long(11)), "Dijkstra paso a paso"),
            noteDescription(millisOf(today), "Dijkstra paso a paso", today, canarias),
        )
    }

    // ── El título ──

    @Test
    fun `the title says whose notes they are`() {
        assertEquals(UiText.of(R.string.notes_title, "BFS"), notesTitle("BFS"))
    }
}
