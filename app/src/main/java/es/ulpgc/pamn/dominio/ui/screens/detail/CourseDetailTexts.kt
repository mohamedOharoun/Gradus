package es.ulpgc.pamn.dominio.ui.screens.detail

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.PublishedMonth
import es.ulpgc.pamn.dominio.ui.components.courseMetaText
import es.ulpgc.pamn.dominio.ui.components.joinedWithCommas
import es.ulpgc.pamn.dominio.ui.components.moduleCountText
import es.ulpgc.pamn.dominio.ui.components.publishedText
import es.ulpgc.pamn.dominio.ui.components.shortLabel
import es.ulpgc.pamn.dominio.ui.text.UiText

// Los textos de la ficha de un curso, como funciones puras (se prueban en CourseDetailTextsTest).
// Reutilizan los de la tarjeta de Buscar (CourseTexts.kt, del compañero), para que la ficha diga lo
// mismo que la tarjeta: «2 módulos · nov 2026».

private val Course.publishedMonth: PublishedMonth?
    get() = publication?.let { PublishedMonth.parse(it.publishedAt) }

// «por Ana Ruiz», o null si no se sabe quién lo publicó. El nombre va como dato, sin traducir.
fun Course.authorText(): UiText? = publication?.let { UiText.of(R.string.detail_by_author, it.author.name) }

// Etiqueta bajo el número de módulos en las estadísticas (no inscrito): «módulo» / «módulos».
fun modulesStatLabel(count: Int): UiText = UiText.plural(R.plurals.detail_modules_label, count)

// Fecha corta para la estadística de publicación («nov 2026»), o null si no hay fecha válida.
fun Course.publishedShortDate(): UiText? = publishedMonth?.shortLabel()

// Línea visual de publicación en la vista inscrita («Publicado en nov 2026»), o null sin fecha válida.
fun Course.publishedInText(): UiText? = publishedMonth?.let { UiText.of(R.string.detail_published_in, it.shortLabel()) }

// Versión hablada para TalkBack con mes completo («publicado en noviembre de 2026»), o null.
fun Course.spokenPublishedText(): UiText? = publishedMonth?.publishedText()

// Porcentaje del bloque de progreso («43 %» en ES, «43%» en EN).
fun progressPercentText(percent: Int): UiText = UiText.of(R.string.detail_progress_percent, percent)

// Resumen bajo la barra de progreso: «6 de 14 módulos dominados» / «1 de 1 módulo dominado».
fun masteredModulesText(masteredCount: Int, total: Int): UiText =
    UiText.plural(R.plurals.detail_progress_modules, total, masteredCount, total)

// Texto del botón inferior según el estado de inscripción.
fun ctaButtonText(enrolled: Boolean, enrolling: Boolean): UiText = when {
    enrolled -> UiText.of(R.string.detail_go_to_course)
    enrolling -> UiText.of(R.string.enroll_enrolling)
    else -> UiText.of(R.string.detail_enroll)
}

// Texto del botón destructivo del diálogo de baja según su estado.
fun UnenrollDialogState.confirmButtonText(): UiText = when (this) {
    UnenrollDialogState.Sending -> UiText.of(R.string.detail_unenrolling)
    UnenrollDialogState.Closed,
    UnenrollDialogState.Confirming,
    is UnenrollDialogState.Failed,
    -> UiText.of(R.string.detail_unenroll)
}

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
