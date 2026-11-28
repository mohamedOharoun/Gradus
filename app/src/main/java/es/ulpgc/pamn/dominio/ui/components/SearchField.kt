package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Caja de búsqueda (Figma «3 · Buscar»): píldora con lupa, texto de ayuda y ✕ para borrar.
// Es "controlada", como FormField: no guarda el texto; lo recibe en value y avisa de cada cambio.
// La tecla «Buscar» del teclado cierra el teclado y llama a onSearch (decisión 4 del documento).
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        // Decorativa: el campo ya dice para qué sirve, así que TalkBack no la anuncia.
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = if (value.isEmpty()) null else {
            { ClearButton(description = stringResource(R.string.search_clear), onClick = { onValueChange("") }) }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            keyboard?.hide()
            onSearch()
        }),
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

// La ✕ de los campos de texto. `description` es lo que lee TalkBack («Borrar búsqueda», «Borrar creador»).
// En esta versión de Material 3 el IconButton mide 40 dp: se fuerza a 48 dp (zona táctil mínima).
@Composable
internal fun ClearButton(description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(painterResource(R.drawable.ic_close), contentDescription = description)
    }
}

@LightDarkPreviews
@Composable
private fun SearchFieldPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SearchField(value = "", onValueChange = {}, onSearch = {})
                SearchField(value = "grafos", onValueChange = {}, onSearch = {})
            }
        }
    }
}
