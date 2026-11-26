package es.ulpgc.pamn.dominio.data.progress

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

// Pide sincronizar el progreso con WorkManager (Command / Adapter sobre WorkManager):
// - Trabajo único ("progress-sync") con ExistingWorkPolicy.REPLACE: dos peticiones seguidas dejan
//   un único trabajo pendiente.
// - Restricción NetworkType.CONNECTED: solo corre cuando hay conexión de red.
// - BackoffPolicy.EXPONENTIAL (30 s iniciales): ante reintentos, la espera crece exponencialmente.
class WorkSyncRequester(private val workManager: WorkManager) : ProgressSyncRequester {

    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<ProgressSyncWorker>()
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

        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val WORK_NAME: String = "progress-sync"
    }
}
