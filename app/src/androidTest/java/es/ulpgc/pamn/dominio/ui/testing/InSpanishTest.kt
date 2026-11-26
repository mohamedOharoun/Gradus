package es.ulpgc.pamn.dominio.ui.testing

import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.text.UiText
import es.ulpgc.pamn.dominio.ui.text.asString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// La ayuda de los tests de interfaz (docs/tareas/ajustes.md, fase 3, decisión T): dentro de setContentInSpanish
// todo se pinta en español, esté el emulador en el idioma que esté (el nuestro está en inglés).
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class InSpanishTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inside_everything_says_spanish() {
        var configuration = ""
        var resources = ""
        var context = ""
        composeRule.setContentInSpanish {
            configuration = LocalConfiguration.current.locales[0].language
            resources = LocalResources.current.configuration.locales[0].language
            context = LocalContext.current.resources.configuration.locales[0].language
        }
        assertEquals("es", configuration)
        assertEquals("es", resources)
        assertEquals("es", context)
    }

    // Lo que importa: los textos de strings.xml salen de los Resources en español.
    @Test
    fun a_text_is_taken_from_the_spanish_resources() {
        composeRule.setContentInSpanish { Text(UiText.of(R.string.app_name).asString()) }
        composeRule.onNodeWithText("Dominio").assertExists()
    }

    // Fuera de la ayuda no cambia nada: el idioma es el del dispositivo.
    @Test
    fun outside_it_is_the_language_of_the_device() {
        val device = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.locales[0].language
        var shown = ""
        composeRule.setContent { shown = LocalConfiguration.current.locales[0].language }
        assertEquals(device, shown)
    }
}
