package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.ulpgc.pamn.dominio.ui.navigation.TopLevelDestination
import es.ulpgc.pamn.dominio.ui.screens.ScreenSearch
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Armazón de las pantallas con sesión: el contenido arriba y GradusNavigationBar abajo.
// Sin estado, como la barra: recibe la pestaña actual y avisa al pulsar otra.
// content recibe un Modifier con el hueco que deja la barra: hay que pasárselo a la pantalla.
@Composable
fun MainScaffold(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { GradusNavigationBar(selected = selected, onSelect = onSelect) },
    ) { innerPadding ->
        // padding: deja sitio a la barra (y a la barra de estado).
        // consumeWindowInsets: avisa a la pantalla de que ese sitio ya está cubierto, para que su
        // safeDrawingPadding no lo vuelva a sumar (si no, habría doble margen).
        content(Modifier.padding(innerPadding).consumeWindowInsets(innerPadding))
    }
}

@LightDarkPreviews
@Composable
private fun MainScaffoldPreview() {
    GradusTheme {
        MainScaffold(selected = TopLevelDestination.SEARCH, onSelect = {}) { ScreenSearch(it) }
    }
}
