package com.example.boardgamerapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.boardgamerapp.domain.repository.GameNightRepository
import com.example.boardgamerapp.domain.repository.RepositoryException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: GameNightRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = DashboardUiState.Loading
        viewModelScope.launch {
            try {
                val gameNight = repository.getNextGameNight()
                if (gameNight == null) {
                    _uiState.value = DashboardUiState.Empty
                    return@launch
                }

                val host = repository.getPlayers().firstOrNull { it.id == gameNight.hostId }
                if (host == null) {
                    _uiState.value = DashboardUiState.Error(
                        "Der Gastgeber für den nächsten Spieleabend wurde nicht gefunden.",
                    )
                    return@launch
                }

                _uiState.value = DashboardUiState.Success(gameNight, host)
            } catch (exception: RepositoryException) {
                _uiState.value = DashboardUiState.Error(
                    exception.message ?: "Die Spieldaten konnten nicht geladen werden.",
                )
            }
        }
    }

    companion object {
        fun factory(repository: GameNightRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
                    return DashboardViewModel(repository) as T
                }
            }
    }
}
