package es.ulpgc.pamn.dominio.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Test

// Tarea 7b de buscar-cursos.md: los textos de los filtros, como funciones puras (sin Compose).
class FilterTextsTest {

    // --- Textos fijos ---

    @Test
    fun `the filters button is called Filtros`() {
        assertEquals("Filtros", FILTERS_LABEL)
    }

    @Test
    fun `with no tags to choose the sheet says so`() {
        assertEquals("No hay etiquetas que elegir", NO_TAGS_TEXT)
    }

    // --- filtersButtonDescription (lo que lee TalkBack) ---

    @Test
    fun `without filters the button is just Filtros`() {
        assertEquals("Filtros", filtersButtonDescription(0))
    }

    @Test
    fun `one filter is singular`() {
        assertEquals("Filtros, 1 activo", filtersButtonDescription(1))
    }

    @Test
    fun `several filters are plural`() {
        assertEquals("Filtros, 2 activos", filtersButtonDescription(2))
        assertEquals("Filtros, 12 activos", filtersButtonDescription(12))
    }

    @Test
    fun `a negative count is read as no filters`() {
        assertEquals("Filtros", filtersButtonDescription(-1))
    }

    // --- tagChipLabel ---

    @Test
    fun `the chip label puts a hash before the tag`() {
        assertEquals("#grafos", tagChipLabel("grafos"))
    }

    @Test
    fun `the chip label keeps capitals, accents and inner spaces`() {
        assertEquals("#Bases de datos", tagChipLabel("Bases de datos"))
        assertEquals("#árboles", tagChipLabel("árboles"))
    }

    // --- removeFilterDescription (lo que lee TalkBack) ---

    @Test
    fun `removing a filter is read without the hash`() {
        assertEquals("Quitar filtro grafos", removeFilterDescription("grafos"))
    }

    @Test
    fun `removing a filter keeps inner spaces`() {
        assertEquals("Quitar filtro bases de datos", removeFilterDescription("bases de datos"))
    }
}
