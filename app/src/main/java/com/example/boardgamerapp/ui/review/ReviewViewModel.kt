package com.example.boardgamerapp.ui.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.boardgamerapp.data.repository.PlayerRepository
import com.example.boardgamerapp.data.repository.ReviewRepository
import com.example.boardgamerapp.domain.model.GameNightStatus
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewViewModel(
    private val reviewRepository: ReviewRepository,
    private val playerRepository: PlayerRepository,
    private val currentPlayerId: Long,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    var uiState: ReviewUiState by mutableStateOf(ReviewUiState.Loading)
        private set

    init { load() }

    fun load(successMessage: String? = null) {
        uiState = ReviewUiState.Loading
        viewModelScope.launch {
        val snapshotResult = withContext(ioDispatcher) { reviewRepository.getReviewSnapshot() }
        val playersResult = withContext(ioDispatcher) { playerRepository.getPlayers() }
        val error = snapshotResult.exceptionOrNull() ?: playersResult.exceptionOrNull()
        if (error != null) {
            uiState = ReviewUiState.Error(error.message ?: "Bewertungen konnten nicht geladen werden.")
            return@launch
        }
        val snapshot = snapshotResult.getOrThrow()
        if (snapshot == null) {
            uiState = ReviewUiState.Empty
            return@launch
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
            selectedPlayerId = currentPlayerId.takeIf { id -> selectable.any { it.id == id } },
            currentPlayerName = players.firstOrNull { it.id == currentPlayerId }?.name,
            currentPlayerHasReviewed = currentPlayerId in reviewedPlayerIds,
            reviewCount = snapshot.reviews.size,
            averages = snapshot.averages?.let {
                ReviewAveragesUiModel(format(it.host), format(it.food), format(it.evening))
            },
            message = successMessage,
        )
        }
    }

    fun finishGameNight() {
        val content = uiState as? ReviewUiState.Content ?: return
        viewModelScope.launch {
        withContext(ioDispatcher) { reviewRepository.finishGameNight(content.gameNightId) }.fold(
            onSuccess = {
                load(successMessage = "Spieleabend abgeschlossen.")
            },
            onFailure = { uiState = content.copy(errorMessage = it.message) },
        )
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
        viewModelScope.launch {
        withContext(ioDispatcher) { reviewRepository.submitReview(
            playerId, content.gameNightId, editor.hostRating, editor.foodRating,
            editor.eveningRating, editor.comment,
        ) }.fold(
            onSuccess = {
                load(successMessage = "Bewertung gespeichert.")
            },
            onFailure = { uiState = content.copy(editor = editor.copy(errorMessage = it.message)) },
        )
        }
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

        fun factory(repository: com.example.boardgamerapp.data.repository.BoardGamerRepository, currentPlayerId: Long) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ReviewViewModel(repository, repository, currentPlayerId) as T
            }
    }
}
