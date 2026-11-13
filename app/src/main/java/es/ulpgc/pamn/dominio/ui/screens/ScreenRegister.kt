package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.model.RegisterForm
import es.ulpgc.pamn.dominio.model.RegisterStep
import es.ulpgc.pamn.dominio.ui.components.FormTopBar
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.components.PromptLink
import es.ulpgc.pamn.dominio.ui.components.StepIndicator
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterActions
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterAddedData
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterIllustration
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterStepField
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterStepHeader
import es.ulpgc.pamn.dominio.ui.screens.register.texts
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Registro por pasos (Figma 1a, 1b y 1c). Sin estado: pinta el RegisterForm y avisa con `actions`.
@Composable
fun ScreenRegister(
    form: RegisterForm,
    actions: RegisterActions,
    modifier: Modifier = Modifier,
) {
    val texts = form.step.texts
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            FormTopBar(title = "Crear cuenta", onBack = actions.onBack)
            StepIndicator(
                labels = RegisterStep.entries.map { it.texts.label },
                currentIndex = form.step.ordinal,
                modifier = Modifier.padding(top = 16.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                RegisterAddedData(form, actions.onEdit)
                RegisterStepHeader(texts)
                RegisterStepField(form, actions)
                texts.caption?.let { RegisterIllustration(form.step, it) }
            }
            Spacer(Modifier.height(16.dp))
            RegisterMainButton(form, actions)
            PromptLink(prompt = "¿Ya tienes cuenta?", action = "Inicia sesión", onClick = actions.onLogin)
        }
    }
}

// En el último paso, «Crear cuenta» (provisional: en T11 será el deslizador); antes, «Siguiente».
@Composable
private fun RegisterMainButton(form: RegisterForm, actions: RegisterActions) {
    if (form.step == RegisterStep.PASSWORD) {
        PrimaryButton(text = "Crear cuenta", onClick = actions.onRegister, enabled = form.canRegister)
    } else {
        PrimaryButton(text = "Siguiente", onClick = actions.onNext, trailingIcon = R.drawable.ic_arrow_forward)
    }
}

@Composable
private fun RegisterPreview(form: RegisterForm) {
    GradusTheme { ScreenRegister(form, RegisterActions()) }
}

@LightDarkPreviews
@Composable
private fun ScreenRegisterNamePreview() = RegisterPreview(RegisterForm())

@LightDarkPreviews
@Composable
private fun ScreenRegisterNameErrorPreview() = RegisterPreview(RegisterForm(name = "A", isStepSubmitted = true))

@LightDarkPreviews
@Composable
private fun ScreenRegisterEmailPreview() =
    RegisterPreview(RegisterForm(name = "Ana", email = "ana@", step = RegisterStep.EMAIL, isStepSubmitted = true))

@LightDarkPreviews
@Composable
private fun ScreenRegisterPasswordPreview() = RegisterPreview(
    RegisterForm(
        name = "Ana",
        email = "ana@ejemplo.com",
        step = RegisterStep.PASSWORD,
        password = "secreta1",
        confirmation = "secreta1",
    ),
)
