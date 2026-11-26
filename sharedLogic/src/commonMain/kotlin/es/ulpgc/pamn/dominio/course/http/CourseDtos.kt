package es.ulpgc.pamn.dominio.course.http

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// DTO: el JSON de la API de cursos (backend/docs/api-cursos.md), campo a campo. No son el modelo de
// la app: si el JSON cambia, cambian estas clases y su mapper (CourseDtoMappers.kt), nada más.
// Los campos que no se usan (formatVersion de los metadatos, sha256) no se declaran y se ignoran.

// ── Metadatos y búsqueda ──

@Serializable
internal data class AuthorDto(val id: String, val name: String)

// «Metadatos de una versión»: cada resultado de la búsqueda.
@Serializable
internal data class CourseSummaryDto(
    val id: String,
    val version: Int,
    val author: AuthorDto,
    val publishedAt: String,
    val title: String,
    val description: String,
    val language: String,
    val tags: List<String>,
    val moduleCount: Int,
)

// «Página de la búsqueda».
@Serializable
internal data class CoursePageDto(
    val items: List<CourseSummaryDto>,
    val page: Int,
    val size: Int,
    val total: Int,
)

// «Versión con contenido» (GET /api/courses/{id}). El contenido se guarda sin leer (JsonObject)
// porque antes hay que mirar su formatVersion: un formato futuro puede tener otra forma.
// `version`, `author` y `publishedAt` son opcionales aquí: el backend siempre los manda, pero sin
// ellos el curso se puede estudiar igual (solo la ficha se queda sin esa línea).
@Serializable
internal data class CourseVersionDto(
    val id: String,
    val content: JsonObject,
    val version: Int? = null,
    val author: AuthorDto? = null,
    val publishedAt: String? = null,
)

// ── Contenido del curso (course-v1.schema.json) ──

@Serializable
internal data class CourseContentDto(
    val formatVersion: Int,
    val title: String,
    val description: String,
    val language: String,
    val tags: List<String>,
    val defaults: DefaultsDto,
    val modules: List<ModuleDto>,
)

@Serializable
internal data class DefaultsDto(val mastery: MasteryDto)

@Serializable
internal data class MasteryDto(val questions: Int, val minCorrect: Int)

@Serializable
internal data class ModuleDto(
    val id: String,
    val title: String,
    val requires: List<String>,
    val mastery: MasteryDto? = null,
    val steps: List<StepDto>,
    // Solo ejercicios: una lección aquí no se puede leer como ExerciseDto y da error.
    val review: List<ExerciseDto>,
)

// Polimorfismo de kotlinx.serialization: el campo "type" del JSON dice qué subclase leer
// (@SerialName). Un "type" que no está aquí da error al leer.
@Serializable
internal sealed interface StepDto

@Serializable
@SerialName("lesson")
internal data class LessonDto(val id: String, val text: String) : StepDto

// Un paso con foto. Es un StepDto pero no un ExerciseDto: en `review` no se puede leer y da error,
// como lo rechaza el servidor (PHOTO_IN_REVIEW). `explanation` es obligatoria (decisión A).
@Serializable
@SerialName("photo")
internal data class PhotoDto(val id: String, val prompt: String, val explanation: String) : StepDto

@Serializable
internal sealed interface ExerciseDto : StepDto

@Serializable
internal data class OptionDto(val id: String, val text: String)

@Serializable
internal data class PairDto(val left: String, val right: String)

@Serializable
@SerialName("singleChoice")
internal data class SingleChoiceDto(
    val id: String,
    val prompt: String,
    val options: List<OptionDto>,
    val answer: String,
    val explanation: String,
) : ExerciseDto

@Serializable
@SerialName("multipleChoice")
internal data class MultipleChoiceDto(
    val id: String,
    val prompt: String,
    val options: List<OptionDto>,
    val correct: List<String>,
    val explanation: String,
) : ExerciseDto

@Serializable
@SerialName("matching")
internal data class MatchingDto(
    val id: String,
    val prompt: String,
    val pairs: List<PairDto>,
    val explanation: String,
) : ExerciseDto
