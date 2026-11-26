package es.ulpgc.pamn.dominio.ui.screens.tags

import es.ulpgc.pamn.dominio.settings.TagAddition

// Lo que enseña «Etiquetas preferidas» (docs/tareas/ajustes.md, fase 3).
// - `tags`: las elegidas, en su orden.
// - `draft`: lo que se está escribiendo en el campo «Nueva etiqueta».
// - `problem`: por qué no se añadió la última (Empty, AlreadyAdded o Full); null si no hay.
data class PreferredTagsUiState(
    val tags: List<String> = emptyList(),
    val draft: String = "",
    val problem: TagAddition? = null,
)

// Lo que se puede hacer en la pantalla.
class PreferredTagsActions(
    val onDraftChange: (String) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onRemove: (String) -> Unit = {},
    val onBack: () -> Unit = {},
)
