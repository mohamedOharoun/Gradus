package es.ulpgc.pamn.dominio.data.progress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Acceso a las tablas del progreso: insertar y leer (7a) y lo que usa RoomProgressStore (7b). Nunca
// se borra nada. Lo de la sincronización llega en 7c.
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
}
