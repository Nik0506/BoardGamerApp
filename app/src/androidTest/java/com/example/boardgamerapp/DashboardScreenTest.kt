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
import com.example.boardgamerapp.ui.dashboard.GameNightPickerUiModel
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

    @Test
    fun cancelGameNightOptionOpensDialogAndConfirms() {
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
                        players = listOf(DashboardPlayerUiModel(1L, "Max Mustermann")),
                        selectedPlayerId = 1L,
                    ),
                )
            }

            DashboardScreen(
                uiState = state,
                onRetry = {},
                onBeginCancelGameNight = {
                    state = state.copy(
                        cancelGameNightEditor = com.example.boardgamerapp.ui.dashboard.CancelGameNightEditorUiState(),
                    )
                },
                onCancelReasonChange = { reason ->
                    state = state.copy(cancelGameNightEditor = state.cancelGameNightEditor?.copy(reason = reason))
                },
                onConfirmCancelGameNight = {
                    state = state.copy(
                        cancelGameNightEditor = null,
                        gameNight = state.gameNight.copy(
                            status = com.example.boardgamerapp.domain.model.GameNightStatus.CANCELLED,
                            cancelReason = state.cancelGameNightEditor?.reason,
                        ),
                        message = "Der Spieleabend wurde abgesagt. Teilnehmer wurden per Push-Nachricht informiert.",
                    )
                },
                onDismissCancelGameNight = {
                    state = state.copy(cancelGameNightEditor = null)
                },
            )
        }

        composeRule.onNodeWithContentDescription("Optionen").performClick()
        composeRule.onNodeWithText("Spieleabend absagen").assertIsDisplayed()
        composeRule.onNodeWithText("Spieleabend absagen").performClick()

        composeRule.onNodeWithText("Meldungstext (optional)").assertIsDisplayed()
        composeRule.onNodeWithText("Absagen bestätigen").performClick()

        composeRule.onNodeWithText(
            "Der Spieleabend wurde abgesagt. Teilnehmer wurden per Push-Nachricht informiert.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("❌ Dieser Spieleabend wurde abgesagt.").assertIsDisplayed()
    }

    @Test
    fun attendanceRsvpActionsAreAccessible() {
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
                        attendances = listOf(
                            com.example.boardgamerapp.ui.dashboard.DashboardAttendanceUiModel(
                                playerId = 1L,
                                playerName = "Max Mustermann",
                                status = com.example.boardgamerapp.domain.model.AttendanceStatusType.ATTENDING,
                                isCurrentPlayer = true,
                            ),
                            com.example.boardgamerapp.ui.dashboard.DashboardAttendanceUiModel(
                                playerId = 2L,
                                playerName = "Erika Musterfrau",
                                status = com.example.boardgamerapp.domain.model.AttendanceStatusType.PENDING,
                            ),
                        ),
                    ),
                )
            }

            DashboardScreen(
                uiState = state,
                onRetry = {},
                onConfirmAttending = {
                    state = state.copy(
                        message = "Deine Zusage wurde gespeichert.",
                    )
                },
                onBeginStatusReport = {
                    state = state.copy(
                        statusReportEditor = com.example.boardgamerapp.ui.dashboard.StatusReportEditorUiState(),
                    )
                },
                onSelectStatusReportType = { type ->
                    state = state.copy(
                        statusReportEditor = state.statusReportEditor?.copy(type = type),
                    )
                },
                onSaveStatusReport = {
                    state = state.copy(
                        statusReportEditor = null,
                        message = "Deine Absage wurde gespeichert.",
                    )
                },
                onDismissStatusReport = {
                    state = state.copy(statusReportEditor = null)
                },
            )
        }

        // Verify status section and buttons
        composeRule.onNodeWithText("Mein Teilnahmestatus").assertIsDisplayed()
        composeRule.onNodeWithText("Teilnahme der Gruppe").assertIsDisplayed()
        composeRule.onNodeWithText("Zusagen").assertIsDisplayed()
        composeRule.onNodeWithText("Status melden").assertIsDisplayed()

        // Click Zusagen
        composeRule.onNodeWithText("Zusagen").performClick()
        composeRule.onNodeWithText("Deine Zusage wurde gespeichert.").assertIsDisplayed()

        // Click "Status melden" to open the unified dialog, then switch to "Absage"
        composeRule.onNodeWithText("Status melden").performClick()
        composeRule.onNodeWithText("Absage").performClick()
        composeRule.onNodeWithText("Möchtest du deine Teilnahme für diesen Spieleabend absagen?").assertIsDisplayed()
        composeRule.onNodeWithText("Absage bestätigen").assertIsDisplayed()
        composeRule.onNodeWithText("Abbrechen").performClick()
    }

    @Test
    fun gameNightPickerShowsUpcomingNightsAndSwitchesSelection() {
        var selectedGroupId = "groupA"

        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    DashboardUiState.Content(
                        gameNight = GameNightUiModel(
                            date = "Freitag, 28. August 2026",
                            time = "19:00 Uhr",
                            hostName = "Max Mustermann",
                            location = "Musterstraße 12",
                            groupName = "Würfelfreunde",
                        ),
                        upcomingGameNights = listOf(
                            GameNightPickerUiModel(
                                groupId = "groupA",
                                gameNightDocId = "docA",
                                groupName = "Würfelfreunde",
                                date = "Freitag, 28. August 2026",
                                time = "19:00 Uhr",
                                hostName = "Max Mustermann",
                                isSelected = true,
                                hasCollision = false,
                            ),
                            GameNightPickerUiModel(
                                groupId = "groupB",
                                gameNightDocId = "docB",
                                groupName = "Brettspielnacht",
                                date = "Samstag, 5. September 2026",
                                time = "20:00 Uhr",
                                hostName = "Erika Musterfrau",
                                isSelected = false,
                                hasCollision = false,
                            ),
                        ),
                    ),
                )
            }

            DashboardScreen(
                uiState = state,
                onRetry = {},
                onSelectGameNight = { groupId, _ ->
                    selectedGroupId = groupId
                    state = state.copy(message = "Termin gewechselt")
                },
            )
        }

        composeRule.onNodeWithText("Deine anstehenden Termine").assertIsDisplayed()
        composeRule.onNodeWithText("Würfelfreunde").assertIsDisplayed()
        composeRule.onNodeWithText("Brettspielnacht").assertIsDisplayed()

        composeRule.onNodeWithText("Brettspielnacht").performClick()
        composeRule.onNodeWithText("Termin gewechselt").assertIsDisplayed()
        org.junit.Assert.assertEquals("groupB", selectedGroupId)
    }

    @Test
    fun emptyStateOffersPlanningAction() {
        var planClicked = false

        composeRule.setContent {
            DashboardScreen(
                uiState = DashboardUiState.Empty,
                onRetry = {},
                onPlanGameNight = { planClicked = true },
            )
        }

        composeRule.onNodeWithText("Noch kein Spieleabend geplant").assertIsDisplayed()
        composeRule.onNodeWithText("Spieleabend planen").assertIsDisplayed()
        composeRule.onNodeWithText("Spieleabend planen").performClick()
        org.junit.Assert.assertTrue(planClicked)
    }
}
