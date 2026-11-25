package es.ulpgc.pamn.dominio.submission

import kotlin.math.roundToInt

// El tamaño de una foto en píxeles.
data class PixelSize(val width: Int, val height: Int) {
    init {
        require(width > 0 && height > 0) { "Una foto tiene lados positivos: $width × $height." }
    }

    val longest: Int get() = maxOf(width, height)
}

// Cuánto reducir una foto antes de entregarla (docs/tareas/ejercicio-foto.md, decisiones F y X). Funciones
// puras: la app lee la foto, pregunta aquí y hace el trabajo con BitmapFactory.
object PhotoScaling {

    // Un folio fotografiado a 2000 px se lee bien y, en JPEG, ocupa ≈ 0,5–1,5 MB: muy por debajo de
    // PhotoSubmission.MAX_BYTES.
    const val MAX_SIDE = 2000

    // El tamaño final: el lado mayor como mucho MAX_SIDE, la misma proporción, y nunca más grande.
    fun fit(size: PixelSize): PixelSize {
        if (size.longest <= MAX_SIDE) return size
        val scale = MAX_SIDE.toDouble() / size.longest
        return PixelSize(
            width = (size.width * scale).roundToInt().coerceIn(1, MAX_SIDE),
            height = (size.height * scale).roundToInt().coerceIn(1, MAX_SIDE),
        )
    }

    // Cuánto reducir al leerla (BitmapFactory.Options.inSampleSize: 1, 2, 4…): la mayor potencia de 2
    // que deja el lado mayor en MAX_SIDE o más. El ajuste fino lo hace después fit().
    fun sampleSize(size: PixelSize): Int {
        var sample = 1
        while (size.longest / (sample * 2) >= MAX_SIDE) sample *= 2
        return sample
    }
}
