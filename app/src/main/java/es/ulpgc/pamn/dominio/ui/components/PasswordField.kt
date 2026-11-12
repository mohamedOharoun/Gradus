package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Campo de contraseña: un FormField con el ojo para mostrar u ocultar.
// Mostrar/ocultar es estado interno (solo le importa al campo); el texto sigue siendo del que lo usa.
@Composable
fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    PasswordField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        visible = visible,
        onToggleVisible = { visible = !visible },
        modifier = modifier,
        error = error,
    )
}

// Versión sin estado: recibe si se ve o no. La usan la de arriba y las previews.
@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    FormField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        error = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { VisibilityToggle(visible = visible, onClick = onToggleVisible) },
    )
}

@Composable
private fun VisibilityToggle(visible: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
            contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
        )
    }
}

@LightDarkPreviews
@Composable
private fun PasswordFieldPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PasswordField("Contraseña", "secreta1!", {}, visible = false, onToggleVisible = {})
                PasswordField("Contraseña", "secreta1!", {}, visible = true, onToggleVisible = {})
                PasswordField(
                    "Repite la contraseña", "secreta", {}, visible = false, onToggleVisible = {},
                    error = "Las contraseñas no coinciden",
                )
            }
        }
    }
}
