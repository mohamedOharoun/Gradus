package es.ulpgc.pamn.dominio.ui.screens.course

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.joinedWithAnd
import es.ulpgc.pamn.dominio.ui.text.UiText

// Textos de los avisos del camino (UiText, fase 3, paso 8a). Los títulos de los módulos son del autor:
// van como datos, sin traducir.
fun CourseNotice.toMessage(): UiText = when (this) {
    is CourseNotice.Locked ->
        if (missing.isEmpty()) UiText.of(R.string.course_locked, module.title)
        else UiText.of(R.string.course_locked_missing, module.title, joinedWithAnd(missing.map { it.title }))
    CourseNotice.NotEnrolled -> UiText.of(R.string.course_not_enrolled)
}
