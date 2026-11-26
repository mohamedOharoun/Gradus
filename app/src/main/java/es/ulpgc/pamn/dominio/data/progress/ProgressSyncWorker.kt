package es.ulpgc.pamn.dominio.data.progress

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import es.ulpgc.pamn.dominio.course.progress.BackgroundSyncDecision
import es.ulpgc.pamn.dominio.course.progress.SessionProgressSync
import es.ulpgc.pamn.dominio.course.progress.backgroundSyncDecision
import es.ulpgc.pamn.dominio.data.AppContainer

// Trabajo en segundo plano de WorkManager (CoroutineWorker): sincroniza el progreso con la sesión
// guardada (SessionProgressSync) y traduce la política pura (backgroundSyncDecision) a Result.
//
// No captura excepciones:
// - Los fallos de red/servidor nunca lanzan (llegan como SyncOutcome).
// - Un fallo del almacén (Room o lectura de sesión) se propaga y WorkManager marca el trabajo FAILED.
// - Una cancelación (CancellationException) se propaga limpiamente; como settle es atómico, lo
//   pendiente queda intacto para la siguiente ejecución.
class ProgressSyncWorker internal constructor(
    appContext: Context,
    params: WorkerParameters,
    private val sessionSync: SessionProgressSync,
) : CoroutineWorker(appContext, params) {

    // Constructor estándar que usa la WorkerFactory por defecto de WorkManager en producción
    // (así no hace falta tocar AndroidManifest.xml ni desactivar el inicializador por defecto).
    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext = appContext,
        params = params,
        sessionSync = AppContainer.sessionProgressSync,
    )

    override suspend fun doWork(): Result =
        backgroundSyncDecision(sessionSync.run(), runAttemptCount).toWorkResult()

    companion object {
        // Factory Method para pruebas (TestListenableWorkerBuilder y WorkManagerTestInitHelper):
        // inyecta un SessionProgressSync controlado sin tocar el AppContainer real.
        fun factory(sessionSync: SessionProgressSync): WorkerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker? =
                if (workerClassName == ProgressSyncWorker::class.java.name) {
                    ProgressSyncWorker(appContext, workerParameters, sessionSync)
                } else {
                    null
                }
        }
    }
}

internal fun BackgroundSyncDecision.toWorkResult(): ListenableWorker.Result = when (this) {
    BackgroundSyncDecision.Done -> ListenableWorker.Result.success()
    BackgroundSyncDecision.Retry -> ListenableWorker.Result.retry()
    BackgroundSyncDecision.GiveUp -> ListenableWorker.Result.failure()
}
