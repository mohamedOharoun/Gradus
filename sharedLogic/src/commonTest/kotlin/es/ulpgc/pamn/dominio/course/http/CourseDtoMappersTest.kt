package es.ulpgc.pamn.dominio.course.http

import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.Mastery
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.exercise.MatchPair
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MultipleChoice
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Del JSON del backend al modelo de la app. El curso de ejemplo es el real del backend.
class CourseDtoMappersTest {

    private val example: Course = decodeOk(EXAMPLE_COURSE_RESPONSE)
    private val representation = example.module("representacion")!!
    private val bfs = example.module("bfs")!!

    // ── El curso de ejemplo del backend ──

    @Test
    fun `the course takes its id from the metadata and the rest from its content`() {
        assertEquals("0b8f2c1e-0000-4000-8000-000000000001", example.id)
        assertEquals("Introducción a los grafos", example.title)
        assertEquals("Cómo se guarda un grafo en memoria y cómo se recorre en anchura (BFS).", example.description)
        assertEquals("es", example.language)
        assertEquals(listOf("grafos", "algoritmos", "estructuras de datos"), example.tags)
        assertEquals(Mastery(questions = 4, minCorrect = 3), example.defaultMastery)
    }

    @Test
    fun `the modules keep their order and their prerequisites`() {
        assertEquals(listOf("representacion", "bfs"), example.modules.map { it.id })
        assertEquals(emptyList(), representation.requires)
        assertEquals(listOf("representacion"), bfs.requires)
        assertEquals("Recorrido en anchura (BFS)", bfs.title)
    }

    @Test
    fun `the steps keep their order, mixing lessons and exercises`() {
        assertEquals(listOf("bfs-l1", "bfs-f1", "bfs-l2", "bfs-f2"), bfs.steps.map { it.id })
        assertIs<Step.Lesson>(bfs.steps[0])
        assertIs<Step.Exercise>(bfs.steps[1])
    }

    @Test
    fun `a lesson keeps its Markdown text`() {
        val lesson = representation.steps.first() as Step.Lesson
        assertTrue(lesson.text.startsWith("Un **grafo** G = (V, E)"))
    }

    @Test
    fun `the answer id of a single choice becomes the index of its option`() {
        val question = representation.exercises.first() as SingleChoice
        assertEquals(listOf("Matriz de adyacencia", "Lista de adyacencia", "Lista de aristas"), question.options)
        assertEquals(0, question.correct)
    }

    @Test
    fun `the correct ids of a multiple choice become the indices of their options`() {
        val question = representation.review.first { it.id == "repr-r4" } as MultipleChoice
        assertEquals(setOf(0, 2), question.correct)
    }

    @Test
    fun `a matching keeps its pairs`() {
        val question = representation.exercises[1] as Matching
        assertEquals(
            listOf(MatchPair("Matriz de adyacencia", "O(V²)"), MatchPair("Lista de adyacencia", "O(V + E)")),
            question.pairs,
        )
    }

    @Test
    fun `every exercise keeps its prompt and its explanation`() {
        val question = bfs.exercises.first()
        assertEquals("¿Qué estructura de datos usa BFS para decidir qué vértice examinar a continuación?", question.prompt)
        assertTrue(question.explanation.startsWith("La cola es FIFO"))
    }

    @Test
    fun `the review pool is kept apart from the steps`() {
        assertEquals((1..8).map { "repr-r$it" }, representation.review.map { it.id })
        assertEquals(2, representation.exercises.size)
    }

    @Test
    fun `a module without its own mastery has none`() {
        assertNull(bfs.mastery)
    }

    // ── Cursos pequeños escritos aquí ──

    @Test
    fun `an answer that is not the first option gets its own index`() {
        val question = decodeOk(courseWith(exercise = SINGLE_CHOICE_ANSWER_C)).questionsFor("m1").single() as SingleChoice
        assertEquals(2, question.correct)
    }

    @Test
    fun `a module with its own mastery keeps it`() {
        val course = decodeOk(courseWith(moduleExtra = """"mastery": {"questions": 2, "minCorrect": 2},"""))
        assertEquals(Mastery(questions = 2, minCorrect = 2), course.module("m1")!!.mastery)
    }

    // ── Lo que la app no sabe leer: error claro, sin excepciones ──

    @Test
    fun `another format version is unsupported`() {
        val content = """{"formatVersion": 2, "title": "Del futuro", "modules": "otra forma"}"""
        assertEquals(CourseDecoding.UnsupportedFormat(2), decodeCourseVersion(response(content)))
    }

    @Test
    fun `an unknown kind of exercise is malformed`() {
        val unknown = """{"type": "dragAndDrop", "id": "x1", "prompt": "Arrastra"}"""
        assertIs<CourseDecoding.Malformed>(decodeCourseVersion(courseWith(exercise = unknown)))
    }

    @Test
    fun `an answer that is not one of the options is malformed`() {
        val wrongAnswer = SINGLE_CHOICE_ANSWER_C.replace(""""answer": "c"""", """"answer": "z"""")
        val result = decodeCourseVersion(courseWith(exercise = wrongAnswer))
        assertIs<CourseDecoding.Malformed>(result)
        assertTrue("«z»" in result.reason, result.reason)
    }

    @Test
    fun `a lesson in the review pool is malformed`() {
        val lessonInReview = """{"type": "lesson", "id": "l9", "text": "Texto"}"""
        assertIs<CourseDecoding.Malformed>(decodeCourseVersion(courseWith(review = lessonInReview)))
    }

    @Test
    fun `a body that is not JSON is malformed`() {
        assertIs<CourseDecoding.Malformed>(decodeCourseVersion("<html>Error</html>"))
    }

    // ── Metadatos y página de la búsqueda ──

    @Test
    fun `a search page maps every result with its metadata`() {
        val page = decodeCoursePage(SEARCH_PAGE)
        assertEquals(
            CourseSummary(
                id = "c1", version = 2, author = Author("u1", "Ana"), publishedAt = "2026-11-18T08:35:00Z",
                title = "Introducción a los grafos", description = "Cómo se guarda un grafo.", language = "es",
                tags = listOf("grafos", "algoritmos"), moduleCount = 2,
            ),
            page!!.items.single(),
        )
        assertEquals(0, page.page)
        assertEquals(21, page.total)
    }

    @Test
    fun `a page knows if there are more results after it`() {
        assertTrue(decodeCoursePage(SEARCH_PAGE)!!.hasMore)
        assertFalse(decodeCoursePage(SEARCH_PAGE.replace(""""total": 21""", """"total": 1"""))!!.hasMore)
    }

    @Test
    fun `a search page that cannot be read gives null`() {
        assertNull(decodeCoursePage("""{"items": "no es una lista"}"""))
    }

    // ── Ayudas ──

    private fun decodeOk(body: String): Course {
        val result = decodeCourseVersion(body)
        assertIs<CourseDecoding.Decoded>(result, result.toString())
        return result.course
    }

    private fun response(content: String) = """{"id": "c1", "version": 1, "content": $content}"""

    // Un curso de un módulo («m1») con un ejercicio en sus pasos y otro en su reserva.
    private fun courseWith(
        exercise: String = SINGLE_CHOICE_ANSWER_C,
        review: String = SINGLE_CHOICE_ANSWER_C.replace("f1", "r1"),
        moduleExtra: String = "",
    ) = response(
        """
        {"formatVersion": 1, "title": "Pequeño", "description": "", "language": "es", "tags": [],
         "defaults": {"mastery": {"questions": 1, "minCorrect": 1}},
         "modules": [{"id": "m1", "title": "Módulo", "requires": [], $moduleExtra
                      "steps": [$exercise], "review": [$review]}]}
        """,
    )

    private companion object {
        const val SINGLE_CHOICE_ANSWER_C = """
            {"type": "singleChoice", "id": "f1", "prompt": "¿Cuál?",
             "options": [{"id": "a", "text": "Uno"}, {"id": "b", "text": "Dos"}, {"id": "c", "text": "Tres"}],
             "answer": "c", "explanation": "Porque sí."}
        """

        const val SEARCH_PAGE = """
            {"items": [{"id": "c1", "version": 2, "author": {"id": "u1", "name": "Ana"},
                        "publishedAt": "2026-11-18T08:35:00Z", "formatVersion": 1,
                        "title": "Introducción a los grafos", "description": "Cómo se guarda un grafo.",
                        "language": "es", "tags": ["grafos", "algoritmos"], "moduleCount": 2, "sha256": "9c1e"}],
             "page": 0, "size": 20, "total": 21}
        """
    }
}
