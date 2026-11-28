package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Los tres tonos de los cuadrados de los cursos en el Figma (Inicio): lila, ámbar y azul.
// `of` lo decide el id del curso, no su sitio en la lista: un curso no cambia de color al subir al
// primer puesto (docs/tareas/inicio.md, paso 4b). String.hashCode es siempre el mismo para el mismo texto.
enum class TileTone {
    LILAC, AMBER, BLUE;

    companion object {
        fun of(courseId: String): TileTone = entries[Math.floorMod(courseId.hashCode(), entries.size)]
    }
}

// El cuadrado con la inicial de un curso (los cursos no tienen imagen; decisión I). Decorativo: TalkBack
// no lo lee, porque al lado está el título. `size`: 40 dp en las filas; más grande en «Continúa».
@Composable
fun CourseInitialTile(letter: String, tone: TileTone, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val (background, content) = tone.colors()
    Box(
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(size / 4))
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(text = letter, style = letterStyle(size), color = content)
    }
}

// Cada tono es un rol del tema (fondo y letra), así funciona en claro y en oscuro.
@Composable
private fun TileTone.colors(): Pair<Color, Color> = with(MaterialTheme.colorScheme) {
    when (this@colors) {
        TileTone.LILAC -> primaryContainer to onPrimaryContainer
        TileTone.AMBER -> tertiaryContainer to onTertiaryContainer
        TileTone.BLUE -> secondaryContainer to onSecondaryContainer
    }
}

@Composable
private fun letterStyle(size: Dp): TextStyle =
    if (size >= 56.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium

@LightDarkPreviews
@Composable
private fun CourseInitialTilePreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CourseInitialTile("K", TileTone.LILAC)
                CourseInitialTile("Á", TileTone.AMBER)
                CourseInitialTile("R", TileTone.BLUE)
                CourseInitialTile("E", TileTone.LILAC, size = 56.dp)
            }
        }
    }
}
