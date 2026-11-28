package es.ulpgc.pamn.dominio.ui.screens.home

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.account.initialsOf
import es.ulpgc.pamn.dominio.ui.components.joinedWithCommas
import es.ulpgc.pamn.dominio.ui.text.UiText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Los textos de Inicio, como funciones puras (se prueban en HomeTextsTest). docs/tareas/inicio.md, paso 4.

// «Miércoles, 28 de octubre» / «Wednesday, October 28». El orden lo pone cada idioma en strings.xml
// (home_date_pattern); los nombres de días y meses, Java con el Locale de la app. En español Java los
// escribe en minúscula: la primera letra se pasa a mayúscula.
fun homeDateText(date: LocalDate, pattern: String, locale: Locale): String =
    DateTimeFormatter.ofPattern(pattern, locale).format(date).replaceFirstChar { it.titlecase(locale) }

// «Hola, Ana», o solo «Hola» sin nombre (greetingName da null).
fun greetingText(name: String?): UiText =
    if (name == null) UiText.of(R.string.home_greeting_no_name) else UiText.of(R.string.home_greeting, name)

// ── Una fila de «Tus cursos» (paso 4b) ──

// La letra del cuadrado: la inicial del título («Álgebra lineal» → «Á»), o «?» sin título. Reutiliza
// initialsOf (la de las iniciales del avatar) y se queda con la primera.
fun courseInitial(title: String): String = initialsOf(title).take(1)

// «Luis Pérez · 16 de 20», o solo «16 de 20» si el curso no tiene autor (cursos de ejemplo).
fun courseDetailText(author: String?, masteredCount: Int, total: Int): UiText {
    val count = UiText.of(R.string.home_course_count, masteredCount, total)
    return if (author == null) count else UiText.of(R.string.home_course_detail, author, count)
}

// «35 %» en español, «35%» en inglés.
fun percentText(percent: Int): UiText = UiText.of(R.string.home_percent, percent)

// Lo que lee TalkBack en una fila: «Kotlin desde cero, Equipo PAMN, 7 de 20 conceptos dominados». La
// cuenta va en palabras (plural según el total); el porcentaje no se lee, porque dice lo mismo.
fun courseRowDescription(title: String, author: String?, masteredCount: Int, total: Int): UiText =
    joinedWithCommas(listOfNotNull(title, author, masteredOfText(masteredCount, total)))

// «12 de 20 conceptos dominados» (plural según el total): bajo el título de «Continúa» y en TalkBack.
fun masteredOfText(masteredCount: Int, total: Int): UiText =
    UiText.plural(R.plurals.home_mastered_of, total, masteredCount, total)

// ── «Continúa donde lo dejaste» (paso 4c) ──

// «Siguiente: Grafos». El título del módulo lo escribe el autor: va tal cual.
fun nextModuleText(moduleTitle: String): UiText = UiText.of(R.string.home_next_module, moduleTitle)

// Lo que lee TalkBack en la tarjeta: «Estructuras de datos, Ana Ruiz, 12 de 20 conceptos dominados,
// Siguiente: Grafos». La acción («Continuar») la añade la tarjeta.
fun continueDescription(title: String, author: String?, masteredCount: Int, total: Int, next: String): UiText =
    joinedWithCommas(listOfNotNull(title, author, masteredOfText(masteredCount, total), nextModuleText(next)))

// ── La pantalla entera (paso 4d) ──

// «No se ha podido cargar 1 curso. Comprueba la conexión.», o null si se han cargado todos.
fun unavailableText(count: Int): UiText? = if (count <= 0) null else UiText.plural(R.plurals.home_unavailable, count)

