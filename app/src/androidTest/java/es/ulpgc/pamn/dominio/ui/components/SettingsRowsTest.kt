package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Las filas de Ajustes (ajustes.md, tarea 4): cómo se tocan y qué lee TalkBack.
// Se ejecuta en el emulador: ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsRowsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(content: @Composable () -> Unit) =
        composeRule.setContent { GradusTheme { content() } }

    // La fila entera, en el árbol fusionado (el que ve TalkBack).
    private fun row() = composeRule.onNode(hasClickAction())

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    // ── SettingsRow ──

    @Test
    fun a_row_is_a_single_button_with_its_text_and_value() {
        show { SettingsRow(icon = R.drawable.ic_language, label = "Idioma", value = "Español", onClick = {}) }

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        row().assert(hasRole(Role.Button)).assert(hasText("Idioma")).assert(hasText("Español"))
    }

    @Test
    fun touching_a_row_calls_its_action() {
        var clicks = 0
        show { SettingsRow(icon = R.drawable.ic_logout, label = "Cerrar sesión", onClick = { clicks++ }) }

        row().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun a_row_is_at_least_48dp_tall() {
        show { SettingsRow(icon = R.drawable.ic_logout, label = "Cerrar sesión", onClick = {}) }

        row().assertHeightIsAtLeast(48.dp)
    }

    // Los iconos (el de la izquierda y ›) son decorativos: TalkBack no los lee.
    @Test
    fun the_icons_are_not_read() {
        show { SettingsRow(icon = R.drawable.ic_language, label = "Idioma", value = "Español", onClick = {}) }

        composeRule.onAllNodes(hasContentDescription("", substring = true), useUnmergedTree = true).assertCountEquals(0)
    }

    // Decisión A: lo que aún no funciona se ve, pero desactivado y con «Próximamente» en lugar del valor.
    @Test
    fun a_disabled_row_says_coming_soon_instead_of_its_value() {
        show { SettingsRow(icon = R.drawable.ic_language, label = "Idioma", value = "Español", onClick = {}, enabled = false) }

        composeRule.onNodeWithText(COMING_SOON).assertExists()
        composeRule.onNodeWithText("Español").assertDoesNotExist()
    }

    @Test
    fun a_disabled_row_cannot_be_touched() {
        var clicks = 0
        show { SettingsRow(icon = R.drawable.ic_delete, label = "Eliminar cuenta", onClick = { clicks++ }, enabled = false) }

        row().assertIsNotEnabled().performClick()

        assertEquals(0, clicks)
    }

    @Test
    fun a_row_without_value_shows_no_coming_soon_while_enabled() {
        show { SettingsRow(icon = R.drawable.ic_logout, label = "Cerrar sesión", onClick = {}) }

        row().assertIsEnabled()
        composeRule.onNodeWithText(COMING_SOON).assertDoesNotExist()
    }

    // ── SettingsSwitchRow ──

    @Test
    fun the_switch_row_is_read_as_a_switch_with_its_state() {
        show { SettingsSwitchRow(icon = R.drawable.ic_dark_mode, label = "Modo oscuro", checked = true, onCheckedChange = {}) }

        composeRule.onNode(hasRole(Role.Switch))
            .assert(hasText("Modo oscuro"))
            .assertIsOn()
    }

    @Test
    fun the_switch_row_shows_off_when_unchecked() {
        show { SettingsSwitchRow(icon = R.drawable.ic_dark_mode, label = "Modo oscuro", checked = false, onCheckedChange = {}) }

        composeRule.onNode(hasRole(Role.Switch)).assertIsOff()
    }

    // Se pulsa en cualquier parte de la fila (aquí, en el texto), no solo en el interruptor.
    @Test
    fun touching_anywhere_on_the_row_asks_for_the_opposite_value() {
        val asked = mutableListOf<Boolean>()
        show { SettingsSwitchRow(icon = R.drawable.ic_dark_mode, label = "Modo oscuro", checked = false, onCheckedChange = { asked += it }) }

        composeRule.onNodeWithText("Modo oscuro").performClick()

        assertEquals(listOf(true), asked)
    }

    // Un solo elemento: el Switch de dentro no es otro elemento pulsable aparte.
    @Test
    fun the_switch_row_is_a_single_element_at_least_48dp_tall() {
        show { SettingsSwitchRow(icon = R.drawable.ic_dark_mode, label = "Modo oscuro", checked = true, onCheckedChange = {}) }

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        composeRule.onNode(hasRole(Role.Switch)).assertHeightIsAtLeast(48.dp)
    }

    // ── ProfileRow ──

    @Test
    fun the_profile_shows_initials_name_and_email() {
        show { ProfileRow(name = "Mohamed Oharoun", email = "nombre@ejemplo.com", onEdit = {}) }

        composeRule.onNodeWithText("MO", useUnmergedTree = true).assertExists()
        row().assert(hasText("Mohamed Oharoun")).assert(hasText("nombre@ejemplo.com")).assert(hasText("Editar"))
    }

    // TalkBack lee el nombre entero, no las iniciales sueltas.
    @Test
    fun talkback_does_not_read_the_initials() {
        show { ProfileRow(name = "Mohamed Oharoun", email = "nombre@ejemplo.com", onEdit = {}) }

        row().assert(hasText("MO").not())
    }

    @Test
    fun touching_the_profile_calls_edit() {
        var edits = 0
        show { ProfileRow(name = "Ana", email = "ana@ejemplo.com", onEdit = { edits++ }) }

        row().assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, edits)
    }

    @Test
    fun when_editing_does_not_work_yet_the_profile_says_coming_soon() {
        var edits = 0
        show { ProfileRow(name = "Ana", email = "ana@ejemplo.com", onEdit = { edits++ }, editEnabled = false) }

        composeRule.onNodeWithText("Ana", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(COMING_SOON, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Editar", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("Ana").performClick()
        assertEquals(0, edits)
    }

    // ── SettingsSection ──

    @Test
    fun the_section_title_is_an_uppercase_heading() {
        show {
            SettingsSection(title = "Sesión") {
                SettingsRow(icon = R.drawable.ic_logout, label = "Cerrar sesión", onClick = {})
            }
        }

        composeRule.onNode(isHeading()).assert(hasText("SESIÓN"))
    }
}
