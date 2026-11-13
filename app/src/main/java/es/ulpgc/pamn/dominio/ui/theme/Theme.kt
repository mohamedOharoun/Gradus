package es.ulpgc.pamn.dominio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Cada rol de Material 3 va en pareja: el color de fondo (primary) y el del contenido encima (onPrimary).
private val EsquemaClaro = lightColorScheme(
    primary = MarcaMorado, onPrimary = TextoSobreOscuro,
    primaryContainer = MarcaMoradoContenedor, onPrimaryContainer = MarcaMoradoTexto,
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
)

// Figma no tiene modo oscuro: se construye con los colores «noche», «lila» y «sobre-oscuro».
private val EsquemaOscuro = darkColorScheme(
    primary = MarcaLila, onPrimary = MarcaMoradoTexto,
    primaryContainer = MarcaMoradoTexto, onPrimaryContainer = MarcaMoradoContenedor,
    tertiary = EstadoFrontera, onTertiary = EstadoFronteraTexto,
    tertiaryContainer = EstadoFronteraTexto, onTertiaryContainer = EstadoFronteraContenedor,
    background = FondoNoche, onBackground = TextoSobreOscuro,
    surface = FondoNocheElevado, onSurface = TextoSobreOscuro,
    surfaceVariant = FondoNocheElevado, onSurfaceVariant = TextoSobreOscuroSuave,
    surfaceContainerLowest = FondoNoche, surfaceContainerLow = FondoNocheElevado,
    surfaceContainer = FondoNocheElevado, surfaceContainerHigh = FondoNocheElevado,
    surfaceContainerHighest = FondoNocheElevado,
    outline = BordeOscuro, outlineVariant = BordeOscuro,
)

@Composable
fun GradusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Typography,
        content = content
    )
}