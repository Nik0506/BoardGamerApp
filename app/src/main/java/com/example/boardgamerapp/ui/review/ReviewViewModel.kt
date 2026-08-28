package com.example.boardgamerapp.ui.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.boardgamerapp.data.repository.PlayerRepository
import com.example.boardgamerapp.data.repository.ReviewRepository
import com.example.boardgamerapp.domain.model.GameNightStatus
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReviewViewModel(
    private val reviewRepository: ReviewRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {
    var uiState: ReviewUiState by mutableStateOf(ReviewUiState.Loading)
        private set

    init { load() }

    fun load() {
        val preferredPlayer = (uiState as? ReviewUiState.Content)?.selectedPlayerId
        uiState = ReviewUiState.Loading
        val snapshotResult = reviewRepository.getReviewSnapshot()
        val playersResult = playerRepository.getPlayers()
        val error = snapshotResult.exceptionOrNull() ?: playersResult.exceptionOrNull()
        if (error != null) {
            uiState = ReviewUiState.Error(error.message ?: "Bewertungen konnten nicht geladen werden.")
            return
        }
        val snapshot = snapshotResult.getOrThrow()
        if (snapshot == null) {
            uiState = ReviewUiState.Empty
            return
        }
        val reviewedPlayerIds = snapshot.reviews.mapTo(mutableSetOf()) { it.playerId }
        val players = playersResult.getOrThrow().map {
            ReviewPlayerUiModel(it.id, it.name, it.id in reviewedPlayerIds)
        }
        val selectable = players.filterNot { it.hasReviewed }
        uiState = ReviewUiState.Content(
            gameNightId = snapshot.gameNight.id,
            date = snapshot.gameNight.startsAt.format(dateFormatter),
            hostName = snapshot.host.name,
            isFinished = snapshot.gameNight.status == GameNightStatus.FINISHED,
            players = players,
            selectedPlayerId = preferredPlayer?.takeIf { id -> selectable.any { it.id == id } }
                ?: selectable.firstOrNull()?.id,
            reviewCount = snapshot.reviews.size,
            averages = snapshot.averages?.let {
                ReviewAveragesUiModel(format(it.host), format(it.food), format(it.evening))
            },
        )
    }

    fun finishGameNight() {
        val content = uiState as? ReviewUiState.Content ?: return
        reviewRepository.finishGameNight(content.gameNightId).fold(
            onSuccess = {
                load()
                uiState = (uiState as ReviewUiState.Content).copy(message = "Spieleabend abgeschlossen.")
            },
            onFailure = { uiState = content.copy(errorMessage = it.message) },
        )
    }

    fun selectPlayer(playerId: Long) {
        val content = uiState as? ReviewUiState.Content ?: return
        if (content.players.any { it.id == playerId && !it.hasReviewed }) {
            uiState = content.copy(selectedPlayerId = playerId, errorMessage = null)
        }
    }

    fun beginReview() {
        val content = uiState as? ReviewUiState.Content ?: return
        if (!content.isFinished || content.selectedPlayerId == null) return
        uiState = content.copy(editor = RatingEditorUiState(), message = null, errorMessage = null)
    }

    fun setHostRating(value: Int) = updateEditor { copy(hostRating = value, errorMessage = null) }
    fun setFoodRating(value: Int) = updateEditor { copy(foodRating = value, errorMessage = null) }
    fun setEveningRating(value: Int) = updateEditor { copy(eveningRating = value, errorMessage = null) }
    fun updateComment(value: String) = updateEditor { copy(comment = value, errorMessage = null) }

    fun saveReview() {
        val content = uiState as? ReviewUiState.Content ?: return
        val editor = content.editor ?: return
        val playerId = content.selectedPlayerId ?: return
        if (listOf(editor.hostRating, editor.foodRating, editor.eveningRating).any { it !in 1..5 }) {
            uiState = content.copy(editor = editor.copy(errorMessage = "Vergib in allen drei Kategorien 1 bis 5 Punkte."))
            return
        }
        reviewRepository.submitReview(
            playerId, content.gameNightId, editor.hostRating, editor.foodRating,
            editor.eveningRating, editor.comment,
        ).fold(
            onSuccess = {
                load()
                uiState = (uiState as ReviewUiState.Content).copy(message = "Bewertung gespeichert.")
            },
            onFailure = { uiState = content.copy(editor = editor.copy(errorMessage = it.message)) },
        )
    }

    fun dismissEditor() {
        val content = uiState as? ReviewUiState.Content ?: return
        uiState = content.copy(editor = null)
    }

    fun clearMessage() {
        val content = uiState as? ReviewUiState.Content ?: return
        uiState = content.copy(message = null, errorMessage = null)
    }

    private fun updateEditor(update: RatingEditorUiState.() -> RatingEditorUiState) {
        val content = uiState as? ReviewUiState.Content ?: return
        val editor = content.editor ?: return
        uiState = content.copy(editor = editor.update())
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMAN)
        private fun format(value: Double) = String.format(Locale.GERMAN, "%.1f", value)

        fun factory(repository: com.example.boardgamerapp.data.repository.BoardGamerRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ReviewViewModel(repository, repository) as T
            }
    }
}
