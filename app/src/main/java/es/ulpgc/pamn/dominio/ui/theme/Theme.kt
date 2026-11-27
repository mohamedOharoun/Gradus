package es.ulpgc.pamn.dominio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Cada rol de Material 3 va en pareja: el color de fondo (primary) y el del contenido encima (onPrimary).
private val EsquemaClaro = lightColorScheme(
    primary = MarcaMorado, onPrimary = TextoSobreOscuro,
    primaryContainer = MarcaMoradoContenedor, onPrimaryContainer = MarcaMoradoTexto,
    secondaryContainer = ExtraAzulContenedor, onSecondaryContainer = ExtraAzulTexto,
    tertiary = EstadoFrontera, onTertiary = EstadoFronteraTexto,
    tertiaryContainer = EstadoFronteraContenedor, onTertiaryContainer = EstadoFronteraTexto,
    background = FondoBase, onBackground = TextoPrincipal,
    surface = FondoSuperficie, onSurface = TextoPrincipal,
    surfaceVariant = FondoChip, onSurfaceVariant = TextoSecundario,
    surfaceContainerLowest = FondoSuperficie, surfaceContainerLow = FondoSuperficie,
    surfaceContainer = FondoChip, surfaceContainerHigh = FondoEstadoNeutro,
    surfaceContainerHighest = FondoEstadoNeutro,
    outline = BordeFuerte, outlineVariant = BordeSuave,
    error = TextoPeligro, onError = TextoSobreOscuro,
    errorContainer = EstadoPeligroContenedor, onErrorContainer = EstadoPeligroTexto,
)

// Figma no tiene modo oscuro: se construye con los colores «noche», «lila» y «sobre-oscuro».
private val EsquemaOscuro = darkColorScheme(
    primary = MarcaLila, onPrimary = MarcaMoradoTexto,
    primaryContainer = MarcaMoradoTexto, onPrimaryContainer = MarcaMoradoContenedor,
    secondaryContainer = ExtraAzulTexto, onSecondaryContainer = ExtraAzulContenedor,
    tertiary = EstadoFrontera, onTertiary = EstadoFronteraTexto,
    tertiaryContainer = EstadoFronteraTexto, onTertiaryContainer = EstadoFronteraContenedor,
    background = FondoNoche, onBackground = TextoSobreOscuro,
    surface = FondoNocheElevado, onSurface = TextoSobreOscuro,
    surfaceVariant = FondoNocheElevado, onSurfaceVariant = TextoSobreOscuroSuave,
    surfaceContainerLowest = FondoNoche, surfaceContainerLow = FondoNocheElevado,
    surfaceContainer = FondoNocheElevado, surfaceContainerHigh = FondoNocheElevado,
    surfaceContainerHighest = FondoNocheElevado,
    outline = BordeOscuro, outlineVariant = BordeOscuro,
    errorContainer = EstadoPeligroContenedorOscuro, onErrorContainer = EstadoPeligroTextoOscuro,
)

// Roles semánticos propios que no existen en MaterialTheme.colorScheme (p. ej. el verde de «Publicado»).
@Immutable
data class GradusExtendedColors(
    val publishedContainer: Color,
    val onPublishedContainer: Color,
)

private val ColoresExtendidosClaros = GradusExtendedColors(
    publishedContainer = EstadoPublicadoContenedor,
    onPublishedContainer = EstadoPublicadoTexto,
)

private val ColoresExtendidosOscuros = GradusExtendedColors(
    publishedContainer = EstadoPublicadoContenedorOscuro,
    onPublishedContainer = EstadoPublicadoTextoOscuro,
)

private val LocalGradusColors = staticCompositionLocalOf { ColoresExtendidosClaros }

object GradusTheme {
    val colors: GradusExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGradusColors.current
}

@Composable
fun GradusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val extendedColors = if (darkTheme) ColoresExtendidosOscuros else ColoresExtendidosClaros
    CompositionLocalProvider(LocalGradusColors provides extendedColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
            typography = Typography,
            content = content
        )
    }
}