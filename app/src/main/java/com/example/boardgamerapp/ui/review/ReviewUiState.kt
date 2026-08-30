package com.example.boardgamerapp.ui.review

data class ReviewPlayerUiModel(val id: Long, val name: String, val hasReviewed: Boolean)

data class RatingEditorUiState(
    val hostRating: Int = 0,
    val foodRating: Int = 0,
    val eveningRating: Int = 0,
    val comment: String = "",
    val errorMessage: String? = null,
)

data class ReviewAveragesUiModel(
    val host: String,
    val food: String,
    val evening: String,
)

sealed interface ReviewUiState {
    data object Loading : ReviewUiState
    data object Empty : ReviewUiState
    data class Content(
        val gameNightId: Long,
        val date: String,
        val hostName: String,
        val isFinished: Boolean,
        val players: List<ReviewPlayerUiModel>,
        val selectedPlayerId: Long?,
        val currentPlayerName: String?,
        val currentPlayerHasReviewed: Boolean,
        val reviewCount: Int,
        val averages: ReviewAveragesUiModel?,
        val editor: RatingEditorUiState? = null,
        val message: String? = null,
        val errorMessage: String? = null,
    ) : ReviewUiState
    data class Error(val message: String) : ReviewUiState
}
