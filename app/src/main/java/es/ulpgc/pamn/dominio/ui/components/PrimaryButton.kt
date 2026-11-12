package es.ulpgc.pamn.dominio.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Botón principal de los formularios (Figma «Botón Siguiente»): píldora morada de 56 dp a todo lo ancho.
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes trailingIcon: Int? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text)
        trailingIcon?.let { icon ->
            Spacer(Modifier.width(8.dp))
            // Decorativo: el texto del botón ya dice lo que hace.
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@LightDarkPreviews
@Composable
private fun PrimaryButtonPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(text = "Entrar", onClick = {})
                PrimaryButton(text = "Siguiente", onClick = {}, trailingIcon = R.drawable.ic_arrow_forward)
                PrimaryButton(text = "Siguiente", onClick = {}, enabled = false, trailingIcon = R.drawable.ic_arrow_forward)
            }
        }
    }
}
