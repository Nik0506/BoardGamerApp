package com.example.boardgamerapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.boardgamerapp.ui.dashboard.DashboardPlayerUiModel
import com.example.boardgamerapp.ui.dashboard.DashboardScreen
import com.example.boardgamerapp.ui.dashboard.DashboardUiState
import com.example.boardgamerapp.ui.dashboard.GameNightEditorUiState
import com.example.boardgamerapp.ui.dashboard.GameNightUiModel
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DashboardScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun optionsMenuAndEditDialogAreAccessible() {
        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    DashboardUiState.Content(
                        gameNight = GameNightUiModel(
                            id = 1L,
                            date = "Freitag, 28. August 2026",
                            time = "19:00 Uhr",
                            hostName = "Max Mustermann",
                            hostId = 1L,
                            location = "Musterstraße 12",
                        ),
                        players = listOf(
                            DashboardPlayerUiModel(1L, "Max Mustermann"),
                            DashboardPlayerUiModel(2L, "Erika Musterfrau"),
                        ),
                        selectedPlayerId = 1L,
                    ),
                )
            }

            DashboardScreen(
                uiState = state,
                onRetry = {},
                onEditGameNight = {
                    state = state.copy(
                        gameNightEditor = GameNightEditorUiState(
                            gameNightId = 1L,
                            selectedDate = LocalDate.of(2026, 8, 28),
                            selectedTime = LocalTime.of(19, 0),
                            selectedHostId = 1L,
                        ),
                    )
                },
                onGameNightDateChange = { newDate ->
                    state = state.copy(
                        gameNightEditor = state.gameNightEditor?.copy(selectedDate = newDate),
                    )
                },
                onGameNightTimeChange = { newTime ->
                    state = state.copy(
                        gameNightEditor = state.gameNightEditor?.copy(selectedTime = newTime),
                    )
                },
                onGameNightHostChange = { newHostId ->
                    state = state.copy(
                        gameNightEditor = state.gameNightEditor?.copy(selectedHostId = newHostId),
                    )
                },
                onSaveEditedGameNight = {
                    state = state.copy(
                        gameNightEditor = null,
                        message = "Spieleabend wurde erfolgreich aktualisiert. Teilnehmer wurden per Push-Nachricht informiert.",
                    )
                },
                onDismissGameNightEditor = {
                    state = state.copy(gameNightEditor = null)
                },
            )
        }

        // Check options menu icon
        composeRule.onNodeWithContentDescription("Optionen").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Optionen").performClick()

        // Check menu item "Spieleabend editieren"
        composeRule.onNodeWithText("Spieleabend editieren").assertIsDisplayed()
        composeRule.onNodeWithText("Spieleabend editieren").performClick()

        // Check Dialog fields
        composeRule.onNodeWithText("Spieleabend editieren").assertIsDisplayed()
        composeRule.onNodeWithText("Datum ändern").assertIsDisplayed()
        composeRule.onNodeWithText("Uhrzeit ändern").assertIsDisplayed()
        composeRule.onNodeWithText("Gastgeber auswählen").assertIsDisplayed()
        composeRule.onNodeWithText("Speichern").assertIsDisplayed()
        composeRule.onNodeWithText("Abbrechen").assertIsDisplayed()

        // Save
        composeRule.onNodeWithText("Speichern").performClick()

        // Check success message
        composeRule.onNodeWithText(
            "Spieleabend wurde erfolgreich aktualisiert. Teilnehmer wurden per Push-Nachricht informiert.",
        ).assertIsDisplayed()
    }
}
