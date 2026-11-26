package es.ulpgc.pamn.dominio.ui.screens.notes

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.text.UiText
import es.ulpgc.pamn.dominio.ui.text.longMonth
import es.ulpgc.pamn.dominio.ui.text.shortMonth
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Textos de las notas con cámara (docs/tareas/notas-camara.md, tarea 4). Funciones puras, probadas en el
// ordenador (NoteTextsTest). «Hoy» y la zona horaria se pasan por parámetro: así el test no depende del reloj.
// Con UiText desde docs/tareas/ajustes.md, paso 8c: cada idioma pone en strings.xml el orden de la fecha
// («24 nov» / «Nov 24»). Los nombres de los meses están en ui/text/Months.kt (paso 9a).

// El día en el que se hizo la foto, en la zona horaria del móvil (createdAt está en UTC).
private fun dayOf(createdAt: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(createdAt).atZone(zone).toLocalDate()

// Bajo cada foto: «24 nov»; si es de otro año, «20 dic 2025».
fun noteDateText(createdAt: Long, today: LocalDate, zone: ZoneId): UiText {
    val day = dayOf(createdAt, zone)
    val month = shortMonth(day.monthValue)
    return if (day.year == today.year) UiText.of(R.string.note_date_short, day.dayOfMonth, month)
    else UiText.of(R.string.note_date_short_year, day.dayOfMonth, month, day.year)
}

// Al ver una nota (tarea 6): «24 de noviembre»; si es de otro año, «20 de diciembre de 2025».
fun noteFullDate(createdAt: Long, today: LocalDate, zone: ZoneId): UiText {
    val day = dayOf(createdAt, zone)
    val month = longMonth(day.monthValue)
    return if (day.year == today.year) UiText.of(R.string.note_date_full, day.dayOfMonth, month)
    else UiText.of(R.string.note_date_full_year, day.dayOfMonth, month, day.year)
}

// Lo que lee TalkBack (decisión Y): «Nota del 24 de noviembre» y, si tiene pie, «: Dijkstra paso a paso».
// El pie lo escribe el alumno: va tal cual.
fun noteDescription(createdAt: Long, caption: String?, today: LocalDate, zone: ZoneId): UiText {
    val date = noteFullDate(createdAt, today, zone)
    return if (caption == null) UiText.of(R.string.note_description, date)
    else UiText.of(R.string.note_description_caption, date, caption)
}

fun notesTitle(moduleTitle: String): UiText = UiText.of(R.string.notes_title, moduleTitle)
