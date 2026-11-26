package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import java.io.File

// La foto de una nota a pantalla completa, con zoom (docs/tareas/notas-camara.md, boceto 4):
// pellizcar para ampliar (hasta 5×) y arrastrar para moverse; doble toque, ampliar o volver al tamaño normal.
// No cambia el estado de la app: el zoom vive solo en esta pantalla (remember).
@Composable
fun ZoomablePhoto(file: File, maxSidePx: Int, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    // transformable avisa de cada gesto: cuánto se ha pellizcado (zoom) y cuánto se ha movido (pan).
    val gestures = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        offset = if (scale == MIN_SCALE) Offset.Zero else offset + pan
    }
    Box(
        modifier = modifier
            .clipToBounds()
            .transformable(gestures)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = if (scale > MIN_SCALE) MIN_SCALE else DOUBLE_TAP_SCALE
                    offset = Offset.Zero
                })
            },
    ) {
        NotePhoto(
            file,
            maxSidePx = maxSidePx,
            contentScale = ContentScale.Fit,
            // graphicsLayer escala y mueve lo ya dibujado: no vuelve a leer la foto en cada gesto.
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
