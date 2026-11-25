package es.ulpgc.pamn.dominio.data.progress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

// Acceso a las tablas del progreso. De momento solo insertar y leer (7a); las consultas de los
// puertos llegan en 7b y 7c.
@Dao
interface ProgressDao {

    @Insert
    suspend fun insertEnrollment(enrollment: EnrollmentEntity)

    @Insert
    suspend fun insertMasteredModule(module: MasteredModuleEntity)

    @Query("SELECT * FROM enrollments WHERE user_id = :userId AND course_id = :courseId")
    suspend fun enrollment(userId: String, courseId: String): EnrollmentEntity?

    @Query("SELECT * FROM mastered_modules WHERE user_id = :userId AND course_id = :courseId")
    suspend fun masteredModules(userId: String, courseId: String): List<MasteredModuleEntity>
}
