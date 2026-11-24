package es.ulpgc.pamn.dominio.ui.screens.tags

import androidx.compose.runtime.Composable
import es.ulpgc.pamn.dominio.settings.TagAddition
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

@Composable
private fun PreferredTagsPreview(state: PreferredTagsUiState) {
    GradusTheme { ScreenPreferredTags(state, PreferredTagsActions()) }
}

@LightDarkPreviews
@Composable
private fun ScreenPreferredTagsEmptyPreview() = PreferredTagsPreview(PreferredTagsUiState())

@LightDarkPreviews
@Composable
private fun ScreenPreferredTagsSomePreview() =
    PreferredTagsPreview(PreferredTagsUiState(tags = listOf("grafos", "kotlin", "algoritmos", "bases de datos")))

@LightDarkPreviews
@Composable
private fun ScreenPreferredTagsRepeatedPreview() =
    PreferredTagsPreview(PreferredTagsUiState(tags = listOf("grafos"), draft = "Grafos", problem = TagAddition.AlreadyAdded))
