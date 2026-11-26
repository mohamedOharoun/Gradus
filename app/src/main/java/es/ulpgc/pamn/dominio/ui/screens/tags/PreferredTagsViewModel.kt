package es.ulpgc.pamn.dominio.ui.screens.tags

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ulpgc.pamn.dominio.settings.SettingsStore
import es.ulpgc.pamn.dominio.settings.TagAddition
import kotlinx.coroutines.launch

// «Etiquetas preferidas» (docs/tareas/ajustes.md, fase 3, tarea 3). Cada cambio se guarda al momento:
// no hay botón «Guardar». Las reglas (normalizar, sin repetir, máximo 10) son de PreferredTags; aquí
// solo se conectan con lo que se escribe. Recibe el puerto SettingsStore por constructor.
class PreferredTagsViewModel(private val settings: SettingsStore) : ViewModel() {

    companion object {
        fun factory(settings: SettingsStore) = viewModelFactory {
            initializer { PreferredTagsViewModel(settings) }
        }
    }

    var state by mutableStateOf(PreferredTagsUiState(tags = settings.settings.value.preferredTags.values))
        private set

    // Lo guardado manda: cualquier cambio del store (también desde otro sitio) se ve aquí.
    init {
        viewModelScope.launch {
            settings.settings.collect { state = state.copy(tags = it.preferredTags.values) }
        }
    }

    // Escribir borra el aviso anterior.
    fun onDraftChange(draft: String) {
        state = state.copy(draft = draft, problem = null)
    }

    fun onAdd() {
        when (val result = settings.settings.value.preferredTags.add(state.draft)) {
            is TagAddition.Added -> {
                state = state.copy(draft = "", problem = null)
                viewModelScope.launch { settings.setPreferredTags(result.tags) }
            }
            // No se añade: se dice por qué y el borrador se queda para corregirlo.
            else -> state = state.copy(problem = result)
        }
    }

    // Quitar una deja sitio, así que el aviso («máximo 10», etc.) ya no aplica.
    fun onRemove(tag: String) {
        state = state.copy(problem = null)
        val remaining = settings.settings.value.preferredTags.remove(tag)
        viewModelScope.launch { settings.setPreferredTags(remaining) }
    }
}
