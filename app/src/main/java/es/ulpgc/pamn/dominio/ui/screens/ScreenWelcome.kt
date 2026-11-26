package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.BrandLogo
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.components.WelcomeIllustration
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Bienvenida (Figma «1 · Bienvenida»). Sigue el modo del móvil; solo la ilustración es siempre oscura.
@Composable
fun ScreenWelcome(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit,
    onExploreCourses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            BrandLogo()
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                WelcomeIllustration(modifier = Modifier.widthIn(max = 282.dp))
            }
            WelcomeMessage()
            Spacer(Modifier.height(24.dp))
            WelcomeActions(onCreateAccount, onLogin, onExploreCourses)
        }
    }
}

@Composable
private fun WelcomeMessage(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WelcomeActions(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit,
    onExploreCourses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val buttonModifier = Modifier.fillMaxWidth().height(52.dp)
        Button(onClick = onCreateAccount, modifier = buttonModifier) {
            Text(stringResource(R.string.action_create_account))
        }
        OutlinedButton(
            onClick = onLogin,
            modifier = buttonModifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
        ) {
            Text(stringResource(R.string.action_log_in))
        }
        TextButton(onClick = onExploreCourses, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(stringResource(R.string.welcome_explore), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@LightDarkPreviews
@Composable
private fun ScreenWelcomePreview() {
    GradusTheme { ScreenWelcome(onCreateAccount = {}, onLogin = {}, onExploreCourses = {}) }
}
