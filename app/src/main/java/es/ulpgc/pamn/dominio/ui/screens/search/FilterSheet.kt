package es.ulpgc.pamn.dominio.ui.screens.search

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import es.ulpgc.pamn.dominio.course.search.FilterSheetState
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// La hoja inferior de filtros (Figma «4 · Buscar con filtros»): se ocupa solo de la ventana y de
// cómo se cierra; lo que se dibuja dentro está en FilterSheetContent.
//
// Se abre entera (`skipPartiallyExpanded`, decisión I). Hay dos formas de cerrarla:
// - Deslizar hacia abajo, tocar fuera o «atrás»: `onCloseFilters` (descarta el borrador). Material
//   ya anima el cierre y llama a `onDismissRequest` cuando termina.
// - «Mostrar cursos» y ✕: primero se esconde la hoja con animación y, al acabar, se avisa
//   (`hideThen`). Si se avisara antes, quien la quita del árbol la haría desaparecer de golpe.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    state: FilterSheetState,
    actions: SearchActions,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Se pone a `true` al empezar a cerrar: un segundo toque (o «atrás» mientras se esconde) no debe
    // avisar otra vez ni avisar de otra cosa.
    var closing by remember { mutableStateOf(false) }

    // `launch` arranca una corrutina: `hide()` es una función `suspend` que espera a que acabe la
    // animación. `invokeOnCompletion` ejecuta el aviso cuando termina. También se llama si `hide()` se
    // cancela (por ejemplo, si «atrás» arranca la animación propia de Material): el usuario ya ha
    // decidido, así que se avisa igualmente. Solo no se avisa si el `scope` está cancelado, que es
    // cuando la hoja ha salido de la pantalla y ya no hay nadie a quien avisar.
    fun hideThen(action: () -> Unit) {
        if (closing) return
        closing = true
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (scope.isActive) action() }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!closing) {
                closing = true
                actions.onCloseFilters()
            }
        },
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        FilterSheetContent(
            state = state,
            actions = actions,
            onApply = { hideThen(actions.onApplyFilters) },
            onClose = { hideThen(actions.onCloseFilters) },
        )
    }
}
