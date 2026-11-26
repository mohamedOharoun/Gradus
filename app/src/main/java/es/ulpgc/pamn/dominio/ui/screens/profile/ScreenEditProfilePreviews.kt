package es.ulpgc.pamn.dominio.ui.screens.profile

import androidx.compose.runtime.Composable
import es.ulpgc.pamn.dominio.model.FailureReason
import es.ulpgc.pamn.dominio.model.ProfileForm
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

private val start = ProfileForm(original = "Mohamed Oharoun")

@Composable
private fun EditProfilePreview(form: ProfileForm) {
    GradusTheme { ScreenEditProfile(email = "nombre@ejemplo.com", form = form, actions = EditProfileActions()) }
}

// Recién abierta: sin cambios, el botón está desactivado.
@LightDarkPreviews
@Composable
private fun ScreenEditProfileStartPreview() = EditProfilePreview(start)

@LightDarkPreviews
@Composable
private fun ScreenEditProfileChangedPreview() = EditProfilePreview(start.withName("Mohamed O. Haroun"))

@LightDarkPreviews
@Composable
private fun ScreenEditProfileErrorPreview() = EditProfilePreview(start.withName("M").sending())

@LightDarkPreviews
@Composable
private fun ScreenEditProfileSendingPreview() = EditProfilePreview(start.withName("Mohamed O. Haroun").sending())

@LightDarkPreviews
@Composable
private fun ScreenEditProfileUnreachablePreview() =
    EditProfilePreview(start.withName("Mohamed O. Haroun").sending().failed(FailureReason.UNREACHABLE))
