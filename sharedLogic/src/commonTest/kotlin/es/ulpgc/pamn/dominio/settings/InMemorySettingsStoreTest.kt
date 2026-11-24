package es.ulpgc.pamn.dominio.settings

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Las preferencias de la app (Ajustes). Hoy, solo el modo oscuro (ajustes.md, decisión B).
class InMemorySettingsStoreTest {

    // ── AppSettings ──

    @Test
    fun `by default the dark mode is the one of the system`() {
        assertNull(AppSettings().darkMode)
        assertTrue(AppSettings().isDark(systemDark = true))
        assertFalse(AppSettings().isDark(systemDark = false))
    }

    @Test
    fun `once chosen, the dark mode does not follow the system`() {
        assertTrue(AppSettings(darkMode = true).isDark(systemDark = false))
        assertFalse(AppSettings(darkMode = false).isDark(systemDark = true))
    }

    // ── El store ──

    @Test
    fun `at first nothing has been chosen`() = runTest {
        assertEquals(AppSettings(), InMemorySettingsStore().settings.first())
    }

    @Test
    fun `the chosen dark mode is remembered`() = runTest {
        val store = InMemorySettingsStore()
        store.setDarkMode(true)
        assertEquals(AppSettings(darkMode = true), store.settings.first())
        store.setDarkMode(false)
        assertEquals(AppSettings(darkMode = false), store.settings.first())
    }

    @Test
    fun `it can start with saved settings`() = runTest {
        assertEquals(AppSettings(darkMode = true), InMemorySettingsStore(AppSettings(darkMode = true)).settings.first())
    }

    // ── Etiquetas preferidas (fase 3) ──

    @Test
    fun `at first there are no preferred tags`() {
        assertEquals(PreferredTags(), AppSettings().preferredTags)
    }

    @Test
    fun `the preferred tags are remembered without touching the dark mode`() = runTest {
        val store = InMemorySettingsStore(AppSettings(darkMode = true))
        val tags = PreferredTags.of(listOf("grafos", "kotlin"))

        store.setPreferredTags(tags)

        assertEquals(AppSettings(darkMode = true, preferredTags = tags), store.settings.value)
    }

    @Test
    fun `changing the dark mode keeps the preferred tags`() = runTest {
        val tags = PreferredTags.of(listOf("grafos"))
        val store = InMemorySettingsStore(AppSettings(preferredTags = tags))

        store.setDarkMode(true)

        assertEquals(tags, store.settings.value.preferredTags)
    }

    // La app escucha: al cambiar el interruptor, el tema cambia sin volver a preguntar.
    @Test
    fun `who listens gets each change`() = runTest {
        val store = InMemorySettingsStore()
        val seen = mutableListOf<AppSettings>()
        val listening = launch { store.settings.take(2).toList(seen) }
        testScheduler.runCurrent()
        store.setDarkMode(true)
        listening.join()
        assertEquals(listOf(AppSettings(), AppSettings(darkMode = true)), seen)
    }

    // Elegir lo mismo que ya había no vuelve a avisar (no repinta la app para nada).
    @Test
    fun `choosing the same again is not a change`() = runTest {
        val store = InMemorySettingsStore(AppSettings(darkMode = true))
        val seen = mutableListOf<AppSettings>()
        val listening = launch { store.settings.toList(seen) }
        testScheduler.runCurrent()
        store.setDarkMode(true)
        testScheduler.runCurrent()
        listening.cancel()
        assertEquals(listOf(AppSettings(darkMode = true)), seen)
    }
}
