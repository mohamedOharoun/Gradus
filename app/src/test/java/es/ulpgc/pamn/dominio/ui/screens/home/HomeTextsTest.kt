package es.ulpgc.pamn.dominio.ui.screens.home

import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.TileTone
import es.ulpgc.pamn.dominio.ui.components.joinedWithCommas
import es.ulpgc.pamn.dominio.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

// Los textos de la cabecera de Inicio (docs/tareas/inicio.md, paso 4a). El patrón de la fecha lo pone cada
// idioma en strings.xml (home_date_pattern); aquí se pasan los mismos patrones que hay allí.
class HomeTextsTest {

    private val spanish = Locale.forLanguageTag("es-ES")
    private val english = Locale.forLanguageTag("en-US")
    private val spanishPattern = "EEEE, d 'de' MMMM"
    private val englishPattern = "EEEE, MMMM d"
    private val wednesday = LocalDate.of(2026, 10, 28)

    // ── La fecha ──

    // En español, Java escribe el día de la semana en minúscula: la frase empieza en mayúscula.
    @Test
    fun `the date in spanish starts with a capital letter`() {
        assertEquals("Miércoles, 28 de octubre", homeDateText(wednesday, spanishPattern, spanish))
    }

    @Test
    fun `the date in english follows the english order`() {
        assertEquals("Wednesday, October 28", homeDateText(wednesday, englishPattern, english))
    }

    // El día sin cero delante («1», no «01»).
    @Test
    fun `the day has no leading zero`() {
        assertEquals("Domingo, 1 de noviembre", homeDateText(LocalDate.of(2026, 11, 1), spanishPattern, spanish))
    }

    // ── El saludo ──

    @Test
    fun `the greeting says the name`() {
        assertEquals(UiText.of(R.string.home_greeting, "Ana"), greetingText("Ana"))
    }

    // Sin nombre (greetingName da null), solo «Hola».
    @Test
    fun `without a name the greeting is just hello`() {
        assertEquals(UiText.of(R.string.home_greeting_no_name), greetingText(null))
    }

    // ── Una fila de «Tus cursos» (paso 4b) ──

    // La letra del cuadrado: la inicial del título, en mayúscula y con su tilde.
    @Test
    fun `the tile letter is the first letter of the title`() {
        assertEquals("K", courseInitial("kotlin desde cero"))
        assertEquals("Á", courseInitial("Álgebra lineal"))
    }

    @Test
    fun `a course without title has a question mark`() {
        assertEquals("?", courseInitial("   "))
    }

    @Test
    fun `the detail says the author and how far the student is`() {
        assertEquals(
            UiText.of(R.string.home_course_detail, "Luis Pérez", UiText.of(R.string.home_course_count, 16, 20)),
            courseDetailText("Luis Pérez", masteredCount = 16, total = 20),
        )
    }

    // Un curso de ejemplo no tiene autor: solo «16 de 20».
    @Test
    fun `without an author the detail is only the count`() {
        assertEquals(UiText.of(R.string.home_course_count, 16, 20), courseDetailText(null, masteredCount = 16, total = 20))
    }

    @Test
    fun `the percent is a text of each language`() {
        assertEquals(UiText.of(R.string.home_percent, 35), percentText(35))
    }

    // TalkBack lee la fila entera: título, autor y cuánto se lleva, en palabras («conceptos dominados»).
    @Test
    fun `talkback reads the title the author and the progress`() {
        assertEquals(
            joinedWithCommas(listOf("Kotlin desde cero", "Equipo PAMN", UiText.plural(R.plurals.home_mastered_of, 20, 7, 20))),
            courseRowDescription("Kotlin desde cero", "Equipo PAMN", masteredCount = 7, total = 20),
        )
    }

    @Test
    fun `without an author talkback does not leave a gap`() {
        assertEquals(
            joinedWithCommas(listOf("Kotlin desde cero", UiText.plural(R.plurals.home_mastered_of, 20, 7, 20))),
            courseRowDescription("Kotlin desde cero", null, masteredCount = 7, total = 20),
        )
    }

    // ── El color del cuadrado ──

    // Depende del curso, no de su sitio en la lista: al subir al primer puesto no cambia de color.
    @Test
    fun `a course always gets the same tone`() {
        assertEquals(TileTone.of("kotlin"), TileTone.of("kotlin"))
    }

    // Con ids distintos salen los tres tonos (lila, ámbar y azul, como en el Figma).
    @Test
    fun `different courses use the three tones`() {
        assertEquals(TileTone.entries.toSet(), (1..30).map { TileTone.of("curso-$it") }.toSet())
    }

    // ── «Continúa donde lo dejaste» (paso 4c) ──

    // El título del módulo lo escribe el autor: va tal cual.
    @Test
    fun `the next module label says its title`() {
        assertEquals(UiText.of(R.string.home_next_module, "Grafos"), nextModuleText("Grafos"))
    }

    @Test
    fun `the progress line says how many concepts are mastered`() {
        assertEquals(UiText.plural(R.plurals.home_mastered_of, 20, 12, 20), masteredOfText(12, 20))
    }

    // TalkBack lee la tarjeta entera: título, autor, cuánto se lleva y qué toca después.
    @Test
    fun `talkback reads the whole continue card`() {
        assertEquals(
            joinedWithCommas(
                listOf("Estructuras de datos", "Ana Ruiz", masteredOfText(12, 20), nextModuleText("Grafos")),
            ),
            continueDescription("Estructuras de datos", "Ana Ruiz", masteredCount = 12, total = 20, next = "Grafos"),
        )
    }

    @Test
    fun `without an author the continue card skips it`() {
        assertEquals(
            joinedWithCommas(listOf("Estructuras de datos", masteredOfText(12, 20), nextModuleText("Grafos"))),
            continueDescription("Estructuras de datos", null, masteredCount = 12, total = 20, next = "Grafos"),
        )
    }

    // ── Cursos que no se han podido cargar (paso 4d) ──

    @Test
    fun `with every course loaded there is no notice`() {
        assertEquals(null, unavailableText(0))
    }

    // Plural: «No se ha podido cargar 1 curso» / «No se han podido cargar 2 cursos».
    @Test
    fun `the notice says how many courses could not be loaded`() {
        assertEquals(UiText.plural(R.plurals.home_unavailable, 1), unavailableText(1))
        assertEquals(UiText.plural(R.plurals.home_unavailable, 3), unavailableText(3))
    }
}

