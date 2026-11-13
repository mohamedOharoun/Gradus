package es.ulpgc.pamn.dominio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import es.ulpgc.pamn.dominio.R

// Fuentes de res/font. Cada Font dice qué fichero usar para cada grosor.
val SourceSerif4 = FontFamily(
    Font(R.font.source_serif_4_semibold, FontWeight.SemiBold),
)

val SourceSans3 = FontFamily(
    Font(R.font.source_sans_3_regular, FontWeight.Normal),
    Font(R.font.source_sans_3_semibold, FontWeight.SemiBold),
    Font(R.font.source_sans_3_bold, FontWeight.Bold),
)

// Factory method: crea un estilo con los mismos datos que muestra Figma
// (tamaño en px → sp; interlineado como multiplicador → em).
private fun figmaStyle(family: FontFamily, weight: FontWeight, size: Int, lineHeight: Float) =
    TextStyle(fontFamily = family, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.em)

private fun serif(size: Int, lineHeight: Float) = figmaStyle(SourceSerif4, FontWeight.SemiBold, size, lineHeight)
private fun sans(weight: FontWeight, size: Int, lineHeight: Float) = figmaStyle(SourceSans3, weight, size, lineHeight)

// Roles sin estilo en Figma: se toman los de Material y solo se cambia la fuente.
private val base = Typography()
private fun TextStyle.inSerif() = copy(fontFamily = SourceSerif4, fontWeight = FontWeight.SemiBold)

val Typography = Typography(
    displayLarge = base.displayLarge.inSerif(),
    displayMedium = base.displayMedium.inSerif(),
    displaySmall = base.displaySmall.inSerif(),
    headlineLarge = serif(30, 1.15f),                     // Título/Pantalla
    headlineMedium = serif(26, 1.2f),                     // Título/Destacado
    headlineSmall = base.headlineSmall.inSerif(),
    titleLarge = sans(FontWeight.Bold, 18, 1.25f),        // Texto/Sección
    titleMedium = sans(FontWeight.SemiBold, 16, 1.3f),    // Texto/Cuerpo fuerte
    titleSmall = sans(FontWeight.Bold, 14, 1.2f),         // Texto/Botón pequeño
    bodyLarge = sans(FontWeight.Normal, 17, 1.45f),       // Texto/Cuerpo grande
    bodyMedium = sans(FontWeight.Normal, 15, 1.4f),       // Texto/Cuerpo
    bodySmall = sans(FontWeight.Normal, 14, 1.4f),        // Texto/Secundario
    labelLarge = sans(FontWeight.Bold, 16, 1.2f),         // Texto/Botón
    labelMedium = sans(FontWeight.Bold, 13, 1.2f),        // Texto/Etiqueta
    labelSmall = sans(FontWeight.SemiBold, 12, 1.2f),     // Texto/Navegación
)
