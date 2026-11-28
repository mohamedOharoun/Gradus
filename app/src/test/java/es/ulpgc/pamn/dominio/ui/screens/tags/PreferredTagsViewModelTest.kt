package es.ulpgc.pamn.dominio.ui.screens.tags

import es.ulpgc.pamn.dominio.settings.AppSettings
import es.ulpgc.pamn.dominio.settings.InMemorySettingsStore
import es.ulpgc.pamn.dominio.settings.PreferredTags
import es.ulpgc.pamn.dominio.settings.TagAddition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// «Etiquetas preferidas» (docs/tareas/ajustes.md, fase 3, tarea 3): añadir y quitar, y se guardan al
// momento (no hay botón «Guardar»). Tiempo simulado: nada corre hasta runCurrent().
@OptIn(ExperimentalCoroutinesApi::class)
class PreferredTagsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.opened(vararg saved: String): Pair<PreferredTagsViewModel, InMemorySettingsStore> {
        val store = InMemorySettingsStore(AppSettings(preferredTags = PreferredTags.of(saved.toList())))
        val viewModel = PreferredTagsViewModel(store)
        runCurrent()
        return viewModel to store
    }

    private fun TestScope.add(viewModel: PreferredTagsViewModel, raw: String) {
        viewModel.onDraftChange(raw)
        viewModel.onAdd()
        runCurrent()
    }

    @Test
    fun `it shows the saved tags`() = runTest {
        val (viewModel, _) = opened("grafos", "kotlin")
        assertEquals(PreferredTagsUiState(tags = listOf("grafos", "kotlin")), viewModel.state)
    }

    @Test
    fun `typing changes the draft`() = runTest {
        val (viewModel, _) = opened()
        viewModel.onDraftChange("redes")
        assertEquals("redes", viewModel.state.draft)
    }

    // ── Añadir ──

    @Test
    fun `adding saves it at once and clears the draft`() = runTest {
        val (viewModel, store) = opened("grafos")

        add(viewModel, "#Kotlin")

        assertEquals(listOf("grafos", "kotlin"), store.settings.value.preferredTags.values)
        assertEquals(PreferredTagsUiState(tags = listOf("grafos", "kotlin")), viewModel.state)
    }

    @Test
    fun `an empty tag is not added and says so`() = runTest {
        val (viewModel, store) = opened()

        add(viewModel, " # ")

        assertEquals(TagAddition.Empty, viewModel.state.problem)
        assertEquals(PreferredTags(), store.settings.value.preferredTags)
    }

    // El borrador se conserva para corregirlo.
    @Test
    fun `a repeated tag is not added and the draft stays`() = runTest {
        val (viewModel, _) = opened("grafos")

        add(viewModel, "GRAFOS")

        assertEquals(TagAddition.AlreadyAdded, viewModel.state.problem)
        assertEquals("GRAFOS", viewModel.state.draft)
    }

    @Test
    fun `with ten tags no more are added`() = runTest {
        val (viewModel, store) = opened(*Array(PreferredTags.MAX) { "tema$it" })

        add(viewModel, "otra")

        assertEquals(TagAddition.Full, viewModel.state.problem)
        assertEquals(PreferredTags.MAX, store.settings.value.preferredTags.values.size)
    }

    @Test
    fun `typing again clears the problem`() = runTest {
        val (viewModel, _) = opened("grafos")
        add(viewModel, "grafos")

        viewModel.onDraftChange("grafo")

        assertNull(viewModel.state.problem)
    }

    // ── Quitar ──

    @Test
    fun `removing a tag saves it at once`() = runTest {
        val (viewModel, store) = opened("grafos", "kotlin")

        viewModel.onRemove("grafos")
        runCurrent()

        assertEquals(listOf("kotlin"), store.settings.value.preferredTags.values)
        assertEquals(listOf("kotlin"), viewModel.state.tags)
    }

    // Quitar una deja sitio: «máximo 10» ya no aplica.
    @Test
    fun `removing a tag clears the problem`() = runTest {
        val (viewModel, _) = opened(*Array(PreferredTags.MAX) { "tema$it" })
        add(viewModel, "otra")

        viewModel.onRemove("tema0")
        runCurrent()

        assertNull(viewModel.state.problem)
    }
}
