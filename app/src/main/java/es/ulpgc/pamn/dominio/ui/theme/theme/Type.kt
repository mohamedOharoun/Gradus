package es.ulpgc.pamn.dominio.ui.theme.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val base = Typography()
private fun serif(estilo: TextStyle) =
    estilo.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)

val Typography = Typography(
    displaySmall = serif(base.displaySmall),
    headlineLarge = serif(base.headlineLarge),
    headlineMedium = serif(base.headlineMedium),
    headlineSmall = serif(base.headlineSmall),
    titleLarge = serif(base.titleLarge),
)