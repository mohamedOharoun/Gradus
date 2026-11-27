package es.ulpgc.pamn.dominio.ui.screens.mycourses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.dominio.R
import es.ulpgc.pamn.dominio.ui.components.LightDarkPreviews
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

// Tarjeta destacada en la parte superior de «Mis cursos»: explicación breve, botón principal
// «Subir curso» y acceso a la guía para crear cursos con IA. Sin estado propio (state hoisting).
@Composable
fun UploadHeroCard(
    onUploadCourse: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.my_courses_upload_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onUploadCourse,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_upload),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.my_courses_upload_action),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onOpenGuide,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.my_courses_guide_link),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@LightDarkPreviews
@Composable
private fun UploadHeroCardPreview() {
    GradusTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            UploadHeroCard(
                onUploadCourse = {},
                onOpenGuide = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
