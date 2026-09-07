package com.oscar.consultareat.ui.baremo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oscar.consultareat.data.baremo.BaremoIndice
import com.oscar.consultareat.data.baremo.BaremoInfraccion
import com.oscar.consultareat.data.baremo.BaremoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context

data class BaremoUiState(
    val indices: List<BaremoIndice> = emptyList(),
    val resultados: List<BaremoInfraccion> = emptyList(),
    val indiceSeleccionado: BaremoIndice? = null,
    val gravedad: String = BaremoRepository.TODAS,
    val busqueda: String = "",
    val cargando: Boolean = true,
    val error: String? = null
)

class BaremoViewModel(
    private val repository: BaremoRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BaremoUiState())
    val uiState: StateFlow<BaremoUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    fun actualizarBusqueda(value: String) {
        _uiState.value = _uiState.value.copy(busqueda = value)
        cargarResultados()
    }

    fun seleccionarGravedad(value: String) {
        _uiState.value = _uiState.value.copy(gravedad = value)
        cargarResultados()
    }

    fun seleccionarIndice(value: BaremoIndice?) {
        _uiState.value = _uiState.value.copy(indiceSeleccionado = value)
        cargarResultados()
    }

    fun reintentar() = cargarDatos()

    private fun cargarDatos() {
        _uiState.value = _uiState.value.copy(cargando = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val indices = repository.findIndices()
                indices to repository.findInfracciones(null, BaremoRepository.TODAS, null)
            }.onSuccess { (indices, resultados) ->
                _uiState.value = _uiState.value.copy(
                    indices = indices,
                    resultados = resultados,
                    cargando = false,
                    error = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    cargando = false,
                    error = error.message ?: "No se pudo abrir el baremo sancionador."
                )
            }
        }
    }

    private fun cargarResultados() {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.findInfracciones(
                    state.indiceSeleccionado?.id,
                    state.gravedad,
                    state.busqueda
                )
            }.onSuccess { resultados ->
                _uiState.value = _uiState.value.copy(resultados = resultados, error = null)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }
}

class BaremoViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val database = com.oscar.consultareat.data.baremo.BaremoDatabase(context.applicationContext)
        return BaremoViewModel(BaremoRepository(database)) as T
    }
}
