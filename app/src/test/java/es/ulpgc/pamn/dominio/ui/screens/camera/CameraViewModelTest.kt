package es.ulpgc.pamn.dominio.ui.screens.camera

import es.ulpgc.pamn.dominio.data.notes.PhotoFiles
import es.ulpgc.pamn.dominio.notes.InMemoryNotesStore
import es.ulpgc.pamn.dominio.notes.MAX_CAPTION_LENGTH
import es.ulpgc.pamn.dominio.notes.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// La cámara de las notas (docs/tareas/notas-camara.md, tarea 5, boceto 3 y decisión X): permiso, disparar,
// revisar (Repetir / Guardar con el pie) y salir. Sin cámara de verdad: la pantalla avisa con onCaptured.
// Tiempo simulado: nada corre hasta runCurrent().
@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakePhotoFiles : PhotoFiles {
        var captures = 0
        val kept = mutableListOf<String>()
        val discarded = mutableListOf<String>()
        var keepAnswer: String? = "notes/guardada.jpg"
        override suspend fun import(source: String): String? = null
        override suspend fun delete(path: String) {}
        override fun newCapture(): String = "cache/captura${++captures}.jpg"
        override suspend fun keepCapture(capture: String): String? = keepAnswer.also { kept += capture }
        override suspend fun discardCapture(capture: String) {
            discarded += capture
        }
    }

    private val notes = InMemoryNotesStore()
    private val photos = FakePhotoFiles()
    private val viewModel = CameraViewModel(CameraPurpose.Note("g", "bfs", notes), photos, clock = { 7_000L }, newId = { "id-nueva" })

    private val stage get() = viewModel.state.stage

    // Hasta tener una foto para revisar: permiso dado, disparar y que la cámara termine.
    private fun reviewing() {
        viewModel.onPermission(granted = true)
        viewModel.onShutter()
        viewModel.onCaptured()
    }

    // ── Permiso (decisión X) ──

    @Test
    fun `it starts asking for the camera`() {
        assertEquals(CameraStage.NeedsPermission(), stage)
    }

    @Test
    fun `with permission the camera is ready`() {
        viewModel.onPermission(granted = true)
        assertEquals(CameraStage.Ready, stage)
    }

    // Se explica para qué hace falta y se puede volver a pedir.
    @Test
    fun `without permission it explains and can ask again`() {
        viewModel.onPermission(granted = false, canAskAgain = true)
        assertEquals(CameraStage.NeedsPermission(denied = true, permanently = false), stage)
    }

    // Denegado para siempre: solo queda ir a los ajustes del sistema.
    @Test
    fun `denied for good it says so`() {
        viewModel.onPermission(granted = false, canAskAgain = false)
        assertEquals(CameraStage.NeedsPermission(denied = true, permanently = true), stage)
    }

    // Al volver de los ajustes con el permiso dado.
    @Test
    fun `a later permission makes it ready`() {
        viewModel.onPermission(granted = false, canAskAgain = false)
        viewModel.onPermission(granted = true)
        assertEquals(CameraStage.Ready, stage)
    }

    // Si ya está haciendo o revisando una foto, volver a comprobar el permiso no lo deshace.
    @Test
    fun `checking the permission again while reviewing changes nothing`() {
        reviewing()
        viewModel.onPermission(granted = true)
        assertTrue(stage is CameraStage.Reviewing)
    }

    // ── Disparar ──

    @Test
    fun `the shutter asks the camera to write a new temporary file`() {
        viewModel.onPermission(granted = true)
        viewModel.onShutter()
        assertEquals(CameraStage.Capturing("cache/captura1.jpg"), stage)
    }

    // Dos toques rápidos: una sola foto.
    @Test
    fun `while capturing the shutter does nothing`() {
        viewModel.onPermission(granted = true)
        viewModel.onShutter()
        viewModel.onShutter()
        assertEquals(1, photos.captures)
    }

    @Test
    fun `without permission the shutter does nothing`() {
        viewModel.onShutter()
        assertEquals(0, photos.captures)
    }

    @Test
    fun `once taken the photo is reviewed with an empty caption`() {
        reviewing()
        assertEquals(CameraStage.Reviewing("cache/captura1.jpg"), stage)
    }

    @Test
    fun `if the camera fails it says so, cleans up and is ready again`() = runTest {
        viewModel.onPermission(granted = true)
        viewModel.onShutter()

        viewModel.onCaptureFailed()
        runCurrent()

        assertEquals(CameraStage.Ready, stage)
        assertTrue(viewModel.state.captureFailed)
        assertEquals(listOf("cache/captura1.jpg"), photos.discarded)
    }

    @Test
    fun `shooting again clears the camera failure`() {
        viewModel.onPermission(granted = true)
        viewModel.onShutter()
        viewModel.onCaptureFailed()
        viewModel.onShutter()
        assertFalse(viewModel.state.captureFailed)
    }

    // ── Revisar: Repetir ──

    @Test
    fun `retake throws the photo away and goes back to the camera`() = runTest {
        reviewing()

        viewModel.onRetake()
        runCurrent()

        assertEquals(CameraStage.Ready, stage)
        assertEquals(listOf("cache/captura1.jpg"), photos.discarded)
    }

    // ── Revisar: el pie (decisión Y) ──

    @Test
    fun `typing changes the caption`() {
        reviewing()
        viewModel.onCaptionChange("Dijkstra")
        assertEquals("Dijkstra", (stage as CameraStage.Reviewing).caption)
    }

    // ── Revisar: Guardar ──

    @Test
    fun `saving keeps the photo and saves the note with its caption`() = runTest {
        reviewing()
        viewModel.onCaptionChange("  Dijkstra   paso a paso ")

        viewModel.onSave()
        runCurrent()

        assertEquals(listOf("cache/captura1.jpg"), photos.kept)
        assertEquals(
            listOf(Note("id-nueva", "g", "bfs", "notes/guardada.jpg", "Dijkstra paso a paso", 7_000)),
            notes.notes("g", "bfs").first(),
        )
        assertEquals(CameraStage.Closed, stage)
    }

    @Test
    fun `without a caption the note has none`() = runTest {
        reviewing()
        viewModel.onSave()
        runCurrent()
        assertEquals(null, notes.notes("g", "bfs").first().single().caption)
    }

    @Test
    fun `a too long caption is not saved and says so`() = runTest {
        reviewing()
        viewModel.onCaptionChange("a".repeat(MAX_CAPTION_LENGTH + 1))

        viewModel.onSave()
        runCurrent()

        assertTrue((stage as CameraStage.Reviewing).captionTooLong)
        assertEquals(emptyList<String>(), photos.kept)
    }

    @Test
    fun `typing again clears the too long caption warning`() = runTest {
        reviewing()
        viewModel.onCaptionChange("a".repeat(MAX_CAPTION_LENGTH + 1))
        viewModel.onSave()
        runCurrent()

        viewModel.onCaptionChange("corto")

        assertFalse((stage as CameraStage.Reviewing).captionTooLong)
    }

    @Test
    fun `if the photo cannot be kept it says so and can be retried`() = runTest {
        photos.keepAnswer = null
        reviewing()

        viewModel.onSave()
        runCurrent()

        val review = stage as CameraStage.Reviewing
        assertTrue(review.saveFailed)
        assertFalse(review.saving)
        assertEquals(emptyList<Note>(), notes.notes("g", "bfs").first())
    }

    @Test
    fun `while saving it says so and saving again does nothing`() = runTest {
        reviewing()

        viewModel.onSave()
        assertTrue((stage as CameraStage.Reviewing).saving)
        viewModel.onSave()
        runCurrent()

        assertEquals(1, photos.kept.size)
    }

    // ── Salir ──

    @Test
    fun `leaving before taking a photo just closes`() = runTest {
        viewModel.onPermission(granted = true)
        viewModel.onLeave()
        runCurrent()
        assertEquals(CameraStage.Closed, stage)
        assertEquals(emptyList<String>(), photos.discarded)
    }

    // Salir sin guardar no deja la foto temporal en el móvil.
    @Test
    fun `leaving while reviewing throws the photo away`() = runTest {
        reviewing()
        viewModel.onLeave()
        runCurrent()
        assertEquals(CameraStage.Closed, stage)
        assertEquals(listOf("cache/captura1.jpg"), photos.discarded)
    }

    // Mientras se guarda no se sale: la foto ya va camino de las notas.
    @Test
    fun `leaving while saving waits for the save`() = runTest {
        reviewing()
        viewModel.onSave()
        viewModel.onLeave()
        runCurrent()
        assertEquals(emptyList<String>(), photos.discarded)
        assertEquals(1, notes.notes("g", "bfs").first().size)
    }

    // ── La foto de una entrega (docs/tareas/ejercicio-foto.md, decisión W) ──

    private val forSubmission = CameraViewModel(CameraPurpose.Submission, photos)

    private fun CameraViewModel.reviewingPhoto() {
        onPermission(granted = true)
        onShutter()
        onCaptured()
    }

    @Test
    fun `a camera for notes asks for a caption`() {
        assertFalse(viewModel.state.forSubmission)
    }

    // Sin pie: la foto de un ejercicio no lo lleva.
    @Test
    fun `a camera for a submission has no caption, also after shooting`() {
        assertTrue(forSubmission.state.forSubmission)
        forSubmission.reviewingPhoto()
        assertTrue(forSubmission.state.forSubmission)
    }

    // «Usar foto»: la foto pasa al paso tal cual, sin guardar ninguna nota ni tirarla (ahora es del paso).
    @Test
    fun `using the photo of a submission hands it over without saving a note`() = runTest {
        forSubmission.reviewingPhoto()
        forSubmission.onSave()
        runCurrent()

        assertEquals(CameraStage.Used("cache/captura1.jpg"), forSubmission.state.stage)
        assertEquals(emptyList<String>(), photos.kept)
        assertEquals(emptyList<String>(), photos.discarded)
        assertTrue(notes.notes("g", "bfs").first().isEmpty())
    }

    // Una vez entregada al paso, salir no la tira.
    @Test
    fun `leaving after using the photo does not throw it away`() = runTest {
        forSubmission.reviewingPhoto()
        forSubmission.onSave()
        forSubmission.onLeave()
        runCurrent()
        assertEquals(emptyList<String>(), photos.discarded)
    }

    @Test
    fun `retaking the photo of a submission throws it away as for a note`() = runTest {
        forSubmission.reviewingPhoto()
        forSubmission.onRetake()
        runCurrent()
        assertEquals(CameraStage.Ready, forSubmission.state.stage)
        assertEquals(listOf("cache/captura1.jpg"), photos.discarded)
    }

    // Si la cámara falla, sigue siendo la cámara de una entrega.
    @Test
    fun `a failed capture keeps the purpose`() {
        forSubmission.onPermission(granted = true)
        forSubmission.onShutter()
        forSubmission.onCaptureFailed()
        assertTrue(forSubmission.state.forSubmission)
    }
}
