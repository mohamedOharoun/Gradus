package es.ulpgc.pamn.dominio.ui.screens.settings

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.settings.AppLanguage
import es.ulpgc.pamn.dominio.ui.text.UiText

// El nombre de cada idioma en Ajustes (fase 3, tarea 5b). «Español» y «English» van escritos en su propio
// idioma en los dos strings.xml: así los reconoce también quien no entiende el idioma actual de la app.
fun AppLanguage.label(): UiText = UiText.of(
    when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.SPANISH -> R.string.language_spanish
        AppLanguage.ENGLISH -> R.string.language_english
    },
)
