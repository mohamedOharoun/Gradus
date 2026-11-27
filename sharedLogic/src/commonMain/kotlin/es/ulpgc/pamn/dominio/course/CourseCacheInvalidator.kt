package es.ulpgc.pamn.dominio.course

// Puerto funcional (SAM): permite invalidar la caché de un curso concreto (p. ej. tras publicar
// una nueva versión) sin acoplar a quien invalida con CachingCourseGateway.
fun interface CourseCacheInvalidator {
    fun invalidate(courseId: String)
}
