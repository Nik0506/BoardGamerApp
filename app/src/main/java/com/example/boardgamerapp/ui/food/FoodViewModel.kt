package com.example.boardgamerapp.ui.food

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.boardgamerapp.data.repository.BoardGamerRepository
import com.example.boardgamerapp.data.repository.FoodVotingRepository
import java.time.format.DateTimeFormatter
import java.util.Locale

class FoodViewModel(private val repository: FoodVotingRepository) : ViewModel() {
    var uiState by mutableStateOf(FoodUiState())
        private set

    init { load() }

    fun load() {
        val preferredPlayer = uiState.selectedPlayerId
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        repository.getFoodVotingSnapshot().fold(
            onSuccess = { snapshot ->
                if (snapshot == null) {
                    uiState = FoodUiState(isLoading = false)
                    return@fold
                }
                val players = snapshot.players.map { FoodPlayerUiModel(it.id, it.name) }
                val selected = preferredPlayer?.takeIf { id -> players.any { it.id == id } }
                    ?: players.firstOrNull()?.id
                val highest = snapshot.results.maxOfOrNull { it.voteCount } ?: 0
                val leaders = snapshot.results.filter { it.voteCount == highest && highest > 0 }
                uiState = uiState.copy(
                    isLoading = false,
                    gameNightDate = snapshot.gameNight.startsAt.format(dateFormatter),
                    players = players,
                    selectedPlayerId = selected,
                    categories = snapshot.results.map {
                        FoodCategoryUiModel(
                            it.category.id, it.category.name, it.voteCount, it.voterIds,
                            selected in it.voterIds,
                        )
                    },
                    totalVotes = snapshot.totalVotes,
                    resultText = when {
                        leaders.isEmpty() -> "Noch keine Stimmen"
                        leaders.size == 1 -> "Aktuell vorne: ${leaders.single().category.name}"
                        else -> "Gleichstand: ${leaders.joinToString(" und ") { it.category.name }}"
                    },
                    missingPlayerNames = snapshot.missingPlayers.map { it.name },
                )
            },
            onFailure = { uiState = uiState.copy(isLoading = false, errorMessage = it.message) },
        )
    }

    fun selectPlayer(playerId: Long) {
        if (uiState.players.any { it.id == playerId }) {
            uiState = uiState.copy(
                selectedPlayerId = playerId,
                categories = uiState.categories.map { it.copy(isSelected = playerId in it.voterIds) },
            )
        }
    }

    fun castVote(categoryId: Long) {
        val playerId = uiState.selectedPlayerId ?: return
        repository.castFoodVote(playerId, categoryId).fold(
            onSuccess = { load(); uiState = uiState.copy(message = "Essensstimme gespeichert.") },
            onFailure = { uiState = uiState.copy(errorMessage = it.message) },
        )
    }

    fun beginAddCategory() { uiState = uiState.copy(categoryEditor = "", editorError = null) }
    fun updateCategoryName(value: String) { uiState = uiState.copy(categoryEditor = value, editorError = null) }
    fun dismissCategoryEditor() { uiState = uiState.copy(categoryEditor = null, editorError = null) }

    fun saveCategory() {
        val name = uiState.categoryEditor ?: return
        repository.addFoodCategory(name).fold(
            onSuccess = { load(); uiState = uiState.copy(message = "${it.name} wurde hinzugefügt.") },
            onFailure = { uiState = uiState.copy(editorError = it.message) },
        )
    }

    fun deleteCategory(categoryId: Long) {
        repository.deleteFoodCategory(categoryId).fold(
            onSuccess = { load(); uiState = uiState.copy(message = "Kategorie wurde gelöscht.") },
            onFailure = { uiState = uiState.copy(errorMessage = it.message) },
        )
    }

    fun remindMissingPlayers() {
        uiState = uiState.copy(
            message = if (uiState.missingPlayerNames.isEmpty()) {
                "Alle haben bereits abgestimmt."
            } else {
                "Lokale Erinnerung: Es fehlen ${uiState.missingPlayerNames.joinToString()} — es wurde keine Nachricht versendet."
            },
        )
    }

    fun clearMessage() { uiState = uiState.copy(message = null, errorMessage = null) }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)
        fun factory(repository: BoardGamerRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = FoodViewModel(repository) as T
        }
    }
}
