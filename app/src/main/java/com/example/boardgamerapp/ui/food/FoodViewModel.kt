package com.example.boardgamerapp.ui.food

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.boardgamerapp.data.repository.BoardGamerRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FoodViewModel(
    private val repository: BoardGamerRepository,
    private val currentPlayerId: Long,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    var uiState by mutableStateOf(FoodUiState())
        private set

    init { load() }

    fun load() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
        withContext(ioDispatcher) { repository.getFoodVotingSnapshot() }.fold(
            onSuccess = { snapshot ->
                if (snapshot == null) {
                    uiState = FoodUiState(isLoading = false)
                    return@fold
                }
                val players = snapshot.players.map { FoodPlayerUiModel(it.id, it.name) }
                val selected = currentPlayerId.takeIf { id -> players.any { it.id == id } }
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
                loadOrders()
            },
            onFailure = { uiState = uiState.copy(isLoading = false, errorMessage = it.message) },
        )
        }
    }

    private fun loadOrders() {
        viewModelScope.launch {
        withContext(ioDispatcher) { repository.getOrderingSnapshot() }.onSuccess { snapshot ->
            snapshot ?: return@onSuccess
            uiState = uiState.copy(
                hostId = snapshot.host.id,
                restaurantName = snapshot.restaurant?.name,
                menuUrl = snapshot.restaurant?.menuUrl,
                orders = snapshot.orders.map { FoodOrderUiModel(it.order.id, it.player.id, it.player.name, it.order.dish, it.order.note, formatCents(it.order.priceCents)) },
                totalPrice = formatCents(snapshot.totalCents),
            )
        }.onFailure { uiState = uiState.copy(errorMessage = it.message) }
        }
    }

    fun beginRestaurantEditor() {
        if (uiState.selectedPlayerId != uiState.hostId) {
            uiState = uiState.copy(errorMessage = "Nur der Gastgeber kann das Restaurant bearbeiten.")
            return
        }
        uiState = uiState.copy(restaurantEditor = RestaurantEditor(uiState.restaurantName.orEmpty(), uiState.menuUrl.orEmpty()), editorError = null)
    }
    fun updateRestaurantName(value: String) { uiState = uiState.copy(restaurantEditor = uiState.restaurantEditor?.copy(name = value), editorError = null) }
    fun updateMenuUrl(value: String) { uiState = uiState.copy(restaurantEditor = uiState.restaurantEditor?.copy(menuUrl = value), editorError = null) }
    fun dismissRestaurantEditor() { uiState = uiState.copy(restaurantEditor = null, editorError = null) }
    fun saveRestaurant() {
        val editor = uiState.restaurantEditor ?: return
        val playerId = uiState.selectedPlayerId ?: return
        viewModelScope.launch {
        withContext(ioDispatcher) { repository.saveRestaurant(playerId, editor.name, editor.menuUrl) }.fold(
            onSuccess = { uiState = uiState.copy(restaurantEditor = null, message = "Restaurant gespeichert."); loadOrders() },
            onFailure = { uiState = uiState.copy(editorError = it.message) },
        )
        }
    }

    fun beginOrderEditor() {
        val own = uiState.orders.firstOrNull { it.playerId == uiState.selectedPlayerId }
        uiState = uiState.copy(orderEditor = OrderEditor(own?.dish.orEmpty(), own?.note.orEmpty(), own?.price?.removeSuffix(" €")?.trim().orEmpty()), editorError = null)
    }
    fun updateOrderDish(value: String) { uiState = uiState.copy(orderEditor = uiState.orderEditor?.copy(dish = value), editorError = null) }
    fun updateOrderNote(value: String) { uiState = uiState.copy(orderEditor = uiState.orderEditor?.copy(note = value), editorError = null) }
    fun updateOrderPrice(value: String) { uiState = uiState.copy(orderEditor = uiState.orderEditor?.copy(price = value), editorError = null) }
    fun dismissOrderEditor() { uiState = uiState.copy(orderEditor = null, editorError = null) }
    fun saveOrder() {
        val editor = uiState.orderEditor ?: return
        val playerId = uiState.selectedPlayerId ?: return
        val cents = runCatching { editor.price.replace(',', '.').toBigDecimal().setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact() }
            .getOrElse { uiState = uiState.copy(editorError = "Bitte einen gültigen Preis mit höchstens zwei Nachkommastellen eingeben."); return }
        viewModelScope.launch {
        withContext(ioDispatcher) { repository.saveFoodOrder(playerId, editor.dish, editor.note, cents) }.fold(
            onSuccess = { uiState = uiState.copy(orderEditor = null, message = "Bestellung gespeichert."); loadOrders() },
            onFailure = { uiState = uiState.copy(editorError = it.message) },
        )
        }
    }
    fun deleteOrder(orderId: Long) {
        val playerId = uiState.selectedPlayerId ?: return
        viewModelScope.launch {
        withContext(ioDispatcher) { repository.deleteFoodOrder(orderId, playerId) }.fold(
            onSuccess = { uiState = uiState.copy(message = "Bestellung gelöscht."); loadOrders() },
            onFailure = { uiState = uiState.copy(errorMessage = it.message) },
        )
        }
    }

    fun castVote(categoryId: Long) {
        val playerId = uiState.selectedPlayerId ?: return
        viewModelScope.launch {
        withContext(ioDispatcher) { repository.castFoodVote(playerId, categoryId) }.fold(
            onSuccess = { load(); uiState = uiState.copy(message = "Essensstimme gespeichert.") },
            onFailure = { uiState = uiState.copy(errorMessage = it.message) },
        )
        }
    }

    fun beginAddCategory() { uiState = uiState.copy(categoryEditor = "", editorError = null) }
    fun updateCategoryName(value: String) { uiState = uiState.copy(categoryEditor = value, editorError = null) }
    fun dismissCategoryEditor() { uiState = uiState.copy(categoryEditor = null, editorError = null) }

    fun saveCategory() {
        val name = uiState.categoryEditor ?: return
        viewModelScope.launch {
        withContext(ioDispatcher) { repository.addFoodCategory(name) }.fold(
            onSuccess = { load(); uiState = uiState.copy(message = "${it.name} wurde hinzugefügt.") },
            onFailure = { uiState = uiState.copy(editorError = it.message) },
        )
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
        withContext(ioDispatcher) { repository.deleteFoodCategory(categoryId) }.fold(
            onSuccess = { load(); uiState = uiState.copy(message = "Kategorie wurde gelöscht.") },
            onFailure = { uiState = uiState.copy(errorMessage = it.message) },
        )
        }
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
        fun factory(repository: BoardGamerRepository, currentPlayerId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = FoodViewModel(repository, currentPlayerId) as T
        }

        private fun formatCents(cents: Long): String = String.format(Locale.GERMANY, "%.2f €", BigDecimal.valueOf(cents, 2))
    }
}
