package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.ulpgc.pamn.dominio.ui.navigation.TopLevelDestination
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Lo que TalkBack lee de la barra: cada pestaña es una «pestaña» con su nombre, solo una está
// seleccionada y todas tienen una zona táctil de al menos 48 dp. Se ejecuta en el emulador:
// ./gradlew :app:connectedDebugAndroidTest
@RunWith(AndroidJUnit4::class)
class GradusNavigationBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun every_tab_is_announced_as_a_tab_with_its_name_and_only_the_current_one_is_selected() {
        composeRule.setContent {
            GradusTheme { GradusNavigationBar(selected = TopLevelDestination.SEARCH, onSelect = {}) }
        }

        TopLevelDestination.entries.forEach { destination ->
            val tab = composeRule.onNodeWithText(destination.label)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
            if (destination == TopLevelDestination.SEARCH) tab.assertIsSelected() else tab.assertIsNotSelected()
        }
    }

    @Test
    fun tapping_a_tab_reports_which_one() {
        var tapped: TopLevelDestination? = null
        composeRule.setContent {
            GradusTheme { GradusNavigationBar(selected = TopLevelDestination.HOME, onSelect = { tapped = it }) }
        }

        composeRule.onNodeWithText("Mis cursos").performClick()

        assertEquals(TopLevelDestination.MY_COURSES, tapped)
    }
}
