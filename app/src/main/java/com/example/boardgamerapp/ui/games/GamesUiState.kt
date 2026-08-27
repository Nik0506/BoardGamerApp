package com.example.boardgamerapp.ui.games

data class GamePlayerUiModel(
    val id: Long,
    val name: String,
)

data class GameSuggestionUiModel(
    val id: Long,
    val name: String,
    val description: String,
    val suggestedByPlayerId: Long,
    val suggestedByName: String,
    val gameNightDate: String,
    val voterIds: Set<Long> = emptySet(),
    val voteCount: Int = voterIds.size,
    val isSelected: Boolean = false,
)

data class GameSuggestionEditorUiState(
    val name: String = "",
    val description: String = "",
    val errorMessage: String? = null,
)

data class GamesUiState(
    val players: List<GamePlayerUiModel> = emptyList(),
    val selectedPlayerId: Long? = null,
    val suggestions: List<GameSuggestionUiModel> = emptyList(),
    val gameNightDate: String? = null,
    val totalVotes: Int = 0,
    val playerCount: Int = 0,
    val resultText: String = "Noch keine Stimmen",
    val isLoading: Boolean = true,
    val editor: GameSuggestionEditorUiState? = null,
    val message: String? = null,
    val errorMessage: String? = null,
)
