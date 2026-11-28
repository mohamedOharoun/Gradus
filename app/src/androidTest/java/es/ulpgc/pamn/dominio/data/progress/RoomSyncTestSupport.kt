package es.ulpgc.pamn.dominio.data.progress

import es.ulpgc.pamn.dominio.course.progress.Enrollment
import es.ulpgc.pamn.dominio.course.progress.EnrollmentSnapshot
import es.ulpgc.pamn.dominio.course.progress.MasteredModule
import es.ulpgc.pamn.dominio.course.progress.ProgressSnapshot
import es.ulpgc.pamn.dominio.course.progress.SyncOutcome
import es.ulpgc.pamn.dominio.course.progress.SyncResponse
import org.junit.Assert.assertEquals
import kotlin.time.Instant

// Ayudas de los tests de pending y settle con Room (7c). Las de commonTest son internal de
// sharedLogic y no se ven desde app: estas hacen lo mismo.

// Un snapshot con las inscripciones dadas.
internal fun snapshot(vararg courses: EnrollmentSnapshot) = ProgressSnapshot(courses.toList())

// Una inscripción con su fecha y sus módulos dominados (id to fecha).
internal fun enrollment(courseId: String, at: Instant, vararg modules: Pair<String, Instant>) =
    EnrollmentSnapshot(Enrollment(courseId, at), modules.map { (moduleId, masteredAt) -> MasteredModule(moduleId, masteredAt) })

// Las listas de un snapshot no tienen orden garantizado: se comparan ordenadas.
internal fun assertSameSnapshot(expected: ProgressSnapshot, actual: ProgressSnapshot) =
    assertEquals(expected.sorted(), actual.sorted())

private fun ProgressSnapshot.sorted() = ProgressSnapshot(
    enrollments
        .map { it.copy(mastered = it.mastered.sortedBy { module -> module.moduleId }) }
        .sortedBy { it.enrollment.courseId },
)

// Un 200 con todo el progreso del servidor y nada omitido.
internal fun synced(remote: ProgressSnapshot = snapshot()) =
    SyncOutcome.Synced(SyncResponse(remote, emptyList()))

// Sube todo lo pendiente del usuario y el servidor responde con lo mismo (sin nada más).
internal suspend fun RoomProgressRule.uploadAll(userId: String = ANA) {
    val sent = store.pending(userId)
    store.settle(userId, sent, remote = sent)
}

// La fila de un módulo dominado, o null si no existe.
internal suspend fun RoomProgressRule.module(userId: String, courseId: String, moduleId: String) =
    dao.masteredModules(userId, courseId).singleOrNull { it.moduleId == moduleId }

// Todas las filas de las dos tablas como texto («tabla:usuario|curso|…») y ordenadas: para comprobar
// que algo no cambia nada (ni borra, ni añade, ni toca una fecha o un pending). Con userId, solo las
// de ese usuario (user_id es la primera columna de las dos tablas).
internal fun RoomProgressRule.allRows(userId: String? = null): List<String> =
    listOf("enrollments", "mastered_modules").flatMap { table ->
        database.openHelper.readableDatabase.query("SELECT * FROM $table").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add((0 until cursor.columnCount).joinToString("|", prefix = "$table:") { cursor.getString(it) })
                }
            }
        }
    }.filter { userId == null || it.substringAfter(':').startsWith("$userId|") }.sorted()
