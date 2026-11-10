package es.ulpgc.pamn.dominio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import es.ulpgc.pamn.dominio.data.conceptosDemo
import es.ulpgc.pamn.dominio.model.ConceptoUi
import es.ulpgc.pamn.dominio.ui.components.TarjetaConcepto
import es.ulpgc.pamn.dominio.ui.theme.GradusTheme

@Composable
fun PantallaCurso(
    tituloCurso: String,
    conceptos: List<ConceptoUi>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Text(
                text = tituloCurso,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conceptos) { concepto ->
                    TarjetaConcepto(concepto.titulo, concepto.estado, concepto.progreso)
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(text = concepto.titulo, style = MaterialTheme.typography.titleLarge)
                            Text(text = "Estado: ${concepto.estado}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaCursoClaroPreview() {
    GradusTheme(darkTheme = false) { 
        PantallaCurso("Estructuras de datos", conceptosDemo) 
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaCursoOscuroPreview() {
    GradusTheme(darkTheme = true) { 
        PantallaCurso("Estructuras de datos", conceptosDemo) 
    }
}
