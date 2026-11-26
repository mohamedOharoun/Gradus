package es.ulpgc.pamn.dominio.data

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.ulpgc.pamn.dominio.settings.AppSettings
import es.ulpgc.pamn.dominio.settings.PreferredTags
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// El modo oscuro guardado en el móvil (ajustes.md, tarea 3). Usa un fichero de preferencias
// propio del test, así no toca lo que haya elegido quien usa la app en el emulador.
@RunWith(AndroidJUnit4::class)
class SharedPreferencesSettingsStoreTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun clean() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = context.getSharedPreferences("settings-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @After
    fun cleanAfter() {
        prefs.edit().clear().commit()
    }

    @Test
    fun at_first_it_follows_the_system() {
        assertEquals(AppSettings(), SharedPreferencesSettingsStore(prefs).settings.value)
    }

    @Test
    fun the_chosen_dark_mode_is_seen_at_once() = runBlocking {
        val store = SharedPreferencesSettingsStore(prefs)
        store.setDarkMode(true)
        assertEquals(AppSettings(darkMode = true), store.settings.value)
        store.setDarkMode(false)
        assertEquals(AppSettings(darkMode = false), store.settings.value)
    }

    // Cerrar y abrir la app = crear otro store sobre el mismo fichero.
    @Test
    fun the_choice_survives_closing_the_app() = runBlocking {
        SharedPreferencesSettingsStore(prefs).setDarkMode(true)
        assertEquals(AppSettings(darkMode = true), SharedPreferencesSettingsStore(prefs).settings.value)
    }

    // ── Etiquetas preferidas (ajustes.md, fase 3, tarea 2) ──

    private val tags = PreferredTags.of(listOf("grafos", "kotlin", "bases de datos"))

    @Test
    fun at_first_there_are_no_preferred_tags() {
        assertEquals(PreferredTags(), SharedPreferencesSettingsStore(prefs).settings.value.preferredTags)
    }

    @Test
    fun the_preferred_tags_are_seen_at_once() = runBlocking {
        val store = SharedPreferencesSettingsStore(prefs)
        store.setPreferredTags(tags)
        assertEquals(tags, store.settings.value.preferredTags)
    }

    // Con su orden: se ven como se añadieron.
    @Test
    fun the_preferred_tags_survive_closing_the_app_in_their_order() = runBlocking {
        SharedPreferencesSettingsStore(prefs).setPreferredTags(tags)
        assertEquals(tags, SharedPreferencesSettingsStore(prefs).settings.value.preferredTags)
    }

    @Test
    fun removing_every_tag_is_remembered_too() = runBlocking {
        SharedPreferencesSettingsStore(prefs).setPreferredTags(tags)
        SharedPreferencesSettingsStore(prefs).setPreferredTags(PreferredTags())
        assertEquals(PreferredTags(), SharedPreferencesSettingsStore(prefs).settings.value.preferredTags)
    }

    // Cada preferencia va por su lado: guardar una no borra la otra.
    @Test
    fun the_dark_mode_and_the_tags_do_not_overwrite_each_other() = runBlocking {
        SharedPreferencesSettingsStore(prefs).setDarkMode(true)
        SharedPreferencesSettingsStore(prefs).setPreferredTags(tags)
        assertEquals(AppSettings(darkMode = true, preferredTags = tags), SharedPreferencesSettingsStore(prefs).settings.value)
    }

    // Si el fichero trae algo raro (tocado a mano, reglas antiguas), se limpia al leerlo.
    @Test
    fun saved_tags_are_tidied_when_read() {
        prefs.edit().putString("preferred_tags", " Grafos\n\n#grafos\nkotlin").commit()
        assertEquals(
            PreferredTags.of(listOf("grafos", "kotlin")),
            SharedPreferencesSettingsStore(prefs).settings.value.preferredTags,
        )
    }

    // «Claro» elegido no es lo mismo que «no elegido»: false se guarda y no vuelve a seguir al sistema.
    @Test
    fun choosing_light_is_remembered_too() = runBlocking {
        SharedPreferencesSettingsStore(prefs).setDarkMode(false)
        assertEquals(AppSettings(darkMode = false), SharedPreferencesSettingsStore(prefs).settings.value)
    }
}
