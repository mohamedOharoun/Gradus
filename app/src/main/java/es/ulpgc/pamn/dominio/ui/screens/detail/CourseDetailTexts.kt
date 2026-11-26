package es.ulpgc.pamn.dominio.ui.screens.detail

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.PublishedMonth
import es.ulpgc.pamn.dominio.ui.components.courseMetaText
import es.ulpgc.pamn.dominio.ui.components.joinedWithCommas
import es.ulpgc.pamn.dominio.ui.components.moduleCountText
import es.ulpgc.pamn.dominio.ui.components.publishedText
import es.ulpgc.pamn.dominio.ui.text.UiText

// Los textos de la ficha de un curso, como funciones puras (se prueban en CourseDetailTextsTest).
// Reutilizan los de la tarjeta de Buscar (CourseTexts.kt, del compañero), para que la ficha diga lo
// mismo que la tarjeta: «2 módulos · nov 2026».

private val Course.publishedMonth: PublishedMonth?
    get() = publication?.let { PublishedMonth.parse(it.publishedAt) }

// «por Ana Ruiz», o null si no se sabe quién lo publicó. El nombre va como dato, sin traducir.
fun Course.authorText(): UiText? = publication?.let { UiText.of(R.string.detail_by_author, it.author.name) }

// «2 módulos · nov 2026», o solo «2 módulos» sin fecha.
fun Course.metaText(): UiText = courseMetaText(modules.size, publishedMonth)

// Lo mismo para TalkBack: «2 módulos, publicado en noviembre de 2026».
fun Course.spokenMetaText(): UiText =
    joinedWithCommas(listOfNotNull(moduleCountText(modules.size), publishedMonth?.publishedText()))

// El botón de abajo, según lo que se lleve dominado (decisión C).
fun startButtonText(masteredCount: Int, total: Int = Int.MAX_VALUE): UiText = when {
    masteredCount <= 0 -> UiText.of(R.string.detail_start)
    masteredCount >= total -> UiText.of(R.string.detail_review)
    else -> UiText.of(R.string.detail_continue)
}

// «1 de 2 dominados» encima del botón; null si todavía no se ha dominado nada.
fun progressText(masteredCount: Int, total: Int): UiText? =
    if (masteredCount <= 0) null else UiText.of(R.string.course_progress, masteredCount, total)
