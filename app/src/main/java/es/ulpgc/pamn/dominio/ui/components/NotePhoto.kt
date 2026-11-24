package es.ulpgc.pamn.dominio.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// La foto de una nota (docs/tareas/notas-camara.md). Sin librerías de imágenes: se lee con BitmapFactory
// de Android, reducida a unos `maxSidePx` (una foto de móvil ocupa decenas de MB en memoria a tamaño
// completo), y fuera del hilo de la pantalla. Mientras carga, o si no se puede leer, un recuadro gris.
// Es decorativa para TalkBack: quien la usa pone la descripción (la fecha y el pie).
@Composable
fun NotePhoto(
    file: File,
    maxSidePx: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    // produceState: empieza en null y, cuando termina de leer la foto, cambia el valor y se repinta.
    // Si cambia el fichero o el tamaño, vuelve a leer.
    val bitmap by produceState<ImageBitmap?>(initialValue = null, file, maxSidePx) {
        value = withContext(Dispatchers.IO) { decodeSampled(file, maxSidePx) }
    }
    val image = bitmap
    if (image == null) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    } else {
        Image(bitmap = image, contentDescription = null, modifier = modifier, contentScale = contentScale)
    }
}

// Lee solo el tamaño, calcula cuánto reducir (potencias de 2, como pide BitmapFactory) y lee la foto ya
// reducida. null si no existe o no es una imagen.
private fun decodeSampled(file: File, maxSidePx: Int): ImageBitmap? {
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSidePx) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return null
    return bitmap.upright(file).asImageBitmap()
}

// Muchas cámaras guardan la foto «tumbada» y apuntan en sus datos EXIF cuánto hay que girarla.
// BitmapFactory no lo mira: se gira aquí (ExifInterface viene con Android).
private fun Bitmap.upright(file: File): Bitmap {
    val degrees = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return this
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)
}
