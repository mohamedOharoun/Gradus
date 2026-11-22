package es.ulpgc.pamn.dominio.ui.screens.detail

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.PublishedMonth
import es.ulpgc.pamn.dominio.ui.components.moduleCountText
import es.ulpgc.pamn.dominio.ui.components.shortLabel
import es.ulpgc.pamn.dominio.ui.components.spokenLabel

// Los textos de la ficha de un curso, como funciones puras (se prueban en CourseDetailTextsTest).
// Reutilizan los de la tarjeta de Buscar (CourseTexts.kt, del compañero) sin cambiarlos, para que
// la ficha diga lo mismo que la tarjeta: «2 módulos · nov 2026».

private const val META_SEPARATOR = " · "

private val Course.publishedMonth: PublishedMonth?
    get() = publication?.let { PublishedMonth.parse(it.publishedAt) }

// «por Ana Ruiz», o null si no se sabe quién lo publicó.
fun Course.authorText(): String? = publication?.let { "por ${it.author.name}" }

// «2 módulos · nov 2026», o solo «2 módulos» sin fecha.
fun Course.metaText(): String =
    listOfNotNull(moduleCountText(modules.size), publishedMonth?.shortLabel()).joinToString(META_SEPARATOR)

// Lo mismo para TalkBack: «2 módulos, publicado en noviembre de 2026».
fun Course.spokenMetaText(): String =
    listOfNotNull(moduleCountText(modules.size), publishedMonth?.let { "publicado en ${it.spokenLabel()}" })
        .joinToString(", ")

// El botón de abajo, según lo que se lleve dominado (decisión C).
fun startButtonText(masteredCount: Int, total: Int = Int.MAX_VALUE): String = when {
    masteredCount <= 0 -> "Empezar el curso"
    masteredCount >= total -> "Repasar el curso"
    else -> "Continuar el curso"
}

// «1 de 2 dominados» encima del botón; null si todavía no se ha dominado nada.
fun progressText(masteredCount: Int, total: Int): String? =
    if (masteredCount <= 0) null else "$masteredCount de $total dominados"
