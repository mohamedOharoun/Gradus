package es.ulpgc.pamn.dominio.ui.components

import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.PublishedMonth
import es.ulpgc.pamn.dominio.course.publishedMonth

// Los textos de la tarjeta de un curso (CourseCard), como funciones puras: sin Compose, para
// probarlas en el ordenador (CourseTextsTest) igual que SubmissionTexts.

private val SHORT_MONTHS = listOf(
    "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic",
)

private val LONG_MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

private const val META_SEPARATOR = " · "

// Las etiquetas que caben en la tarjeta y cuántas se quedan fuera («+2»).
data class TagsPreview(val visible: List<String>, val hidden: Int)

// «1 módulo», «20 módulos».
fun moduleCountText(count: Int): String = if (count == 1) "1 módulo" else "$count módulos"

// «oct 2026». `month` va de 1 a 12, por eso se resta uno para buscarlo en la lista.
fun PublishedMonth.shortLabel(): String = "${SHORT_MONTHS[month - 1]} $year"

// «octubre de 2026»: el mes entero, porque «oct 2026» se lee mal en voz alta (TalkBack).
fun PublishedMonth.spokenLabel(): String = "${LONG_MONTHS[month - 1]} de $year"

// «20 módulos · oct 2026», o solo «20 módulos» si la fecha no se entiende.
fun CourseSummary.metaText(): String =
    listOfNotNull(moduleCountText(moduleCount), publishedMonth?.shortLabel())
        .joinToString(META_SEPARATOR)

// Lo que lee TalkBack al enfocar la tarjeta: «Estructuras de datos, por Ana Ruiz, grafos, árboles,
// 20 módulos, publicado en octubre de 2026». Lee todas las etiquetas, también las que la tarjeta
// esconde tras el «+N», para no perder información.
fun CourseSummary.spokenText(): String =
    (
        listOf(title, "por ${author.name}") +
            tags +
            moduleCountText(moduleCount) +
            listOfNotNull(publishedMonth?.let { "publicado en ${it.spokenLabel()}" })
        ).joinToString(", ")

// Las primeras `max` etiquetas y cuántas sobran. El backend admite hasta 10 y las tarjetas
// tendrían alturas muy distintas.
fun CourseSummary.tagsPreview(max: Int = 3): TagsPreview =
    TagsPreview(visible = tags.take(max), hidden = (tags.size - max).coerceAtLeast(0))
