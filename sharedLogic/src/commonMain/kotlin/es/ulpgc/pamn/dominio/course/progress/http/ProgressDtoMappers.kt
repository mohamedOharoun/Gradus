package es.ulpgc.pamn.dominio.course.progress.http

import es.ulpgc.pamn.dominio.course.progress.Enrollment
import es.ulpgc.pamn.dominio.course.progress.EnrollmentSnapshot
import es.ulpgc.pamn.dominio.course.progress.MasteredModule
import es.ulpgc.pamn.dominio.course.progress.ProgressSnapshot
import es.ulpgc.pamn.dominio.course.progress.SkippedProgress
import es.ulpgc.pamn.dominio.course.progress.SyncResponse
import kotlinx.serialization.json.Json
import kotlin.time.Instant

// Mapper: entre el modelo de progreso de la app y el JSON de POST /api/progress/sync. Funciones
// puras, sin HTTP (eso es el gateway).

// encodeDefaults = true es un refuerzo: los DTO de la petición no tienen valores por defecto, pero
// si alguien añade uno, `"mastered":[]` se sigue escribiendo (sin él, el servidor respondería 400).
// ignoreUnknownKeys: el contrato dice que los campos desconocidos se ignoran.
private val progressJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

// ── Petición ──

// Las fechas van con Instant.toString(): UTC con Z (el servidor redondea la fracción).
internal fun encodeSyncRequest(snapshot: ProgressSnapshot): String =
    progressJson.encodeToString(SyncRequestDto(snapshot.enrollments.map { it.toDto() }))

private fun EnrollmentSnapshot.toDto() = EnrollmentDto(
    courseId = enrollment.courseId,
    enrolledAt = enrollment.enrolledAt.toString(),
    mastered = mastered.map { MasteredDto(moduleId = it.moduleId, masteredAt = it.masteredAt.toString()) },
)

// ── Respuesta ──

// Lee el cuerpo de un 200; null si no se puede leer (JSON roto, falta un campo obligatorio, tipo
// equivocado o fecha inválida). SerializationException y DateTimeFormatException heredan de
// IllegalArgumentException; nunca sale una excepción hacia fuera.
internal fun decodeSyncResponse(body: String): SyncResponse? = try {
    progressJson.decodeFromString<SyncResponseDto>(body).toSyncResponse()
} catch (_: IllegalArgumentException) {
    null
}

private fun SyncResponseDto.toSyncResponse() = SyncResponse(
    remote = ProgressSnapshot(enrollments.map { it.toSnapshot() }),
    skipped = skipped.map { it.toSkipped() },
)

private fun EnrollmentDto.toSnapshot() = EnrollmentSnapshot(
    enrollment = Enrollment(courseId = courseId, enrolledAt = Instant.parse(enrolledAt)),
    mastered = mastered.map { MasteredModule(moduleId = it.moduleId, masteredAt = Instant.parse(it.masteredAt)) },
)

// Sin moduleId (ausente o null) = inscripción entera; con moduleId, tal cual (también « »).
private fun SkippedDto.toSkipped(): SkippedProgress =
    if (moduleId == null) SkippedProgress.WholeEnrollment(courseId, code)
    else SkippedProgress.SingleModule(courseId, moduleId, code)
