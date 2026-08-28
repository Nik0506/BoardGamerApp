package com.example.boardgamerapp.ui.food

data class FoodPlayerUiModel(val id: Long, val name: String)

data class FoodCategoryUiModel(
    val id: Long,
    val name: String,
    val voteCount: Int,
    val voterIds: Set<Long>,
    val isSelected: Boolean,
)

data class FoodOrderUiModel(val id: Long, val playerId: Long, val playerName: String, val dish: String, val note: String, val price: String)

data class RestaurantEditor(val name: String = "", val menuUrl: String = "")
data class OrderEditor(val dish: String = "", val note: String = "", val price: String = "")

data class FoodUiState(
    val isLoading: Boolean = true,
    val gameNightDate: String? = null,
    val players: List<FoodPlayerUiModel> = emptyList(),
    val selectedPlayerId: Long? = null,
    val categories: List<FoodCategoryUiModel> = emptyList(),
    val totalVotes: Int = 0,
    val resultText: String = "Noch keine Stimmen",
    val missingPlayerNames: List<String> = emptyList(),
    val hostId: Long? = null,
    val restaurantName: String? = null,
    val menuUrl: String? = null,
    val orders: List<FoodOrderUiModel> = emptyList(),
    val totalPrice: String = "0,00 €",
    val restaurantEditor: RestaurantEditor? = null,
    val orderEditor: OrderEditor? = null,
    val categoryEditor: String? = null,
    val editorError: String? = null,
    val message: String? = null,
    val errorMessage: String? = null,
)
