package es.ulpgc.pamn.dominio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import es.ulpgc.pamn.dominio.data.conceptosDemo
import es.ulpgc.pamn.dominio.ui.theme.screens.PantallaCurso
import es.ulpgc.pamn.dominio.ui.theme.theme.DominioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DominioTheme {
                PantallaCurso("Estructuras de datos", conceptosDemo)
            }
        }
    }
}