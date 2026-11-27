package es.ulpgc.pamn.dominio.ui.testing

import android.content.res.Configuration
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.test.platform.app.InstrumentationRegistry
import es.ulpgc.pamn.dominio.ui.text.ResourcesResolver
import es.ulpgc.pamn.dominio.ui.text.UiText
import es.ulpgc.pamn.dominio.ui.text.resolve
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import java.util.Locale

// Decisión T (docs/tareas/ajustes.md, fase 3): los tests de interfaz comprueban los textos en español, pero
// el emulador puede estar en otro idioma (el nuestro, en inglés). Estas ayudas pintan la pantalla con los
// Resources en español, sin cambiar el idioma del dispositivo ni el de la app.

// En lugar de setContent { … }: setContentInSpanish { … }. Y, para comprobar la traducción, en inglés.
fun ComposeContentTestRule.setContentInSpanish(content: @Composable () -> Unit) = setContent { InSpanish(content) }
fun ComposeContentTestRule.setContentInEnglish(content: @Composable () -> Unit) = setContent { InLocale(ENGLISH, content) }

@Composable
fun InSpanish(content: @Composable () -> Unit) = InLocale(SPANISH, content)

// stringResource y UiText.asString leen LocalResources; LocalContext y LocalConfiguration, por si algo
// pregunta por el idioma (fechas, plurales): los tres pasan a ser los de un Context en ese idioma.
// También conservamos LocalActivityResultRegistryOwner, porque createConfigurationContext deja de
// envolver a la ComponentActivity y rememberLauncherForActivityResult lo necesita.
@Composable
fun InLocale(locale: Locale, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val base = LocalConfiguration.current
    val activityResultOwner = LocalActivityResultRegistryOwner.current
    val localized = remember(context, base, locale) {
        val configuration = Configuration(base).apply { setLocale(locale) }
        context.createConfigurationContext(configuration) to configuration
    }
    val provided = listOfNotNull(
        LocalContext provides localized.first,
        LocalConfiguration provides localized.second,
        LocalResources provides localized.first.resources,
        activityResultOwner?.let { LocalActivityResultRegistryOwner provides it },
    ).toTypedArray()
    CompositionLocalProvider(*provided) { content() }
}

// La frase de un UiText tal como se ve en español (paso 9a), para buscarla en un test sin copiarla a mano.
fun UiText.inSpanish(): String {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val configuration = Configuration(context.resources.configuration).apply { setLocale(SPANISH) }
    return resolve(ResourcesResolver(context.createConfigurationContext(configuration).resources))
}

private val SPANISH: Locale = Locale.forLanguageTag("es-ES")
private val ENGLISH: Locale = Locale.forLanguageTag("en-US")
