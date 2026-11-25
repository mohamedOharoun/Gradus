package es.ulpgc.pamn.dominio.course.progress

// Transformaciones puras de UserProgress para la sincronización (las usa ProgressSyncStore).
// Devuelven copias nuevas, así que se pueden usar dentro de MutableStateFlow.update.

// Lo pendiente con la forma del contrato: cada curso con algo pendiente, con solo sus módulos pendientes.
internal fun UserProgress.pending(): ProgressSnapshot =
    ProgressSnapshot(courses.values.mapNotNull { it.pendingSnapshot() })

// settle: primero sale de la cola lo enviado, después se fusiona lo del servidor.
internal fun UserProgress.settled(sent: ProgressSnapshot, remote: ProgressSnapshot): UserProgress =
    withoutQueued(sent).mergedWith(remote)

private fun CourseProgress.pendingSnapshot(): EnrollmentSnapshot? =
    if (!enrollmentPending && pendingModules.isEmpty()) null
    else EnrollmentSnapshot(enrollment, pendingModules.map { mastered.getValue(it) })

// Por clave, nunca por fecha. Un curso enviado que el móvil no tiene se ignora.
private fun UserProgress.withoutQueued(sent: ProgressSnapshot): UserProgress =
    sent.enrollments.fold(this) { user, course ->
        val local = user.courses[course.enrollment.courseId] ?: return@fold user
        val unqueued = local.copy(
            enrollmentPending = false,
            pendingModules = local.pendingModules - course.mastered.map { it.moduleId }.toSet(),
        )
        UserProgress(user.courses + (course.enrollment.courseId to unqueued))
    }

// Unión: lo que no estaba entra sin quedar pendiente (valores por defecto de CourseProgress).
private fun UserProgress.mergedWith(remote: ProgressSnapshot): UserProgress =
    remote.enrollments.fold(this) { user, course ->
        val courseId = course.enrollment.courseId
        val local = user.courses[courseId] ?: CourseProgress(course.enrollment)
        UserProgress(user.courses + (courseId to local.mergedWith(course)))
    }

// En lo que ya estaba gana la fecha más antigua; las marcas de pendiente no se tocan.
private fun CourseProgress.mergedWith(remote: EnrollmentSnapshot): CourseProgress = copy(
    enrollment = enrollment.copy(enrolledAt = minOf(enrollment.enrolledAt, remote.enrollment.enrolledAt)),
    mastered = remote.mastered.fold(mastered) { all, module ->
        val local = all[module.moduleId]
        all + (module.moduleId to if (local == null || module.masteredAt < local.masteredAt) module else local)
    },
)
