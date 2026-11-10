package es.ulpgc.pamn.dominio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import es.ulpgc.pamn.dominio.data.conceptosDemo
import es.ulpgc.pamn.dominio.ui.screens.PantallaCurso
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GradusTheme {
                PantallaCurso("Estructuras de datos", conceptosDemo)
            }
        }
    }
}