package es.ulpgc.pamn.dominio.course.http

import es.ulpgc.pamn.dominio.course.Course
import es.ulpgc.pamn.dominio.course.Mastery
import es.ulpgc.pamn.dominio.course.Module
import es.ulpgc.pamn.dominio.course.Step
import es.ulpgc.pamn.dominio.course.exercise.Matching
import es.ulpgc.pamn.dominio.course.exercise.MultipleChoice
import es.ulpgc.pamn.dominio.course.exercise.Question
import es.ulpgc.pamn.dominio.course.exercise.SingleChoice
import kotlinx.serialization.json.jsonObject

// Codificador inverso de CourseDtoMappers: traduce un Course del modelo de la app al JSON con la
// forma de GET /api/courses/{id} para guardar copias locales. Funciones puras.
internal fun encodeCourseVersion(course: Course): String {
    val content = courseJson.encodeToJsonElement(CourseContentDto.serializer(), course.toContentDto()).jsonObject
    return courseJson.encodeToString(CourseVersionDto.serializer(), CourseVersionDto(id = course.id, content = content))
}

private fun Course.toContentDto() = CourseContentDto(
    formatVersion = SUPPORTED_FORMAT_VERSION,
    title = title,
    description = description,
    language = language,
    tags = tags,
    defaults = DefaultsDto(mastery = defaultMastery.toDto()),
    modules = modules.map { it.toDto() },
)

private fun Mastery.toDto() = MasteryDto(questions = questions, minCorrect = minCorrect)

private fun Module.toDto() = ModuleDto(
    id = id,
    title = title,
    requires = requires,
    mastery = mastery?.toDto(),
    steps = steps.map { it.toDto() },
    review = review.map { it.toDto() },
)

private fun Step.toDto(): StepDto = when (this) {
    is Step.Lesson -> LessonDto(id = id, text = text)
    is Step.Exercise -> question.toDto()
}

// En la app las opciones se referencian por posición; en el JSON necesitan un id estable ("o0", "o1"…).
private fun Question.toDto(): ExerciseDto = when (this) {
    is SingleChoice -> SingleChoiceDto(
        id = id,
        prompt = prompt,
        options = options.toOptionDtos(),
        answer = optionId(correct),
        explanation = explanation,
    )
    is MultipleChoice -> MultipleChoiceDto(
        id = id,
        prompt = prompt,
        options = options.toOptionDtos(),
        correct = correct.sorted().map(::optionId),
        explanation = explanation,
    )
    is Matching -> MatchingDto(
        id = id,
        prompt = prompt,
        pairs = pairs.map { PairDto(left = it.left, right = it.right) },
        explanation = explanation,
    )
}

private fun List<String>.toOptionDtos(): List<OptionDto> =
    mapIndexed { index, text -> OptionDto(id = optionId(index), text = text) }

private fun optionId(index: Int): String = "o$index"
