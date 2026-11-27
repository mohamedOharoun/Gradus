package es.ulpgc.pamn.dominio.ui.screens.mycourses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.course.publication.AuthorCourseFilter
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Cabecera de la lista de «Mis cursos»: título de sección a la izquierda y desplegable de filtro
// por estado con contadores a la derecha. Solo guarda si el menú está desplegado.
@Composable
fun CourseFilterHeader(
    selected: AuthorCourseFilter,
    counts: Map<AuthorCourseFilter, Int>,
    onSelectFilter: (AuthorCourseFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCount = counts[selected] ?: 0
    val filterDescription = selected.filterDescription(selectedCount).asString()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.my_courses_section_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        Box {
            TextButton(
                onClick = { expanded = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = filterDescription },
            ) {
                Text(
                    text = selected.filterChipText(selectedCount).asString(),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_drop_down),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                AuthorCourseFilter.entries.forEach { option ->
                    val optionCount = counts[option] ?: 0
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.filterChipText(optionCount).asString(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        trailingIcon = if (option == selected) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            expanded = false
                            onSelectFilter(option)
                        },
                    )
                }
            }
        }
    }
}

@LightDarkPreviews
@Composable
private fun CourseFilterHeaderPreview() {
    val sampleCounts = mapOf(
        AuthorCourseFilter.ALL to 4,
        AuthorCourseFilter.PUBLISHED to 1,
        AuthorCourseFilter.ACCEPTED to 1,
        AuthorCourseFilter.PENDING to 1,
        AuthorCourseFilter.REJECTED to 1,
    )
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CourseFilterHeader(
                    selected = AuthorCourseFilter.ALL,
                    counts = sampleCounts,
                    onSelectFilter = {},
                )
                CourseFilterHeader(
                    selected = AuthorCourseFilter.ACCEPTED,
                    counts = sampleCounts,
                    onSelectFilter = {},
                )
            }
        }
    }
}
