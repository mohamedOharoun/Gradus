package es.ulpgc.pamn.dominio.ui.components

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.PublishedMonth
import es.ulpgc.pamn.dominio.course.publishedMonth
import es.ulpgc.pamn.dominio.ui.text.UiText
import es.ulpgc.pamn.dominio.ui.text.longMonth
import es.ulpgc.pamn.dominio.ui.text.shortMonth

// Los textos de la tarjeta de un curso (CourseCard), como funciones puras: sin Compose, para
// probarlas en el ordenador (CourseTextsTest) igual que SubmissionTexts.
// Desde docs/tareas/ajustes.md, paso 9a, devuelven UiText: cada idioma pone sus palabras en strings.xml.
// Título, autor y etiquetas los escribe la persona: van tal cual, sin traducir.

// Las etiquetas que caben en la tarjeta y cuántas se quedan fuera («+2»).
data class TagsPreview(val visible: List<String>, val hidden: Int)

// «1 módulo», «20 módulos» (un plural: cada idioma decide la forma).
fun moduleCountText(count: Int): UiText = UiText.plural(R.plurals.course_modules, count)

// «oct 2026».
fun PublishedMonth.shortLabel(): UiText = UiText.of(R.string.month_year_short, shortMonth(month), year)

// «octubre de 2026»: el mes entero, porque «oct 2026» se lee mal en voz alta (TalkBack).
fun PublishedMonth.spokenLabel(): UiText = UiText.of(R.string.month_year_long, longMonth(month), year)

// «publicado en octubre de 2026», para TalkBack.
fun PublishedMonth.publishedText(): UiText = UiText.of(R.string.course_published, spokenLabel())

// «20 módulos · oct 2026», o solo «20 módulos» si la fecha no se entiende. Lo comparte la ficha.
fun courseMetaText(moduleCount: Int, month: PublishedMonth?): UiText =
    month?.let { UiText.of(R.string.course_meta, moduleCountText(moduleCount), it.shortLabel()) }
        ?: moduleCountText(moduleCount)

fun CourseSummary.metaText(): UiText = courseMetaText(moduleCount, publishedMonth)

// Lo que lee TalkBack al enfocar la tarjeta: «Estructuras de datos, por Ana Ruiz, grafos, árboles,
// 20 módulos, publicado en octubre de 2026». Lee todas las etiquetas, también las que la tarjeta
// esconde tras el «+N», para no perder información.
fun CourseSummary.spokenText(): UiText = joinedWithCommas(
    listOf(title, UiText.of(R.string.detail_by_author, author.name)) +
        tags +
        listOfNotNull(moduleCountText(moduleCount), publishedMonth?.publishedText()),
)

// Las primeras `max` etiquetas y cuántas sobran. El backend admite hasta 10 y las tarjetas
// tendrían alturas muy distintas.
fun CourseSummary.tagsPreview(max: Int = 3): TagsPreview =
    TagsPreview(visible = tags.take(max), hidden = (tags.size - max).coerceAtLeast(0))
