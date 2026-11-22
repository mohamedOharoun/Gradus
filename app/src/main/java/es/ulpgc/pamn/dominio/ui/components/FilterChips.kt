package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.screens.search.FILTERS_LABEL
import es.ulpgc.pamn.dominio.ui.screens.search.filtersButtonDescription
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Los tres chips de los filtros de Buscar (Figma «3 · Buscar» y «4 · Buscar con filtros»).
// Los tres reciben datos y avisan con lambdas (state hoisting) y miden al menos 48 dp de zona
// táctil. Los textos que lee TalkBack vienen de `FilterTexts` (funciones puras y probadas).
//
// Un detalle de accesibilidad que se repite: lo que se *ve* y lo que se *lee* pueden ser distintos.
// `semantics { contentDescription = … }` cambia lo que lee TalkBack, y `clearAndSetSemantics {}`
// esconde los hijos (el «2» suelto, la ✕) para que no se lean aparte.

// «Filtros» con el número de filtros activos en un círculo (que no sale con 0). Abre la hoja.
// TalkBack lo lee como un solo botón: «Filtros, 2 activos».
@Composable
fun FiltersButton(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        // `minimumInteractiveComponentSize` agranda la zona táctil a 48 dp sin agrandar lo que se ve.
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics {
                role = Role.Button
                contentDescription = filtersButtonDescription(count)
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.clearAndSetSemantics {}.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Decorativo: el botón ya dice «Filtros».
            Icon(painterResource(R.drawable.ic_tune), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(FILTERS_LABEL, style = MaterialTheme.typography.labelLarge)
            if (count > 0) CountBadge(count)
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Box(
            modifier = Modifier.defaultMinSize(minWidth = 22.dp, minHeight = 22.dp).padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(count.toString(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

// Un filtro aplicado: «#grafos ✕». Todo el chip quita el filtro (decisión C): la ✕ solo es un
// dibujo, no un botón aparte. TalkBack lo lee «Quitar filtro grafos» (`removeDescription`).
@Composable
fun RemovableFilterChip(
    label: String,
    removeDescription: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onRemove,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics {
                role = Role.Button
                contentDescription = removeDescription
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.clearAndSetSemantics {}.padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(6.dp))
            Icon(painterResource(R.drawable.ic_close), contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

// Una etiqueta de la hoja de filtros, que se marca o se desmarca (decisión E). Es un `FilterChip`
// de Material, que TalkBack lee como una casilla («marcada» o «no marcada»).
//
// Los colores se ponen a mano desde el tema porque el tema no define `secondary`, que es el que
// usa Material por defecto: marcada, relleno `primary` y ✓; sin marcar, borde `outline`.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectableTagChip(
    tag: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = { Text(tag) },
        modifier = modifier.minimumInteractiveComponentSize(),
        leadingIcon = if (selected) {
            {
                // Decorativa: el estado «marcada» ya lo dice la casilla.
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else null,
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surface,
            labelColor = colors.onSurface,
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary,
            selectedLeadingIconColor = colors.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.outline,
            selectedBorderColor = colors.primary,
        ),
    )
}

@LightDarkPreviews
@Composable
private fun FilterChipsPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltersButton(count = 0, onClick = {})
                FiltersButton(count = 2, onClick = {})
                RemovableFilterChip(label = "#grafos", removeDescription = "Quitar filtro grafos", onRemove = {})
                RemovableFilterChip(label = "#árboles", removeDescription = "Quitar filtro árboles", onRemove = {})
                SelectableTagChip(tag = "grafos", selected = true, onToggle = {})
                SelectableTagChip(tag = "algoritmos", selected = false, onToggle = {})
                SelectableTagChip(tag = "bases de datos", selected = false, onToggle = {})
            }
        }
    }
}
