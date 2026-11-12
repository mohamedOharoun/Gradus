package es.ulpgc.pamn.dominio.ui.screens.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.model.RegisterForm
import es.ulpgc.pamn.dominio.model.RegisterStep
import es.ulpgc.pamn.dominio.ui.components.AddedDataChip
import es.ulpgc.pamn.dominio.ui.components.AddedDataSection
import es.ulpgc.pamn.dominio.ui.components.FormField
import es.ulpgc.pamn.dominio.ui.components.UnlockGraphIllustration
import es.ulpgc.pamn.dominio.ui.components.toMessage

// Textos de cada paso del registro (Figma 1a, 1b y 1c). label = nombre en el indicador;
// caption = texto bajo la ilustración.
internal data class StepTexts(val label: String, val title: String, val help: String, val caption: String?)

internal val RegisterStep.texts: StepTexts
    get() = when (this) {
        RegisterStep.NAME -> StepTexts(
            label = "Nombre",
            title = "¿Cómo te llamas?",
            help = "Así te verán los instructores y el resto de estudiantes.",
            caption = "Así funciona Gradus: cada concepto se desbloquea cuando dominas los anteriores.",
        )
        RegisterStep.EMAIL -> StepTexts(
            label = "Correo",
            title = "¿Cuál es tu correo?",
            help = "Lo usarás para iniciar sesión. No lo compartimos con nadie.",
            caption = "¡Un paso dominado! Ya solo te queda el último.",
        )
        RegisterStep.PASSWORD -> StepTexts(
            label = "Contraseña",
            title = "Crea una contraseña",
            help = "Escríbela dos veces para evitar errores.",
            caption = null,
        )
    }

// Chips con los datos de los pasos ya superados. En el primer paso no hay ninguno.
@Composable
internal fun RegisterAddedData(form: RegisterForm, onEdit: (RegisterStep) -> Unit) {
    if (form.step == RegisterStep.NAME) return
    AddedDataSection {
        AddedDataChip(text = form.name.trim(), onEdit = { onEdit(RegisterStep.NAME) })
        if (form.step > RegisterStep.EMAIL) {
            AddedDataChip(text = form.email.trim(), onEdit = { onEdit(RegisterStep.EMAIL) })
        }
    }
}

@Composable
internal fun RegisterStepHeader(texts: StepTexts) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(texts.title, style = MaterialTheme.typography.headlineLarge)
        Text(texts.help, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// El campo que toca en cada paso.
@Composable
internal fun RegisterStepField(form: RegisterForm, actions: RegisterActions) {
    when (form.step) {
        RegisterStep.NAME -> FormField(
            label = "Nombre",
            value = form.name,
            onValueChange = actions.onNameChange,
            error = form.nameError?.toMessage(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        )
        RegisterStep.EMAIL -> FormField(
            label = "Correo electrónico",
            value = form.email,
            onValueChange = actions.onEmailChange,
            error = form.emailError?.toMessage(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
        )
        RegisterStep.PASSWORD -> RegisterPasswordFields(form, actions)
    }
}

// La X con su texto debajo. El número de nodos dominados coincide con el número de pasos hechos.
@Composable
internal fun RegisterIllustration(step: RegisterStep, caption: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        UnlockGraphIllustration(masteredCount = step.ordinal)
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
