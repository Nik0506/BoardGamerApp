package com.example.boardgamerapp.ui.food

data class FoodPlayerUiModel(val id: Long, val name: String)

data class FoodCategoryUiModel(
    val id: Long,
    val name: String,
    val voteCount: Int,
    val voterIds: Set<Long>,
    val isSelected: Boolean,
)

data class FoodUiState(
    val isLoading: Boolean = true,
    val gameNightDate: String? = null,
    val players: List<FoodPlayerUiModel> = emptyList(),
    val selectedPlayerId: Long? = null,
    val categories: List<FoodCategoryUiModel> = emptyList(),
    val totalVotes: Int = 0,
    val resultText: String = "Noch keine Stimmen",
    val missingPlayerNames: List<String> = emptyList(),
    val categoryEditor: String? = null,
    val editorError: String? = null,
    val message: String? = null,
    val errorMessage: String? = null,
)
