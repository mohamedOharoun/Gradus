package es.ulpgc.pamn.dominio.ui.screens.password

import androidx.compose.runtime.Composable
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.PasswordForm
import es.ulpgc.pamn.dominio.model.PasswordProblem
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

private val filled = PasswordForm().withCurrent("Gradus2026!").withNext("Nueva2026?").withConfirmation("Nueva2026?")

@Composable
private fun ChangePasswordPreview(form: PasswordForm) {
    GradusTheme { ScreenChangePassword(form = form, actions = ChangePasswordActions()) }
}

@LightDarkPreviews
@Composable
private fun ScreenChangePasswordEmptyPreview() = ChangePasswordPreview(PasswordForm())

// Escribiendo: requisitos a medias y repetición que no coincide.
@LightDarkPreviews
@Composable
private fun ScreenChangePasswordTypingPreview() =
    ChangePasswordPreview(PasswordForm().withCurrent("Gradus2026!").withNext("Nueva20").withConfirmation("Nueva2"))

@LightDarkPreviews
@Composable
private fun ScreenChangePasswordEmptyErrorsPreview() = ChangePasswordPreview(PasswordForm().sending())

@LightDarkPreviews
@Composable
private fun ScreenChangePasswordWrongCurrentPreview() =
    ChangePasswordPreview(filled.sending().rejected(PasswordProblem.WrongCurrent))

@LightDarkPreviews
@Composable
private fun ScreenChangePasswordUnreachablePreview() =
    ChangePasswordPreview(filled.sending().failed(FailureReason.UNREACHABLE))

@LightDarkPreviews
@Composable
private fun ScreenChangePasswordChangedPreview() = ChangePasswordPreview(filled.sending().succeeded())
