package es.ulpgc.pamn.dominio.ui.screens.course

import es.ulpgc.pamn.dominio.ui.components.joinedWithAnd

// Textos de los avisos del camino (en el código hasta que pasemos a strings.xml).

fun CourseNotice.toMessage(): String = when (this) {
    is CourseNotice.Locked ->
        if (missing.isEmpty()) "«${module.title}» está bloqueado."
        else "Para desbloquear «${module.title}», domina antes ${missing.map { it.title }.joinedWithAnd()}."
}
