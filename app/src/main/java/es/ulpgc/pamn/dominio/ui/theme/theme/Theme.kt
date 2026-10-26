package es.ulpgc.pamn.dominio.ui.theme.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val EsquemaClaro = lightColorScheme(
    primary = Morado, onPrimary = Color.White,
    primaryContainer = MoradoContenedor, onPrimaryContainer = Noche,
    tertiary = AmbarOscuro, onTertiary = Color.White,
    tertiaryContainer = AmbarContenedor, onTertiaryContainer = Color(0xFF3D2600),
    background = Lavanda, onBackground = Noche,
    surface = Lavanda, onSurface = Noche,
    surfaceVariant = Color(0xFFEAE6F2), onSurfaceVariant = TintaSuave,
    outlineVariant = Color(0xFFDCD6E8),
)

private val EsquemaOscuro = darkColorScheme(
    primary = MoradoClaro, onPrimary = Color(0xFF2A1670),
    primaryContainer = MoradoContenedorOscuro, onPrimaryContainer = MoradoContenedor,
    tertiary = Ambar, onTertiary = Color(0xFF3D2600),
    tertiaryContainer = AmbarContenedorOscuro, onTertiaryContainer = AmbarContenedor,
    background = Noche, onBackground = TintaClara,
    surface = Noche, onSurface = TintaClara,
    surfaceVariant = NocheSuperficie, onSurfaceVariant = Color(0xFFC9C3DB),
    outlineVariant = TintaSuave,
)

@Composable
fun DominioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Typography,
        content = content
    )
}