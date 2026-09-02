package com.example.boardgamerapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.boardgamerapp.ui.food.FoodCategoryUiModel
import com.example.boardgamerapp.ui.food.FoodOrderUiModel
import com.example.boardgamerapp.ui.food.FoodPlayerUiModel
import com.example.boardgamerapp.ui.food.FoodScreen
import com.example.boardgamerapp.ui.food.FoodUiState
import com.example.boardgamerapp.ui.food.OrderEditor
import com.example.boardgamerapp.ui.food.RestaurantEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FoodScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun foodVotingDisplaysCategoriesAndAllowsVoting() {
        var votedCategoryId = -1L

        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    FoodUiState(
                        isLoading = false,
                        gameNightDate = "Freitag, 25. September 2026",
                        selectedPlayerId = 1L,
                        players = listOf(
                            FoodPlayerUiModel(1L, "Max"),
                            FoodPlayerUiModel(2L, "Erika"),
                        ),
                        categories = listOf(
                            FoodCategoryUiModel(10L, "Pizza", 1, setOf(2L), isSelected = false),
                            FoodCategoryUiModel(20L, "Burger", 0, emptySet(), isSelected = false),
                        ),
                        totalVotes = 1,
                        resultText = "Aktuell vorne: Pizza",
                    ),
                )
            }

            FoodScreen(
                uiState = state,
                onCastVote = { id ->
                    votedCategoryId = id
                    state = state.copy(
                        categories = state.categories.map {
                            if (it.id == id) it.copy(isSelected = true, voteCount = it.voteCount + 1)
                            else it
                        },
                        message = "Essensstimme gespeichert.",
                    )
                },
                onAddCategory = {},
                onCategoryNameChange = {},
                onSaveCategory = {},
                onDismissCategoryEditor = {},
                onDeleteCategory = {},
                onRemindMissingPlayers = {},
                onEditRestaurant = {},
                onRestaurantNameChange = {},
                onMenuUrlChange = {},
                onSaveRestaurant = {},
                onDismissRestaurantEditor = {},
                onEditOrder = {},
                onOrderDishChange = {},
                onOrderNoteChange = {},
                onOrderPriceChange = {},
                onSaveOrder = {},
                onDismissOrderEditor = {},
                onDeleteOrder = {},
                onDismissMessage = { state = state.copy(message = null) },
            )
        }

        composeRule.onNodeWithText("Essensabstimmung").assertIsDisplayed()
        composeRule.onNodeWithText("Aktuell vorne: Pizza").assertIsDisplayed()
        composeRule.onNodeWithText("Pizza").assertIsDisplayed()
        composeRule.onNodeWithText("Burger").assertIsDisplayed()

        // Vote for Burger
        composeRule.onNodeWithText("Burger").assertIsDisplayed()
        composeRule.onAllNodes(androidx.compose.ui.test.hasText("Abstimmen"))[1].performClick()

        assertEquals(20L, votedCategoryId)
        composeRule.onNodeWithText("Essensstimme gespeichert.").assertIsDisplayed()
    }

    @Test
    fun addCategoryDialogOpensAndSavesNewCategory() {
        var savedName = ""

        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    FoodUiState(
                        isLoading = false,
                        gameNightDate = "Freitag, 25. September 2026",
                        selectedPlayerId = 1L,
                        players = listOf(FoodPlayerUiModel(1L, "Max")),
                    ),
                )
            }

            FoodScreen(
                uiState = state,
                onCastVote = {},
                onAddCategory = { state = state.copy(categoryEditor = "") },
                onCategoryNameChange = { state = state.copy(categoryEditor = it) },
                onSaveCategory = {
                    savedName = state.categoryEditor.orEmpty()
                    state = state.copy(categoryEditor = null, message = "$savedName wurde hinzugefügt.")
                },
                onDismissCategoryEditor = { state = state.copy(categoryEditor = null) },
                onDeleteCategory = {},
                onRemindMissingPlayers = {},
                onEditRestaurant = {},
                onRestaurantNameChange = {},
                onMenuUrlChange = {},
                onSaveRestaurant = {},
                onDismissRestaurantEditor = {},
                onEditOrder = {},
                onOrderDishChange = {},
                onOrderNoteChange = {},
                onOrderPriceChange = {},
                onSaveOrder = {},
                onDismissOrderEditor = {},
                onDeleteOrder = {},
                onDismissMessage = { state = state.copy(message = null) },
            )
        }

        composeRule.onNodeWithText("Kategorie hinzufügen").performClick()
        composeRule.onNodeWithText("Essenskategorie").assertIsDisplayed()

        composeRule.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextInput("Sushi")
        composeRule.onNodeWithText("Hinzufügen").performClick()

        assertEquals("Sushi", savedName)
        composeRule.onNodeWithText("Sushi wurde hinzugefügt.").assertIsDisplayed()
    }

    @Test
    fun restaurantSectionRestrictsEditingToHost() {
        var editOpened = false

        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    FoodUiState(
                        isLoading = false,
                        gameNightDate = "Freitag, 25. September 2026",
                        hostId = 1L,
                        selectedPlayerId = 1L, // Current user is host
                        restaurantName = "Pizzeria Napoli",
                        menuUrl = "https://napoli.de",
                    ),
                )
            }

            FoodScreen(
                uiState = state,
                onCastVote = {},
                onAddCategory = {},
                onCategoryNameChange = {},
                onSaveCategory = {},
                onDismissCategoryEditor = {},
                onDeleteCategory = {},
                onRemindMissingPlayers = {},
                onEditRestaurant = {
                    editOpened = true
                    state = state.copy(restaurantEditor = RestaurantEditor(state.restaurantName.orEmpty(), state.menuUrl.orEmpty()))
                },
                onRestaurantNameChange = { state = state.copy(restaurantEditor = state.restaurantEditor?.copy(name = it)) },
                onMenuUrlChange = { state = state.copy(restaurantEditor = state.restaurantEditor?.copy(menuUrl = it)) },
                onSaveRestaurant = { state = state.copy(restaurantEditor = null) },
                onDismissRestaurantEditor = { state = state.copy(restaurantEditor = null) },
                onEditOrder = {},
                onOrderDishChange = {},
                onOrderNoteChange = {},
                onOrderPriceChange = {},
                onSaveOrder = {},
                onDismissOrderEditor = {},
                onDeleteOrder = {},
                onDismissMessage = {},
            )
        }

        composeRule.onNodeWithText("Pizzeria Napoli").assertIsDisplayed()
        composeRule.onNodeWithText("Restaurant bearbeiten").assertIsEnabled()
        composeRule.onNodeWithText("Restaurant bearbeiten").performClick()

        assertTrue(editOpened)
        composeRule.onNodeWithText("Restaurantname").assertIsDisplayed()
        composeRule.onNodeWithText("Abbrechen").performClick()
    }

    @Test
    fun orderEditorAllowsAddingAndDeletingOwnOrder() {
        var deletedOrderId = -1L

        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    FoodUiState(
                        isLoading = false,
                        gameNightDate = "Freitag, 25. September 2026",
                        selectedPlayerId = 1L,
                        players = listOf(FoodPlayerUiModel(1L, "Max")),
                        orders = listOf(
                            FoodOrderUiModel(
                                id = 99L,
                                playerId = 1L,
                                playerName = "Max",
                                dish = "Pizza Tonno",
                                note = "Viel Knoblauch",
                                price = "10,50 €",
                            ),
                        ),
                        totalPrice = "10,50 €",
                    ),
                )
            }

            FoodScreen(
                uiState = state,
                onCastVote = {},
                onAddCategory = {},
                onCategoryNameChange = {},
                onSaveCategory = {},
                onDismissCategoryEditor = {},
                onDeleteCategory = {},
                onRemindMissingPlayers = {},
                onEditRestaurant = {},
                onRestaurantNameChange = {},
                onMenuUrlChange = {},
                onSaveRestaurant = {},
                onDismissRestaurantEditor = {},
                onEditOrder = {
                    state = state.copy(orderEditor = OrderEditor("Pizza Tonno", "Viel Knoblauch", "10,50"))
                },
                onOrderDishChange = {},
                onOrderNoteChange = {},
                onOrderPriceChange = {},
                onSaveOrder = {},
                onDismissOrderEditor = { state = state.copy(orderEditor = null) },
                onDeleteOrder = { id ->
                    deletedOrderId = id
                    state = state.copy(orders = emptyList(), totalPrice = "0,00 €", message = "Bestellung gelöscht.")
                },
                onDismissMessage = {},
            )
        }

        composeRule.onNodeWithText("Pizza Tonno").assertIsDisplayed()
        composeRule.onNodeWithText("10,50 €").assertIsDisplayed()
        composeRule.onNodeWithText("Eigene Bestellung löschen").performClick()

        assertEquals(99L, deletedOrderId)
        composeRule.onNodeWithText("Bestellung gelöscht.").assertIsDisplayed()
    }
}
