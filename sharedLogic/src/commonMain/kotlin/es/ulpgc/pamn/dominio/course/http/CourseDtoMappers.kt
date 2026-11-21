package es.ulpgc.pamn.dominio.course.http

import es.ulpgc.pamn.dominio.course.Author
import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.CoursePage
import es.ulpgc.pamn.dominio.course.CourseSummary
import es.ulpgc.pamn.dominio.course.Mastery
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.exercise.MatchPair
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MultipleChoice
import es.ulpgc.pamn.dominio.course.exercise.Question
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

// Mapper: del JSON de la API de cursos al modelo de la app (course/). Funciones puras.

// Lo que sale de leer un curso descargado. Resultado sellado en vez de excepciones: quien lo usa
// (el gateway) está obligado a tratar los tres casos.
internal sealed interface CourseDecoding {
    data class Decoded(val course: Course) : CourseDecoding

    // El curso está en un formato que esta versión de la app no conoce (hay que actualizarla).
    data class UnsupportedFormat(val formatVersion: Int?) : CourseDecoding

    // El JSON no tiene la forma esperada. `reason` es para los logs, no para la pantalla.
    data class Malformed(val reason: String) : CourseDecoding
}

internal const val SUPPORTED_FORMAT_VERSION = 1

private val courseJson = Json { ignoreUnknownKeys = true }

// Lee la respuesta de GET /api/courses/{id}. En dos pasos: primero se mira formatVersion y solo
// si es el 1 se lee el resto (un formato futuro puede tener otra forma).
internal fun decodeCourseVersion(body: String): CourseDecoding = try {
    val version = courseJson.decodeFromString<CourseVersionDto>(body)
    val format = (version.content["formatVersion"] as? JsonPrimitive)?.intOrNull
    if (format != SUPPORTED_FORMAT_VERSION) {
        CourseDecoding.UnsupportedFormat(format)
    } else {
        val content = courseJson.decodeFromJsonElement(CourseContentDto.serializer(), version.content)
        CourseDecoding.Decoded(content.toCourse(id = version.id))
    }
} catch (e: IllegalArgumentException) {
    // SerializationException (JSON mal formado, campo que falta, tipo desconocido) hereda de
    // IllegalArgumentException, igual que los require del modelo (una respuesta que no existe).
    CourseDecoding.Malformed(e.message ?: "Curso ilegible")
}

// Lee una página de la búsqueda; null si no tiene la forma esperada.
internal fun decodeCoursePage(body: String): CoursePage? = try {
    courseJson.decodeFromString<CoursePageDto>(body).toCoursePage()
} catch (_: IllegalArgumentException) {
    null
}

// ── Metadatos ──

internal fun CoursePageDto.toCoursePage() =
    CoursePage(items = items.map { it.toCourseSummary() }, page = page, size = size, total = total)

internal fun CourseSummaryDto.toCourseSummary() = CourseSummary(
    id = id,
    version = version,
    author = Author(id = author.id, name = author.name),
    publishedAt = publishedAt,
    title = title,
    description = description,
    language = language,
    tags = tags,
    moduleCount = moduleCount,
)

// ── Contenido ──

// El contenido no trae id: es el de los metadatos de la versión.
internal fun CourseContentDto.toCourse(id: String) = Course(
    id = id,
    title = title,
    modules = modules.map { it.toModule() },
    description = description,
    language = language,
    tags = tags,
    defaultMastery = defaults.mastery.toMastery(),
)

private fun MasteryDto.toMastery() = Mastery(questions = questions, minCorrect = minCorrect)

private fun ModuleDto.toModule() = Module(
    id = id,
    title = title,
    requires = requires,
    steps = steps.map { it.toStep() },
    review = review.map { it.toQuestion() },
    mastery = mastery?.toMastery(),
)

private fun StepDto.toStep(): Step = when (this) {
    is LessonDto -> Step.Lesson(id = id, text = text)
    is ExerciseDto -> Step.Exercise(toQuestion())
}

// En el JSON las opciones se nombran por id ("answer": "a"); en la app, por su posición.
private fun ExerciseDto.toQuestion(): Question = when (this) {
    is SingleChoiceDto -> SingleChoice(
        id = id,
        prompt = prompt,
        options = options.map { it.text },
        correct = options.indexOfOption(answer, questionId = id),
        explanation = explanation,
    )
    is MultipleChoiceDto -> MultipleChoice(
        id = id,
        prompt = prompt,
        options = options.map { it.text },
        correct = correct.map { options.indexOfOption(it, questionId = id) }.toSet(),
        explanation = explanation,
    )
    is MatchingDto -> Matching(
        id = id,
        prompt = prompt,
        pairs = pairs.map { MatchPair(left = it.left, right = it.right) },
        explanation = explanation,
    )
}

private fun List<OptionDto>.indexOfOption(optionId: String, questionId: String): Int {
    val index = indexOfFirst { it.id == optionId }
    require(index >= 0) { "El ejercicio «$questionId» da por buena la opción «$optionId», que no existe." }
    return index
}
