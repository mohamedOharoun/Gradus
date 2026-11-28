package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.ClearButton
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Campo «Creador» de la hoja de filtros (Figma «4 · Buscar con filtros»): persona, texto de ayuda y ✕.
// Controlado, como SearchField: no guarda el texto. La tecla «Hecho» solo cierra el teclado: aplicar
// es cosa de «Mostrar N cursos» (decisión 7 del documento).
//
// La ayuda desaparece al escribir, y en el Figma no hay etiqueta. Por eso el campo lleva una
// descripción propia («Creador»): así TalkBack dice para qué es aunque ya tenga texto.
@Composable
fun CreatorField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    // Fuera de semantics {}: ahí no se puede leer strings.xml.
    val fieldDescription = CREATOR_FIELD_DESCRIPTION.asString()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { contentDescription = fieldDescription },
        singleLine = true,
        placeholder = { Text(CREATOR_HINT.asString()) },
        // Decorativo: el campo ya dice para qué sirve.
        leadingIcon = { Icon(painterResource(R.drawable.ic_person), contentDescription = null) },
        trailingIcon = if (value.isEmpty()) null else {
            { ClearButton(description = CLEAR_CREATOR_DESCRIPTION.asString(), onClick = { onValueChange("") }) }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@LightDarkPreviews
@Composable
private fun CreatorFieldPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                CreatorField(value = "", onValueChange = {})
                CreatorField(value = "Ana", onValueChange = {})
            }
        }
    }
}
