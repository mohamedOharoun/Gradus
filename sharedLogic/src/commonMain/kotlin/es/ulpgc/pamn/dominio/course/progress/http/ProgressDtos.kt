package es.ulpgc.pamn.dominio.course.progress.http

import kotlinx.serialization.Serializable

// DTO: el JSON de POST /api/progress/sync (backend/docs/api-progreso.md), campo a campo. No son el
// modelo de la app: si el JSON cambia, cambian estas clases y su mapper (ProgressDtoMappers.kt).
// Las fechas son texto aquí; el mapper las convierte a Instant.
//
// Estas clases NO tienen valores por defecto: en el contrato todos los campos de la petición son
// obligatorios (también `mastered: []`) y un valor por defecto podría no escribirse → 400.

@Serializable
internal data class SyncRequestDto(val enrollments: List<EnrollmentDto>)

// La misma forma en la petición y en la respuesta.
@Serializable
internal data class EnrollmentDto(val courseId: String, val enrolledAt: String, val mastered: List<MasteredDto>)

@Serializable
internal data class MasteredDto(val moduleId: String, val masteredAt: String)

@Serializable
internal data class SyncResponseDto(val enrollments: List<EnrollmentDto>, val skipped: List<SkippedDto>)

// Solo la respuesta, y el único campo opcional: sin `moduleId` (o null, B-9) es la inscripción entera.
@Serializable
internal data class SkippedDto(val courseId: String, val moduleId: String? = null, val code: String)
