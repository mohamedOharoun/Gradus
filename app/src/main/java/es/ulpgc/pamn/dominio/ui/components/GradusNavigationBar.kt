package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.ui.navigation.TopLevelDestination
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Barra de navegación de las pantallas con sesión (Figma «Barra de navegación»).
// Sin estado: pinta la pestaña que le dicen (selected) y avisa al pulsar otra (onSelect).
// No sabe navegar: eso lo decide quien la usa (MainScaffold / GradusNavHost).
@Composable
fun GradusNavigationBar(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    // NavigationBar ya mide 80 dp, deja sitio a la barra de gestos del sistema (edge-to-edge)
    // y cada pestaña es una zona táctil grande que TalkBack lee como «pestaña, seleccionada».
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val isSelected = destination == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(destination) },
                // Decorativo: el texto de debajo ya dice qué es (y TalkBack lo lee).
                icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                label = {
                    Text(
                        destination.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else null,
                    )
                },
                colors = gradusNavigationBarItemColors(),
            )
        }
    }
}

// Colores por rol del tema (claro y oscuro salen solos de GradusTheme).
@Composable
private fun gradusNavigationBarItemColors(): NavigationBarItemColors {
    val colors = MaterialTheme.colorScheme
    return NavigationBarItemDefaults.colors(
        indicatorColor = colors.primaryContainer, // la píldora lavanda
        selectedIconColor = colors.onPrimaryContainer,
        // primary y no onPrimaryContainer: en oscuro, onPrimaryContainer es casi blanco y la
        // pestaña seleccionada no se distinguiría de las demás.
        selectedTextColor = colors.primary,
        unselectedIconColor = colors.onSurfaceVariant,
        unselectedTextColor = colors.onSurfaceVariant,
    )
}

@LightDarkPreviews
@Composable
private fun GradusNavigationBarPreview() {
    GradusTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TopLevelDestination.entries.forEach { selected ->
                GradusNavigationBar(selected = selected, onSelect = {})
            }
        }
    }
}
