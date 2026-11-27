package es.ulpgc.pamn.dominio.course.publication

// Operación en segundo plano que está esperando completarse en un borrador pendiente.
enum class PendingAction {
    VALIDATING,
    PUBLISHING,
}

// Aviso no definitivo en un borrador pendiente (la pantalla decide cómo redactarlo).
enum class PendingNotice {
    NONE,
    NETWORK,
    SESSION_EXPIRED,
    SERVER_FAILURE,
}

// Motivo concreto por el que un borrador quedó rechazado (sin textos localizados rígidos).
sealed interface DraftRejection {
    data class ValidationReport(val report: CourseReport) : DraftRejection {
        init {
            require(!report.isValid) { "ValidationReport rejection must contain at least one error" }
        }
    }

    data object IdenticalToLatest : DraftRejection
    data object NotAuthor : DraftRejection
    data object CourseNotFound : DraftRejection
    data object TooLarge : DraftRejection
    data object FileMissing : DraftRejection
}

// Estado del ciclo de vida de un borrador local.
sealed interface DraftStatus {
    data class Pending(
        val action: PendingAction,
        val notice: PendingNotice = PendingNotice.NONE,
    ) : DraftStatus

    data class Accepted(val report: CourseReport = CourseReport.EMPTY) : DraftStatus {
        init {
            require(report.isValid) { "Accepted draft report must not contain errors" }
        }
    }

    data class Rejected(val reason: DraftRejection) : DraftStatus
}
