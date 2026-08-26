package com.example.boardgamerapp.ui.players

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.boardgamerapp.data.repository.MoveDirection
import com.example.boardgamerapp.data.repository.PlayerRepository
import com.example.boardgamerapp.domain.model.Player
import java.time.format.DateTimeFormatter
import java.util.Locale

class PlayersViewModel(
    private val repository: PlayerRepository,
) : ViewModel() {

    var uiState by mutableStateOf(PlayersUiState())
        private set

    init {
        loadPlayers()
    }

    fun loadPlayers() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        repository.getPlayers().fold(
            onSuccess = { players ->
                uiState = uiState.copy(
                    players = players.map { it.toUiModel() },
                    isLoading = false,
                )
            },
            onFailure = { error ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Die Spieler konnten nicht geladen werden.",
                )
            },
        )
    }

    fun beginAddPlayer() {
        uiState = uiState.copy(editor = PlayerEditorUiState(), message = null)
    }

    fun beginEditPlayer(playerId: Long) {
        val player = uiState.players.firstOrNull { it.id == playerId } ?: return
        uiState = uiState.copy(
            editor = PlayerEditorUiState(
                playerId = player.id,
                name = player.name,
                address = player.address,
            ),
            message = null,
        )
    }

    fun updateEditorName(name: String) {
        uiState = uiState.copy(
            editor = uiState.editor?.copy(name = name, errorMessage = null),
        )
    }

    fun updateEditorAddress(address: String) {
        uiState = uiState.copy(
            editor = uiState.editor?.copy(address = address, errorMessage = null),
        )
    }

    fun dismissEditor() {
        uiState = uiState.copy(editor = null)
    }

    fun savePlayer() {
        val editor = uiState.editor ?: return
        val result = if (editor.playerId == null) {
            repository.addPlayer(editor.name, editor.address)
        } else {
            repository.updatePlayer(editor.playerId, editor.name, editor.address)
        }

        result.fold(
            onSuccess = { player ->
                uiState = uiState.copy(
                    editor = null,
                    message = "${player.name} wurde gespeichert.",
                )
                loadPlayers()
            },
            onFailure = { error ->
                uiState = uiState.copy(
                    editor = editor.copy(
                        errorMessage = error.message ?: "Der Spieler konnte nicht gespeichert werden.",
                    ),
                )
            },
        )
    }

    fun movePlayer(playerId: Long, direction: MoveDirection) {
        repository.movePlayer(playerId, direction).fold(
            onSuccess = { players ->
                uiState = uiState.copy(
                    players = players.map { it.toUiModel() },
                    message = "Die Gastgeberreihenfolge wurde aktualisiert.",
                    errorMessage = null,
                )
            },
            onFailure = { error ->
                uiState = uiState.copy(errorMessage = error.message)
            },
        )
    }

    fun createNextGameNight() {
        repository.createNextGameNight().fold(
            onSuccess = { upcoming ->
                val date = upcoming.gameNight.startsAt.format(dateFormatter)
                uiState = uiState.copy(
                    message = "Nächster Termin: $date bei ${upcoming.host.name}.",
                    errorMessage = null,
                )
            },
            onFailure = { error ->
                uiState = uiState.copy(
                    errorMessage = error.message
                        ?: "Der nächste Spieleabend konnte nicht angelegt werden.",
                )
            },
        )
    }

    fun clearMessage() {
        uiState = uiState.copy(message = null, errorMessage = null)
    }

    private fun Player.toUiModel() = PlayerUiModel(
        id = id,
        name = name,
        address = address,
        hostOrder = hostOrder,
    )

    companion object {
        private val dateFormatter =
            DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN)

        fun factory(repository: PlayerRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(PlayersViewModel::class.java))
                    return PlayersViewModel(repository) as T
                }
            }
    }
}
