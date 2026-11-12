package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.model.PasswordRequirement
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Adaptador lógica → interfaz, como FieldError.toMessage().
fun PasswordRequirement.toLabel(): String = when (this) {
    PasswordRequirement.MIN_LENGTH -> "Al menos 8 caracteres"
    PasswordRequirement.LETTER_AND_DIGIT -> "Una letra y un número"
    PasswordRequirement.SPECIAL_CHARACTER -> "Un carácter especial (!, @, #…)"
}

// Requisitos de la contraseña: se marcan con ✓ en cuanto se cumplen.
@Composable
fun PasswordRequirementList(
    unmet: Set<PasswordRequirement>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PasswordRequirement.entries.forEach { requirement ->
            ChecklistItem(
                text = requirement.toLabel(),
                status = if (requirement in unmet) CheckStatus.PENDING else CheckStatus.MET,
            )
        }
    }
}

// Coincidencia de las dos contraseñas. Con null (repetición vacía) no muestra nada.
@Composable
fun PasswordMatchItem(
    match: Boolean?,
    modifier: Modifier = Modifier,
) {
    when (match) {
        null -> Unit
        true -> ChecklistItem("Las contraseñas coinciden", CheckStatus.MET, modifier, emphasized = true)
        false -> ChecklistItem("Las contraseñas no coinciden", CheckStatus.FAILED, modifier)
    }
}

@LightDarkPreviews
@Composable
private fun PasswordChecksPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PasswordRequirementList(unmet = setOf(PasswordRequirement.SPECIAL_CHARACTER))
                PasswordMatchItem(match = true)
                PasswordMatchItem(match = false)
            }
        }
    }
}
