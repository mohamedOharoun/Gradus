package es.ulpgc.pamn.dominio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.model.PasswordRequirement
import es.ulpgc.pamn.dominio.ui.text.UiText
import es.ulpgc.pamn.dominio.ui.text.asString
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Adaptador lógica → interfaz, como FieldError.toMessage().
fun PasswordRequirement.toLabel(): UiText = when (this) {
    PasswordRequirement.MIN_LENGTH -> UiText.of(R.string.password_rule_min_length)
    PasswordRequirement.LETTER_AND_DIGIT -> UiText.of(R.string.password_rule_letter_and_digit)
    PasswordRequirement.SPECIAL_CHARACTER -> UiText.of(R.string.password_rule_special_character)
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
                text = requirement.toLabel().asString(),
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
        true -> ChecklistItem(stringResource(R.string.passwords_match), CheckStatus.MET, modifier, emphasized = true)
        false -> ChecklistItem(stringResource(R.string.error_passwords_dont_match), CheckStatus.FAILED, modifier)
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
