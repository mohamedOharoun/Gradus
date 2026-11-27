package es.ulpgc.pamn.dominio.data.publication

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import es.ulpgc.pamn.dominio.course.publication.DraftWorkDecision
import es.ulpgc.pamn.dominio.course.publication.ProcessCourseDraft
import es.ulpgc.pamn.dominio.data.AppContainer

// Trabajo en segundo plano de WorkManager (CoroutineWorker, espejo de ProgressSyncWorker):
// procesa un borrador pendiente (validar o publicar) delegando en ProcessCourseDraft y traduciendo
// DraftWorkDecision a Result.
//
// No captura excepciones:
// - Los fallos de red/servidor/fichero llegan como DraftWorkDecision desde ProcessCourseDraft.
// - Un fallo del almacén (Room o lectura de sesión) se propaga y WorkManager marca FAILED.
// - Una cancelación (CancellationException) se propaga limpiamente sin corromper el estado.
class CourseUploadWorker internal constructor(
    appContext: Context,
    params: WorkerParameters,
    private val processor: ProcessCourseDraft,
) : CoroutineWorker(appContext, params) {

    // Constructor estándar para la WorkerFactory por defecto de WorkManager en producción
    // (así no hace falta tocar AndroidManifest.xml).
    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext = appContext,
        params = params,
        processor = AppContainer.processCourseDraft,
    )

    override suspend fun doWork(): Result {
        val draftId = inputData.getString(KEY_DRAFT_ID)?.takeIf { it.isNotBlank() }
            ?: return Result.failure()
        return processor.process(draftId, runAttemptCount).toWorkResult()
    }

    companion object {
        const val KEY_DRAFT_ID: String = "draft_id"

        // Factory Method para pruebas (TestListenableWorkerBuilder y WorkManagerTestInitHelper):
        // inyecta un ProcessCourseDraft controlado sin tocar el AppContainer real.
        fun factory(processor: ProcessCourseDraft): WorkerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker? =
                if (workerClassName == CourseUploadWorker::class.java.name) {
                    CourseUploadWorker(appContext, workerParameters, processor)
                } else {
                    null
                }
        }
    }
}

internal fun DraftWorkDecision.toWorkResult(): ListenableWorker.Result = when (this) {
    DraftWorkDecision.Done -> ListenableWorker.Result.success()
    DraftWorkDecision.Retry -> ListenableWorker.Result.retry()
    DraftWorkDecision.GiveUp -> ListenableWorker.Result.failure()
}
