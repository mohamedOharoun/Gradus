package es.ulpgc.pamn.dominio.ui.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Un texto de la interfaz que se traduce (docs/tareas/ajustes.md, fase 3, decisión U): qué texto es (su
// id en strings.xml) y sus datos. Aquí, con un traductor falso: las plantillas son de mentira.
class UiTextTest {

    private val greeting = 1
    private val stepOf = 2
    private val questions = 3
    private val submittedOn = 4

    private val resolved = mutableListOf<Int>()

    // Como Resources: plantillas con %1$s / %1$d y un plural «una / varias».
    private val resolver = object : StringResolver {
        override fun string(id: Int, args: List<Any>): String {
            resolved += id
            val template = when (id) {
                greeting -> "Hola"
                stepOf -> "Paso %1\$d de %2\$d"
                submittedOn -> "Entregada el %1\$s"
                else -> error("id desconocido $id")
            }
            return template.format(*args.toTypedArray())
        }

        override fun plural(id: Int, count: Int, args: List<Any>): String {
            resolved += id
            check(id == questions)
            return (if (count == 1) "%1\$d pregunta" else "%1\$d preguntas").format(*args.toTypedArray())
        }
    }

    @Test
    fun `a text without data is its template`() {
        assertEquals("Hola", UiText.of(greeting).resolve(resolver))
    }

    @Test
    fun `the data fill the template in order`() {
        assertEquals("Paso 2 de 5", UiText.of(stepOf, 2, 5).resolve(resolver))
    }

    // Los plurales eligen la forma por la cantidad, que por defecto también es el dato.
    @Test
    fun `a plural picks its form by the count`() {
        assertEquals("1 pregunta", UiText.plural(questions, 1).resolve(resolver))
        assertEquals("3 preguntas", UiText.plural(questions, 3).resolve(resolver))
    }

    // Un dato puede ser otro texto que también se traduce (p. ej. una fecha): se resuelve primero.
    @Test
    fun `a datum that is a text is translated too`() {
        val nested = UiText.of(submittedOn, UiText.of(greeting))
        assertEquals("Entregada el Hola", nested.resolve(resolver))
    }

    // Decisión R: lo que escribe el autor del curso no se traduce; ni se pide al traductor.
    @Test
    fun `a raw text is shown as it is`() {
        assertEquals("Recorrido en anchura", UiText.raw("Recorrido en anchura").resolve(resolver))
        assertTrue(resolved.isEmpty())
    }

    @Test
    fun `a raw text can be a datum of a translated one`() {
        assertEquals("Entregada el 25 nov", UiText.of(submittedOn, UiText.raw("25 nov")).resolve(resolver))
    }

    // Los tests de las funciones de textos comparan UiText: mismo id y mismos datos → iguales.
    @Test
    fun `two texts with the same id and data are equal`() {
        assertEquals(UiText.of(stepOf, 2, 5), UiText.of(stepOf, 2, 5))
        assertNotEquals(UiText.of(stepOf, 2, 5), UiText.of(stepOf, 3, 5))
        assertNotEquals(UiText.of(stepOf), UiText.of(greeting))
        assertEquals(UiText.plural(questions, 3), UiText.plural(questions, 3))
    }
}
