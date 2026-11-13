package es.ulpgc.pamn.dominio.ui.components

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

// Multipreview: poner @LightDarkPreviews en una función equivale a poner las dos @Preview.
@Preview(name = "Claro", showBackground = true)
@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class LightDarkPreviews
