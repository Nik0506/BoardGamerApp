package com.example.boardgamerapp.ui.players

data class PlayerUiModel(
    val id: Long,
    val name: String,
    val address: String,
    val hostOrder: Int,
)

data class PlayerEditorUiState(
    val playerId: Long? = null,
    val name: String = "",
    val address: String = "",
    val errorMessage: String? = null,
) {
    val title: String = if (playerId == null) "Spieler hinzufügen" else "Spieler bearbeiten"
}

data class PlayersUiState(
    val players: List<PlayerUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val editor: PlayerEditorUiState? = null,
    val message: String? = null,
    val errorMessage: String? = null,
)
