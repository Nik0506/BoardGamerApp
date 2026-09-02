package com.example.boardgamerapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.boardgamerapp.data.repository.MoveDirection
import com.example.boardgamerapp.ui.players.PlayerEditorUiState
import com.example.boardgamerapp.ui.players.PlayerUiModel
import com.example.boardgamerapp.ui.players.PlayersScreen
import com.example.boardgamerapp.ui.players.PlayersUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayersScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playersListDisplaysMembersAndGroupId() {
        composeRule.setContent {
            PlayersScreen(
                uiState = PlayersUiState(
                    isLoading = false,
                    players = listOf(
                        PlayerUiModel(
                            id = 1L,
                            name = "Max Mustermann",
                            address = "Musterstraße 12",
                            hostOrder = 1,
                        ),
                        PlayerUiModel(
                            id = 2L,
                            name = "Erika Musterfrau",
                            address = "Neustraße 5",
                            hostOrder = 2,
                        ),
                    ),
                ),
                onAddPlayer = {},
                onEditPlayer = {},
                onMovePlayer = { _, _ -> },
                onCreateNextGameNight = {},
                onNameChange = {},
                onAddressChange = {},
                onSavePlayer = {},
                onDismissEditor = {},
                onDismissMessage = {},
                groupId = "group-xyz-123",
            )
        }

        composeRule.onNodeWithText("Spielgruppe").assertIsDisplayed()
        composeRule.onNodeWithText("Die Reihenfolge bestimmt, wer den nächsten Spieleabend ausrichtet.").assertIsDisplayed()
        composeRule.onNodeWithText("group-xyz-123").assertIsDisplayed()
        composeRule.onNodeWithText("Kopieren").assertIsDisplayed()
        composeRule.onNodeWithText("1. Max Mustermann").assertIsDisplayed()
        composeRule.onNodeWithText("Musterstraße 12").assertIsDisplayed()
        composeRule.onNodeWithText("2. Erika Musterfrau").assertIsDisplayed()
        composeRule.onNodeWithText("Neustraße 5").assertIsDisplayed()
    }

    @Test
    fun movePlayerTriggersDirectionCallback() {
        var movedDirection: MoveDirection? = null
        var movedPlayerId = -1L

        composeRule.setContent {
            PlayersScreen(
                uiState = PlayersUiState(
                    isLoading = false,
                    players = listOf(
                        PlayerUiModel(1L, "Max", "Adresse 1", 1),
                        PlayerUiModel(2L, "Erika", "Adresse 2", 2),
                    ),
                ),
                onAddPlayer = {},
                onEditPlayer = {},
                onMovePlayer = { id, direction ->
                    movedPlayerId = id
                    movedDirection = direction
                },
                onCreateNextGameNight = {},
                onNameChange = {},
                onAddressChange = {},
                onSavePlayer = {},
                onDismissEditor = {},
                onDismissMessage = {},
            )
        }

        // Click "Nach unten" on first player
        composeRule.onAllNodesWithText("Nach unten")[0].performClick()
        assertEquals(1L, movedPlayerId)
        assertEquals(MoveDirection.DOWN, movedDirection)
    }

    @Test
    fun playerEditorDialogOpensAndAllowsSaving() {
        var savedName = ""

        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    PlayersUiState(
                        isLoading = false,
                        players = listOf(PlayerUiModel(1L, "Max", "Adresse 1", 1)),
                        editor = PlayerEditorUiState(playerId = 1L, name = "Max", address = "Adresse 1"),
                    ),
                )
            }

            PlayersScreen(
                uiState = state,
                onAddPlayer = {},
                onEditPlayer = {},
                onMovePlayer = { _, _ -> },
                onCreateNextGameNight = {},
                onNameChange = { state = state.copy(editor = state.editor?.copy(name = it)) },
                onAddressChange = { state = state.copy(editor = state.editor?.copy(address = it)) },
                onSavePlayer = {
                    savedName = state.editor?.name.orEmpty()
                    state = state.copy(editor = null, message = "Spieler gespeichert.")
                },
                onDismissEditor = { state = state.copy(editor = null) },
                onDismissMessage = { state = state.copy(message = null) },
            )
        }

        composeRule.onNodeWithText("Spieler bearbeiten").assertIsDisplayed()
        composeRule.onNodeWithText("Speichern").performClick()

        assertEquals("Max", savedName)
        composeRule.onNodeWithText("Spieler gespeichert.").assertIsDisplayed()
    }

    @Test
    fun planNextGameNightButtonCallsAction() {
        var createCalled = false

        composeRule.setContent {
            PlayersScreen(
                uiState = PlayersUiState(
                    isLoading = false,
                    players = listOf(PlayerUiModel(1L, "Max", "Adresse 1", 1)),
                ),
                onAddPlayer = {},
                onEditPlayer = {},
                onMovePlayer = { _, _ -> },
                onCreateNextGameNight = { createCalled = true },
                onNameChange = {},
                onAddressChange = {},
                onSavePlayer = {},
                onDismissEditor = {},
                onDismissMessage = {},
            )
        }

        composeRule.onNodeWithText("Nächsten Spieleabend planen").performClick()
        assertTrue(createCalled)
    }
}
