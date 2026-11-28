package es.ulpgc.pamn.dominio.data.progress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

// Acceso a las tablas del progreso: insertar y leer (7a), lo que usa RoomProgressStore (7b),
// sincronización (7c: pendingRows y settle) y baja / marcado protegido en transacción (B4).
// Patrón Unit of Work (@Transaction) para operaciones compuestas atómicas.
@Dao
interface ProgressDao {

    @Insert
    suspend fun insertEnrollment(enrollment: EnrollmentEntity)

    @Insert
    suspend fun insertMasteredModule(module: MasteredModuleEntity)

    // INSERT OR IGNORE: si ya hay una fila con esa clave primaria, no la toca (se conservan la fecha y
    // pending). Ojo: SQLite NO ignora así las violaciones de clave foránea; esas siguen lanzando.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEnrollmentIfAbsent(enrollment: EnrollmentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMasteredModuleIfAbsent(module: MasteredModuleEntity)

    @Query("SELECT * FROM enrollments WHERE user_id = :userId AND course_id = :courseId")
    suspend fun enrollment(userId: String, courseId: String): EnrollmentEntity?

    @Query("SELECT * FROM mastered_modules WHERE user_id = :userId AND course_id = :courseId")
    suspend fun masteredModules(userId: String, courseId: String): List<MasteredModuleEntity>

    // Los Flow de Room vuelven a consultar con cualquier cambio en la tabla, aunque sea de otro
    // usuario u otro curso: quien los use debe filtrar los valores repetidos.
    @Query("SELECT EXISTS(SELECT 1 FROM enrollments WHERE user_id = :userId AND course_id = :courseId)")
    fun watchEnrolled(userId: String, courseId: String): Flow<Boolean>

    @Query("SELECT module_id FROM mastered_modules WHERE user_id = :userId AND course_id = :courseId")
    fun watchMasteredIds(userId: String, courseId: String): Flow<List<String>>

    // --- Sincronización (7c): lo usan pendingRows y settle, de abajo. ---

    // Las inscripciones que hay que subir: las pendientes y las que tienen algún módulo pendiente (el
    // contrato no admite módulos sueltos: van dentro de su inscripción aunque ya estuviera subida).
    @Query(
        """SELECT * FROM enrollments WHERE user_id = :userId AND (pending = 1 OR EXISTS (
               SELECT 1 FROM mastered_modules AS m WHERE m.user_id = enrollments.user_id
               AND m.course_id = enrollments.course_id AND m.pending = 1))""",
    )
    suspend fun enrollmentsToUpload(userId: String): List<EnrollmentEntity>

    @Query("SELECT * FROM mastered_modules WHERE user_id = :userId AND pending = 1")
    suspend fun pendingModules(userId: String): List<MasteredModuleEntity>

    // Sacar de la cola por clave, sin mirar fechas. Si la fila no existe, no cambia nada.
    @Query("UPDATE enrollments SET pending = 0 WHERE user_id = :userId AND course_id = :courseId")
    suspend fun unqueueEnrollment(userId: String, courseId: String)

    @Query(
        "UPDATE mastered_modules SET pending = 0 " +
            "WHERE user_id = :userId AND course_id = :courseId AND module_id = :moduleId",
    )
    suspend fun unqueueModule(userId: String, courseId: String, moduleId: String)

    // Quedarse con la fecha más antigua (MIN con dos argumentos da el menor; las fechas son
    // nanosegundos, que conservan el orden). pending no se toca.
    @Query(
        "UPDATE enrollments SET enrolled_at = MIN(enrolled_at, :enrolledAt) " +
            "WHERE user_id = :userId AND course_id = :courseId",
    )
    suspend fun keepOlderEnrollmentDate(userId: String, courseId: String, enrolledAt: Instant)

    @Query(
        "UPDATE mastered_modules SET mastered_at = MIN(mastered_at, :masteredAt) " +
            "WHERE user_id = :userId AND course_id = :courseId AND module_id = :moduleId",
    )
    suspend fun keepOlderMasteredDate(userId: String, courseId: String, moduleId: String, masteredAt: Instant)

    // Lo pendiente del usuario. Las dos lecturas van en una transacción: ven el mismo estado, así que
    // cada módulo pendiente tiene su inscripción en la otra lista.
    @Transaction
    suspend fun pendingRows(userId: String): ProgressRows =
        ProgressRows(enrollmentsToUpload(userId), pendingModules(userId))

    // settle en UNA transacción: si algo lanza, se deshace todo. Nunca DELETE.
    // 1. sent sale de la cola por clave (de sent solo se usan las claves, nunca sus fechas).
    // 2. remote se fusiona: lo que no está entra con su pending (false, de toRows); en lo que ya
    //    estaba gana la fecha más antigua y su pending no cambia. Las inscripciones van antes que
    //    los módulos por la clave foránea.
    // Sin UPSERT (INSERT … ON CONFLICT DO UPDATE): necesita SQLite 3.24 y Android 10 trae la 3.22.
    // Tampoco @Upsert de Room: reescribe la fila entera, pending incluido.
    @Transaction
    suspend fun settle(sent: ProgressRows, remote: ProgressRows) {
        sent.enrollments.forEach { unqueueEnrollment(it.userId, it.courseId) }
        sent.modules.forEach { unqueueModule(it.userId, it.courseId, it.moduleId) }
        remote.enrollments.forEach {
            insertEnrollmentIfAbsent(it)
            keepOlderEnrollmentDate(it.userId, it.courseId, it.enrolledAt)
        }
        remote.modules.forEach {
            insertMasteredModuleIfAbsent(it)
            keepOlderMasteredDate(it.userId, it.courseId, it.moduleId, it.masteredAt)
        }
    }

    // --- Baja local y marcado protegido en transacción (B4) ---

    @Query("DELETE FROM mastered_modules WHERE user_id = :userId AND course_id = :courseId")
    suspend fun deleteMasteredModules(userId: String, courseId: String)

    @Query("DELETE FROM enrollments WHERE user_id = :userId AND course_id = :courseId")
    suspend fun deleteEnrollment(userId: String, courseId: String)

    // Borra primero los módulos dominados y luego la inscripción en una misma transacción para
    // respetar la clave foránea de v3 (sin onDelete = CASCADE). Room serializa los @Transaction,
    // así que una baja no puede entrelazarse con markMasteredIfEnrolled.
    @Transaction
    suspend fun deleteEnrollmentWithMastered(userId: String, courseId: String) {
        deleteMasteredModules(userId, courseId)
        deleteEnrollment(userId, courseId)
    }

    // Comprueba la inscripción e inserta el módulo en la misma transacción: evita carreras con una
    // baja concurrente. Devuelve false si el usuario no está inscrito (el almacén lanza la
    // IllegalStateException del puerto).
    @Transaction
    suspend fun markMasteredIfEnrolled(module: MasteredModuleEntity): Boolean {
        if (enrollment(module.userId, module.courseId) == null) return false
        insertMasteredModuleIfAbsent(module)
        return true
    }
}
