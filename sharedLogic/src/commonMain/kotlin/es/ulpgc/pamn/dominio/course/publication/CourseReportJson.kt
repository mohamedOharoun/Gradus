package es.ulpgc.pamn.dominio.course.publication

import es.ulpgc.pamn.dominio.course.publication.http.decodeCourseReport
import es.ulpgc.pamn.dominio.course.publication.http.encodeCourseReport

// Códec público para persistir un CourseReport como JSON (p. ej. en RoomCourseDraftStore)
// reutilizando los mismos DTOs que el adaptador HTTP. Nunca lanza excepciones al decodificar.
object CourseReportJson {

    fun encode(report: CourseReport): String = encodeCourseReport(report)

    fun decode(json: String): CourseReport? = decodeCourseReport(json)
}
