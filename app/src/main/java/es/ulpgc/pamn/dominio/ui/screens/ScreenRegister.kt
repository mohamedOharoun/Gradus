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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.FieldError
import es.ulpgc.pamn.dominio.model.RegisterForm
import es.ulpgc.pamn.dominio.model.RegisterProblem
import es.ulpgc.pamn.dominio.model.RegisterStep
import es.ulpgc.pamn.dominio.model.Submission
import es.ulpgc.pamn.dominio.ui.components.ErrorText
import es.ulpgc.pamn.dominio.ui.components.FormTopBar
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.PrimaryButton
import es.ulpgc.pamn.dominio.ui.components.PromptLink
import es.ulpgc.pamn.dominio.ui.components.StepIndicator
import es.ulpgc.pamn.dominio.ui.components.buttonText
import es.ulpgc.pamn.dominio.ui.components.toMessage
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterActions
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterAddedData
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterIllustration
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterStepField
import es.ulpgc.pamn.dominio.ui.screens.register.RegisterStepHeader
import es.ulpgc.pamn.dominio.ui.screens.register.texts
import es.ulpgc.pamn.dominio.ui.text.UiText
import es.ulpgc.pamn.dominio.ui.text.asString
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
            FormTopBar(title = stringResource(R.string.action_create_account), onBack = actions.onBack)
            StepIndicator(
                labels = RegisterStep.entries.map { stringResource(it.texts.label) },
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
                texts.caption?.let { RegisterIllustration(form.step, stringResource(it)) }
            }
            Spacer(Modifier.height(16.dp))
            RegisterMainButton(form, actions)
            PromptLink(
                prompt = stringResource(R.string.register_have_account),
                action = stringResource(R.string.register_log_in_link),
                onClick = actions.onLogin,
            )
        }
    }
}

// En el último paso, «Crear cuenta» (provisional: en T11 será el deslizador); antes, «Siguiente».
// Si el envío falla, el aviso sale encima del botón y el mismo botón sirve para reintentar.
@Composable
private fun RegisterMainButton(form: RegisterForm, actions: RegisterActions) {
    if (form.step == RegisterStep.PASSWORD) {
        (form.submission as? Submission.Failed)?.let {
            ErrorText(it.reason.toMessage().asString(), Modifier.padding(bottom = 8.dp))
        }
        PrimaryButton(
            text = form.submission.buttonText(
                UiText.of(R.string.action_create_account),
                UiText.of(R.string.action_creating_account),
            ).asString(),
            onClick = actions.onRegister,
            enabled = form.canRegister,
        )
    } else {
        PrimaryButton(text = stringResource(R.string.action_next), onClick = actions.onNext, trailingIcon = R.drawable.ic_arrow_forward)
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

private val readyToRegister = RegisterForm(
    name = "Ana",
    email = "ana@ejemplo.com",
    step = RegisterStep.PASSWORD,
    password = "secreta1!",
    confirmation = "secreta1!",
    acceptedTerms = true,
)

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

@LightDarkPreviews
@Composable
private fun ScreenRegisterSendingPreview() = RegisterPreview(readyToRegister.sending())

@LightDarkPreviews
@Composable
private fun ScreenRegisterUnreachablePreview() = RegisterPreview(readyToRegister.failed(FailureReason.UNREACHABLE))

@LightDarkPreviews
@Composable
private fun ScreenRegisterServerErrorPreview() =
    RegisterPreview(readyToRegister.rejected(RegisterProblem(RegisterStep.PASSWORD, FieldError.TERMS_NOT_ACCEPTED)))
