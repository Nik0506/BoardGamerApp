package com.example.boardgamerapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.boardgamerapp.ui.games.GamePlayerUiModel
import com.example.boardgamerapp.ui.games.GameSuggestionEditorUiState
import com.example.boardgamerapp.ui.games.GameSuggestionUiModel
import com.example.boardgamerapp.ui.games.GamesScreen
import com.example.boardgamerapp.ui.games.GamesUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GamesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun gamesListDisplaysSuggestionsAndAllowsVoting() {
        var votedGameId = -1L

        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    GamesUiState(
                        isLoading = false,
                        gameNightDate = "Freitag, 25. September 2026",
                        selectedPlayerId = 1L,
                        players = listOf(GamePlayerUiModel(1L, "Max"), GamePlayerUiModel(2L, "Erika")),
                        suggestions = listOf(
                            GameSuggestionUiModel(
                                id = 100L,
                                name = "Catan",
                                description = "Das beliebte Handelsspiel",
                                suggestedByPlayerId = 2L,
                                suggestedByName = "Erika",
                                gameNightDate = "Freitag, 25. September 2026",
                                voterIds = setOf(2L),
                                isSelected = false,
                            ),
                        ),
                        totalVotes = 1,
                        playerCount = 2,
                        resultText = "Aktuell vorne: Catan",
                    ),
                )
            }

            GamesScreen(
                uiState = state,
                onAddSuggestion = {},
                onDeleteSuggestion = {},
                onCastVote = { id ->
                    votedGameId = id
                    state = state.copy(
                        suggestions = state.suggestions.map {
                            if (it.id == id) it.copy(isSelected = true, voterIds = it.voterIds + 1L)
                            else it
                        },
                        totalVotes = 2,
                        message = "Deine Stimme wurde gespeichert.",
                    )
                },
                onNameChange = {},
                onDescriptionChange = {},
                onSaveSuggestion = {},
                onDismissEditor = {},
                onDismissMessage = { state = state.copy(message = null) },
            )
        }

        composeRule.onNodeWithText("Spielvorschläge").assertIsDisplayed()
        composeRule.onNodeWithText("Aktuell vorne: Catan").assertIsDisplayed()
        composeRule.onNodeWithText("Catan").assertIsDisplayed()
        composeRule.onNodeWithText("Das beliebte Handelsspiel").assertIsDisplayed()
        composeRule.onNodeWithText("Vorgeschlagen von Erika").assertIsDisplayed()

        composeRule.onNodeWithText("Abstimmen").performClick()
        assertEquals(100L, votedGameId)
        composeRule.onNodeWithText("Deine Stimme wurde gespeichert.").assertIsDisplayed()
    }

    @Test
    fun addSuggestionDialogOpensAndAcceptsInput() {
        var addedName = ""

        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    GamesUiState(
                        isLoading = false,
                        gameNightDate = "Freitag, 25. September 2026",
                        selectedPlayerId = 1L,
                        players = listOf(GamePlayerUiModel(1L, "Max")),
                    ),
                )
            }

            GamesScreen(
                uiState = state,
                onAddSuggestion = { state = state.copy(editor = GameSuggestionEditorUiState()) },
                onDeleteSuggestion = {},
                onCastVote = {},
                onNameChange = { state = state.copy(editor = state.editor?.copy(name = it)) },
                onDescriptionChange = { state = state.copy(editor = state.editor?.copy(description = it)) },
                onSaveSuggestion = {
                    addedName = state.editor?.name.orEmpty()
                    state = state.copy(editor = null, message = "$addedName wurde vorgeschlagen.")
                },
                onDismissEditor = { state = state.copy(editor = null) },
                onDismissMessage = { state = state.copy(message = null) },
            )
        }

        composeRule.onNodeWithText("Spiel vorschlagen").performClick()
        composeRule.onAllNodesWithText("Spiel vorschlagen")[0].assertIsDisplayed()

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Terraforming Mars")
        composeRule.onNodeWithText("Hinzufügen").performClick()

        assertEquals("Terraforming Mars", addedName)
        composeRule.onNodeWithText("Terraforming Mars wurde vorgeschlagen.").assertIsDisplayed()
    }

    @Test
    fun deleteButtonOnlyVisibleForSuggestionOwner() {
        var deleteClicked = false

        composeRule.setContent {
            GamesScreen(
                uiState = GamesUiState(
                    isLoading = false,
                    gameNightDate = "Freitag, 25. September 2026",
                    selectedPlayerId = 1L, // Current user is 1
                    players = listOf(GamePlayerUiModel(1L, "Max"), GamePlayerUiModel(2L, "Erika")),
                    suggestions = listOf(
                        GameSuggestionUiModel(
                            id = 101L,
                            name = "Flügelschlag",
                            description = "Vogel-Kartenspiel",
                            suggestedByPlayerId = 1L, // Owner is current user
                            suggestedByName = "Max",
                            gameNightDate = "Freitag, 25. September 2026",
                            voterIds = emptySet(),
                            isSelected = false,
                        ),
                    ),
                ),
                onAddSuggestion = {},
                onDeleteSuggestion = { deleteClicked = true },
                onCastVote = {},
                onNameChange = {},
                onDescriptionChange = {},
                onSaveSuggestion = {},
                onDismissEditor = {},
                onDismissMessage = {},
            )
        }

        composeRule.onNodeWithText("Flügelschlag").assertIsDisplayed()
        composeRule.onNodeWithText("Löschen").assertIsDisplayed()
        composeRule.onNodeWithText("Löschen").performClick()
        assertTrue(deleteClicked)
    }

    @Test
    fun emptyStateInformsUserWhenNoSuggestionsExist() {
        composeRule.setContent {
            GamesScreen(
                uiState = GamesUiState(
                    isLoading = false,
                    gameNightDate = "Freitag, 25. September 2026",
                    selectedPlayerId = 1L,
                    suggestions = emptyList(),
                ),
                onAddSuggestion = {},
                onDeleteSuggestion = {},
                onCastVote = {},
                onNameChange = {},
                onDescriptionChange = {},
                onSaveSuggestion = {},
                onDismissEditor = {},
                onDismissMessage = {},
            )
        }

        composeRule.onNodeWithText("Noch keine Spiele vorgeschlagen. Mach den Anfang!").assertIsDisplayed()
    }
}
