package es.ulpgc.pamn.dominio.ui.screens.notes

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Textos de las notas con cámara (docs/tareas/notas-camara.md, tarea 4). Funciones puras, probadas en el
// ordenador (NoteTextsTest). «Hoy» y la zona horaria se pasan por parámetro: así el test no depende del reloj.
// Los meses van en una lista propia: las abreviaturas de Java en español cambian según la versión
// («nov» o «nov.»).

private val shortMonths = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic")
private val months = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

// El día en el que se hizo la foto, en la zona horaria del móvil (createdAt está en UTC).
private fun dayOf(createdAt: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(createdAt).atZone(zone).toLocalDate()

// Bajo cada foto: «24 nov»; si es de otro año, «20 dic 2025».
fun noteDateText(createdAt: Long, today: LocalDate, zone: ZoneId): String {
    val day = dayOf(createdAt, zone)
    val text = "${day.dayOfMonth} ${shortMonths[day.monthValue - 1]}"
    return if (day.year == today.year) text else "$text ${day.year}"
}

// Al ver una nota (tarea 6): «24 de noviembre»; si es de otro año, «20 de diciembre de 2025».
fun noteFullDate(createdAt: Long, today: LocalDate, zone: ZoneId): String {
    val day = dayOf(createdAt, zone)
    val year = if (day.year == today.year) "" else " de ${day.year}"
    return "${day.dayOfMonth} de ${months[day.monthValue - 1]}$year"
}

// Lo que lee TalkBack (decisión Y): «Nota del 24 de noviembre» y, si tiene pie, «: Dijkstra paso a paso».
fun noteDescription(createdAt: Long, caption: String?, today: LocalDate, zone: ZoneId): String {
    val date = "Nota del ${noteFullDate(createdAt, today, zone)}"
    return if (caption == null) date else "$date: $caption"
}

fun notesTitle(moduleTitle: String): String = "Notas · $moduleTitle"
