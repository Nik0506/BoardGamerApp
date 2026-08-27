package com.example.boardgamerapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.repository.GameNightRepository
import com.example.boardgamerapp.domain.repository.RepositoryException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: GameNightRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadPlayers()
    }

    fun loadPlayers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    players = repository.getPlayers().sortedBy { it.hostOrder },
                )
            } catch (exception: RepositoryException) {
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    errorMessage = exception.message ?: "Spieler konnten nicht geladen werden.",
                )
            }
        }
    }

    fun savePlayer(
        id: String?,
        name: String,
        address: String,
    ): String? {
        val cleanName = name.trim()
        val cleanAddress = address.trim()
        if (cleanName.isEmpty()) return "Bitte einen Namen eingeben."
        if (cleanAddress.isEmpty()) return "Bitte eine Adresse eingeben."

        viewModelScope.launch {
            val currentPlayers = _uiState.value.players
            val existingPlayer = currentPlayers.firstOrNull { it.id == id }
            val player = existingPlayer?.copy(
                name = cleanName,
                address = cleanAddress,
            ) ?: Player(
                id = "player-${currentPlayers.size + 1}",
                name = cleanName,
                address = cleanAddress,
                hostOrder = (currentPlayers.maxOfOrNull { it.hostOrder } ?: 0) + 1,
            )
            repository.savePlayer(player)
            loadPlayers()
        }
        return null
    }

    fun planNextGameNight(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.planNextGameNight()
            onComplete()
        }
    }

    companion object {
        fun factory(repository: GameNightRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
                    return ProfileViewModel(repository) as T
                }
            }
    }
}
