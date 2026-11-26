package es.ulpgc.pamn.dominio.data.course

import es.ulpgc.pamn.dominio.course.CourseCopyStore

// Patrón Adapter: adapta CourseCopyDao (Room) al puerto CourseCopyStore de sharedLogic (12b-1).
class RoomCourseCopyStore(private val dao: CourseCopyDao) : CourseCopyStore {

    override suspend fun save(courseId: String, json: String) =
        dao.save(CourseCopyEntity(courseId, json))

    override suspend fun load(courseId: String): String? =
        dao.loadJson(courseId)
}
