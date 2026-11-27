package es.ulpgc.pamn.dominio.data.publication

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

// Adapter / Command sobre WorkManager (espejo de WorkSyncRequester):
// - Un trabajo único por borrador ("course-upload-$draftId") con ExistingWorkPolicy.REPLACE:
//   dos encolados del mismo borrador no duplican trabajo, y borradores distintos corren aparte.
// - Restricción NetworkType.CONNECTED: solo se ejecuta cuando hay conexión de red.
// - BackoffPolicy.EXPONENTIAL (30 s iniciales): ante reintentos, la espera crece exponencialmente.
class WorkUploadRequester(private val workManager: WorkManager) : CourseUploadRequester {

    override fun enqueue(draftId: String) {
        require(draftId.isNotBlank()) { "draftId must not be blank" }

        val request = OneTimeWorkRequestBuilder<CourseUploadWorker>()
            .setInputData(workDataOf(CourseUploadWorker.KEY_DRAFT_ID to draftId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(workName(draftId), ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(draftId: String) {
        if (draftId.isNotBlank()) {
            workManager.cancelUniqueWork(workName(draftId))
        }
    }

    companion object {
        fun workName(draftId: String): String = "course-upload-$draftId"
    }
}
