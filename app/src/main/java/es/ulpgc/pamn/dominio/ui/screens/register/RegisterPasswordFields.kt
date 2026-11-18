package es.ulpgc.pamn.dominio.ui.screens.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.model.RegisterForm
import es.ulpgc.pamn.dominio.ui.components.ErrorText
import es.ulpgc.pamn.dominio.ui.components.PasswordField
import es.ulpgc.pamn.dominio.ui.components.PasswordMatchItem
import es.ulpgc.pamn.dominio.ui.components.PasswordRequirementList
import es.ulpgc.pamn.dominio.ui.components.TermsCheckbox
import es.ulpgc.pamn.dominio.ui.components.toMessage

// Paso 3 del registro (Figma 1c): contraseña con requisitos, repetición y términos.
// Sin errores en rojo: todo se marca en directo mientras se escribe.
@Composable
internal fun RegisterPasswordFields(form: RegisterForm, actions: RegisterActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PasswordField(label = "Contraseña", value = form.password, onValueChange = actions.onPasswordChange)
        PasswordRequirementList(unmet = form.unmetRequirements)
        PasswordField(
            label = "Repite la contraseña",
            value = form.confirmation,
            onValueChange = actions.onConfirmationChange,
        )
        PasswordMatchItem(match = form.passwordsMatch)
        TermsCheckbox(checked = form.acceptedTerms, onCheckedChange = actions.onTermsChange)
        // Lo único que puede salir en rojo aquí: lo que rechace el servidor (términos o contraseña).
        form.serverError?.let { ErrorText(it.toMessage()) }
    }
}
