package es.ulpgc.pamn.dominio.course.progress

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

// Ayudas de los tests de sincronización: construir snapshots y compararlos sin depender del orden.

internal fun snapshot(vararg enrollments: EnrollmentSnapshot) = ProgressSnapshot(enrollments.toList())

// Una inscripción con sus módulos dominados: enrollment("grafos", START, "bfs" to START + 1.minutes).
internal fun enrollment(courseId: String, at: Instant, vararg mastered: Pair<String, Instant>) =
    EnrollmentSnapshot(Enrollment(courseId, at), mastered.map { (moduleId, date) -> MasteredModule(moduleId, date) })

// Las listas del snapshot no tienen orden garantizado: se comparan como inscripción → conjunto de módulos.
internal fun ProgressSnapshot.normalized(): Map<Enrollment, Set<MasteredModule>> =
    enrollments.associate { it.enrollment to it.mastered.toSet() }

internal fun assertSameSnapshot(expected: ProgressSnapshot, actual: ProgressSnapshot) =
    assertEquals(expected.normalized(), actual.normalized())

// Invariante: un módulo solo puede estar pendiente si está dominado (pendingModules ⊆ mastered.keys).
internal fun assertPendingWithinMastered(user: UserProgress) = user.courses.forEach { (courseId, course) ->
    assertTrue(course.mastered.keys.containsAll(course.pendingModules), "pending modules outside mastered in $courseId")
}

// Sube lo pendiente y el servidor responde con eso mismo: deja la cola vacía.
internal suspend fun ProgressFixture.uploadAll(userId: String = "ana") {
    val sent = store.pending(userId)
    store.settle(userId, sent, remote = sent)
}
