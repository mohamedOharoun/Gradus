package es.ulpgc.pamn.dominio.course.http

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Mastery
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.exercise.MatchPair
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MultipleChoice
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

// Codificación inversa de un Course al JSON de GET /api/courses/{id} y su ida y vuelta con decodeCourseVersion.
class CourseDtoEncoderTest {

    @Test
    fun `round trip preserves the example course from the backend`() {
        val original = decodeOk(EXAMPLE_COURSE_RESPONSE)

        val encoded = encodeCourseVersion(original)
        val decoded = decodeOk(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `round trip preserves course metadata and default mastery`() {
        val full = Course(
            id = "course-meta",
            title = "Álgebra lineal",
            modules = listOf(Module(id = "m1", title = "Vectores")),
            description = "Espacios vectoriales y matrices.",
            language = "en",
            tags = listOf("álgebra", "matemáticas"),
            defaultMastery = Mastery(questions = 5, minCorrect = 4),
        )
        val minimal = Course(
            id = "course-empty",
            title = "Vacío",
            modules = emptyList(),
            description = "",
            language = "es",
            tags = emptyList(),
            defaultMastery = Mastery(questions = 1, minCorrect = 1),
        )

        assertEquals(full, decodeOk(encodeCourseVersion(full)))
        assertEquals(minimal, decodeOk(encodeCourseVersion(minimal)))
    }

    @Test
    fun `round trip preserves lessons and all exercise kinds in steps and review`() {
        val course = Course(
            id = "all-kinds",
            title = "Todos los tipos",
            modules = listOf(
                Module(
                    id = "m1",
                    title = "Módulo completo",
                    steps = listOf(
                        Step.Lesson(
                            id = "l1",
                            text = "Texto **Markdown** con tildes áéíóú y salto\nde línea.",
                        ),
                        Step.Exercise(
                            SingleChoice(
                                id = "sc1",
                                prompt = "¿Cuál es la tercera opción?",
                                options = listOf("Primera", "Segunda", "Tercera"),
                                correct = 2,
                                explanation = "Es la tercera.",
                            ),
                        ),
                        Step.Exercise(
                            MultipleChoice(
                                id = "mc1",
                                prompt = "¿Cuáles son verdaderas?",
                                options = listOf("Uno", "Dos", "Tres"),
                                correct = setOf(2, 0),
                                explanation = "La primera y la tercera.",
                            ),
                        ),
                        Step.Exercise(
                            Matching(
                                id = "mt1",
                                prompt = "Une cada pareja.",
                                pairs = listOf(MatchPair("A", "1"), MatchPair("B", "2")),
                                explanation = "Correspondencia directa.",
                            ),
                        ),
                    ),
                    review = listOf(
                        SingleChoice(
                            id = "r-sc",
                            prompt = "¿Única correcta?",
                            options = listOf("No", "Sí"),
                            correct = 1,
                            explanation = "Sí.",
                        ),
                        MultipleChoice(
                            id = "r-mc",
                            prompt = "¿Varias correctas?",
                            options = listOf("A", "B", "C", "D"),
                            correct = setOf(3, 1),
                            explanation = "B y D.",
                        ),
                        Matching(
                            id = "r-mt",
                            prompt = "Empareja los términos.",
                            pairs = listOf(
                                MatchPair("Cola", "FIFO"),
                                MatchPair("Pila", "LIFO"),
                                MatchPair("Grafo", "Vértices y aristas"),
                            ),
                            explanation = "Definiciones básicas.",
                        ),
                    ),
                ),
            ),
        )

        assertEquals(course, decodeOk(encodeCourseVersion(course)))
    }

    @Test
    fun `round trip preserves null and explicit module mastery`() {
        val course = Course(
            id = "mastery-course",
            title = "Pruebas de dominio",
            defaultMastery = Mastery(questions = 4, minCorrect = 3),
            modules = listOf(
                Module(id = "m-default", title = "Hereda", mastery = null),
                Module(id = "m-custom", title = "Propia", mastery = Mastery(questions = 2, minCorrect = 2)),
            ),
        )

        val decoded = decodeOk(encodeCourseVersion(course))

        assertEquals(course, decoded)
        assertNull(decoded.module("m-default")!!.mastery)
        assertEquals(Mastery(questions = 2, minCorrect = 2), decoded.module("m-custom")!!.mastery)
    }

    @Test
    fun `round trip preserves module order prerequisites and empty lists`() {
        val course = Course(
            id = "graph-order",
            title = "Orden y prerrequisitos",
            modules = listOf(
                Module(id = "m1", title = "Base", requires = emptyList(), steps = emptyList(), review = emptyList()),
                Module(id = "m2", title = "Medio", requires = listOf("m1")),
                Module(id = "m3", title = "Avanzado", requires = listOf("m1", "m2")),
            ),
        )

        val decoded = decodeOk(encodeCourseVersion(course))

        assertEquals(course, decoded)
        assertEquals(listOf("m1", "m2", "m3"), decoded.modules.map { it.id })
    }

    @Test
    fun `encoded json matches the api version shape with format version one`() {
        val course = Course(
            id = "shape-id",
            title = "Forma API",
            modules = listOf(
                Module(
                    id = "m1",
                    title = "Sin mastery propia",
                    mastery = null,
                    steps = listOf(
                        Step.Lesson("l1", "Hola"),
                        Step.Exercise(SingleChoice("sc1", "P", listOf("A", "B", "C"), correct = 2, explanation = "E")),
                        Step.Exercise(MultipleChoice("mc1", "P", listOf("A", "B", "C"), correct = setOf(2, 0), explanation = "E")),
                        Step.Exercise(Matching("mt1", "P", listOf(MatchPair("L1", "R1"), MatchPair("L2", "R2")), explanation = "E")),
                    ),
                    review = listOf(
                        SingleChoice("r1", "P", listOf("A", "B"), correct = 0, explanation = "E"),
                        MultipleChoice("r2", "P", listOf("A", "B"), correct = setOf(0, 1), explanation = "E"),
                        Matching("r3", "P", listOf(MatchPair("X", "1"), MatchPair("Y", "2")), explanation = "E"),
                    ),
                ),
                Module(
                    id = "m2",
                    title = "Con mastery propia",
                    mastery = Mastery(questions = 2, minCorrect = 1),
                ),
            ),
        )

        val root = Json.parseToJsonElement(encodeCourseVersion(course)).jsonObject
        val content = root["content"]!!.jsonObject
        val modules = content["modules"]!!.jsonArray
        val firstModule = modules[0].jsonObject
        val secondModule = modules[1].jsonObject
        val steps = firstModule["steps"]!!.jsonArray
        val review = firstModule["review"]!!.jsonArray

        assertEquals("shape-id", root["id"]!!.jsonPrimitive.content)
        assertEquals(SUPPORTED_FORMAT_VERSION, content["formatVersion"]!!.jsonPrimitive.int)
        assertEquals(
            listOf("lesson", "singleChoice", "multipleChoice", "matching"),
            steps.map { it.jsonObject["type"]!!.jsonPrimitive.content },
        )
        assertEquals(
            listOf("singleChoice", "multipleChoice", "matching"),
            review.map { it.jsonObject["type"]!!.jsonPrimitive.content },
        )

        val single = steps[1].jsonObject
        assertEquals(listOf("o0", "o1", "o2"), single["options"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content })
        assertEquals("o2", single["answer"]!!.jsonPrimitive.content)

        val multiple = steps[2].jsonObject
        assertEquals(listOf("o0", "o1", "o2"), multiple["options"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content })
        assertEquals(listOf("o0", "o2"), multiple["correct"]!!.jsonArray.map { it.jsonPrimitive.content })

        assertFalse("mastery" in firstModule)
        val secondMastery = secondModule["mastery"]!!.jsonObject
        assertEquals(2, secondMastery["questions"]!!.jsonPrimitive.int)
        assertEquals(1, secondMastery["minCorrect"]!!.jsonPrimitive.int)
    }

    @Test
    fun `encoding is deterministic regardless of multiple choice correct set order`() {
        fun courseWithCorrect(correct: Set<Int>) = Course(
            id = "det",
            title = "Determinista",
            modules = listOf(
                Module(
                    id = "m1",
                    title = "M1",
                    steps = listOf(
                        Step.Exercise(
                            MultipleChoice(
                                id = "mc",
                                prompt = "Elige",
                                options = listOf("A", "B", "C", "D"),
                                correct = correct,
                                explanation = "Explicación",
                            ),
                        ),
                    ),
                ),
            ),
        )

        val reversedOrder = courseWithCorrect(linkedSetOf(3, 0, 2))
        val sortedOrder = courseWithCorrect(linkedSetOf(0, 2, 3))

        assertEquals(encodeCourseVersion(reversedOrder), encodeCourseVersion(sortedOrder))
    }

    @Test
    fun `round trip works with many options beyond the alphabet`() {
        val thirtyOptions = (0 until 30).map { "Opción $it" }
        val course = Course(
            id = "many-options",
            title = "Muchas opciones",
            modules = listOf(
                Module(
                    id = "m1",
                    title = "M1",
                    steps = listOf(
                        Step.Exercise(
                            SingleChoice(
                                id = "sc-30",
                                prompt = "Última opción",
                                options = thirtyOptions,
                                correct = 29,
                                explanation = "Índice 29.",
                            ),
                        ),
                        Step.Exercise(
                            MultipleChoice(
                                id = "mc-30",
                                prompt = "Varias",
                                options = thirtyOptions,
                                correct = setOf(0, 26, 29),
                                explanation = "Tres índices.",
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(course, decodeOk(encodeCourseVersion(course)))
    }

    private fun decodeOk(body: String): Course {
        val result = decodeCourseVersion(body)
        assertIs<CourseDecoding.Decoded>(result, result.toString())
        return result.course
    }
}
