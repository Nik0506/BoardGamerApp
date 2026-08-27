package com.example.boardgamerapp.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.boardgamerapp.data.repository.GameSuggestionRepository
import com.example.boardgamerapp.data.repository.PlayerRepository
import com.example.boardgamerapp.data.repository.VotingRepository
import com.example.boardgamerapp.data.repository.VotingSnapshot
import java.time.format.DateTimeFormatter
import java.util.Locale

class GamesViewModel(
    private val gameRepository: GameSuggestionRepository,
    private val playerRepository: PlayerRepository,
    private val votingRepository: VotingRepository,
) : ViewModel() {

    var uiState by mutableStateOf(GamesUiState())
        private set

    init {
        loadGames()
    }

    fun loadGames() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        val playersResult = playerRepository.getPlayers()
        val votingResult = votingRepository.getVotingSnapshot()

        val error = playersResult.exceptionOrNull() ?: votingResult.exceptionOrNull()
        if (error != null) {
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = error.message ?: "Die Spielvorschläge konnten nicht geladen werden.",
            )
            return
        }

        val players = playersResult.getOrThrow().map {
            GamePlayerUiModel(id = it.id, name = it.name)
        }
        val selectedPlayerId = uiState.selectedPlayerId
            ?.takeIf { selectedId -> players.any { it.id == selectedId } }
            ?: players.firstOrNull()?.id
        val votingSnapshot = votingResult.getOrThrow()

        uiState = uiState.copy(
            players = players,
            selectedPlayerId = selectedPlayerId,
            suggestions = votingSnapshot.toUiModels(selectedPlayerId),
            gameNightDate = votingSnapshot?.gameNight?.startsAt?.format(dateFormatter),
            totalVotes = votingSnapshot?.totalVotes ?: 0,
            playerCount = votingSnapshot?.playerCount ?: players.size,
            resultText = votingSnapshot.resultText(),
            isLoading = false,
        )
    }

    fun selectPlayer(playerId: Long) {
        if (uiState.players.any { it.id == playerId }) {
            uiState = uiState.copy(
                selectedPlayerId = playerId,
                suggestions = uiState.suggestions.map {
                    it.copy(isSelected = playerId in it.voterIds)
                },
                message = null,
            )
        }
    }

    fun castVote(boardGameId: Long) {
        val playerId = uiState.selectedPlayerId ?: return
        votingRepository.castVote(playerId, boardGameId).fold(
            onSuccess = {
                uiState = uiState.copy(message = "Deine Stimme wurde gespeichert.")
                loadGames()
            },
            onFailure = { error ->
                uiState = uiState.copy(
                    errorMessage = error.message ?: "Die Stimme konnte nicht gespeichert werden.",
                )
            },
        )
    }

    fun beginAddSuggestion() {
        if (uiState.selectedPlayerId == null) {
            uiState = uiState.copy(errorMessage = "Lege zuerst einen Spieler an.")
            return
        }
        if (uiState.gameNightDate == null) {
            uiState = uiState.copy(errorMessage = "Lege zuerst einen kommenden Spieleabend an.")
            return
        }
        uiState = uiState.copy(editor = GameSuggestionEditorUiState(), message = null)
    }

    fun updateEditorName(name: String) {
        uiState = uiState.copy(
            editor = uiState.editor?.copy(name = name, errorMessage = null),
        )
    }

    fun updateEditorDescription(description: String) {
        uiState = uiState.copy(
            editor = uiState.editor?.copy(description = description, errorMessage = null),
        )
    }

    fun dismissEditor() {
        uiState = uiState.copy(editor = null)
    }

    fun saveSuggestion() {
        val editor = uiState.editor ?: return
        val playerId = uiState.selectedPlayerId ?: return
        gameRepository.addGameSuggestion(
            name = editor.name,
            description = editor.description,
            suggestedByPlayerId = playerId,
        ).fold(
            onSuccess = { suggestion ->
                uiState = uiState.copy(
                    editor = null,
                    message = "${suggestion.boardGame.name} wurde vorgeschlagen.",
                )
                loadGames()
            },
            onFailure = { error ->
                uiState = uiState.copy(
                    editor = editor.copy(
                        errorMessage = error.message
                            ?: "Der Spielvorschlag konnte nicht gespeichert werden.",
                    ),
                )
            },
        )
    }

    fun deleteSuggestion(boardGameId: Long) {
        val playerId = uiState.selectedPlayerId ?: return
        gameRepository.deleteGameSuggestion(boardGameId, playerId).fold(
            onSuccess = {
                uiState = uiState.copy(message = "Der Spielvorschlag wurde gelöscht.")
                loadGames()
            },
            onFailure = { error ->
                uiState = uiState.copy(
                    errorMessage = error.message ?: "Der Vorschlag konnte nicht gelöscht werden.",
                )
            },
        )
    }

    fun clearMessage() {
        uiState = uiState.copy(message = null, errorMessage = null)
    }

    private fun VotingSnapshot?.toUiModels(selectedPlayerId: Long?): List<GameSuggestionUiModel> {
        if (this == null) return emptyList()
        val formattedDate = gameNight.startsAt.format(dateFormatter)
        return results.map { result ->
            val suggestion = result.suggestion
            GameSuggestionUiModel(
                id = suggestion.boardGame.id,
                name = suggestion.boardGame.name,
                description = suggestion.boardGame.description,
                suggestedByPlayerId = suggestion.suggestedBy.id,
                suggestedByName = suggestion.suggestedBy.name,
                gameNightDate = formattedDate,
                voterIds = result.voterIds,
                isSelected = selectedPlayerId in result.voterIds,
            )
        }
    }

    private fun VotingSnapshot?.resultText(): String {
        if (this == null || results.isEmpty() || totalVotes == 0) return "Noch keine Stimmen"
        val highestVoteCount = results.maxOf { it.voteCount }
        val leaders = results.filter { it.voteCount == highestVoteCount }
        return if (leaders.size == 1) {
            "Aktuell vorne: ${leaders.single().suggestion.boardGame.name}"
        } else {
            "Gleichstand: ${leaders.joinToString(" und ") { it.suggestion.boardGame.name }}"
        }
    }

    companion object {
        private val dateFormatter =
            DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)

        fun factory(
            gameRepository: GameSuggestionRepository,
            playerRepository: PlayerRepository,
            votingRepository: VotingRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(GamesViewModel::class.java))
                return GamesViewModel(gameRepository, playerRepository, votingRepository) as T
            }
        }
    }
}
