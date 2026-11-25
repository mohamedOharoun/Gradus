package es.ulpgc.pamn.dominio.course.progress

import kotlin.time.Instant

// Un módulo dominado y cuándo se dominó.
data class MasteredModule(val moduleId: String, val masteredAt: Instant)
