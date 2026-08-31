package com.example.boardgamerapp.ui.dashboard

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class GameNightUiModel(
    val id: Long = 0L,
    val date: String,
    val time: String,
    val hostName: String,
    val hostId: Long = 0L,
    val location: String,
    val startsAt: LocalDateTime? = null,
)

data class DashboardPlayerUiModel(
    val id: Long,
    val name: String,
)

data class LateNoticeUiModel(
    val id: Long,
    val playerName: String,
    val minutes: Int,
    val createdAt: String,
)

data class LateNoticeEditorUiState(
    val selectedPreset: Int? = 10,
    val customMinutes: String = "",
    val errorMessage: String? = null,
)

data class GameNightEditorUiState(
    val gameNightId: Long = 0L,
    val selectedDate: LocalDate,
    val selectedTime: LocalTime,
    val selectedHostId: Long,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data object Empty : DashboardUiState
    data class Content(
        val gameNight: GameNightUiModel,
        val players: List<DashboardPlayerUiModel> = emptyList(),
        val selectedPlayerId: Long? = null,
        val lateNotices: List<LateNoticeUiModel> = emptyList(),
        val editor: LateNoticeEditorUiState? = null,
        val gameNightEditor: GameNightEditorUiState? = null,
        val message: String? = null,
        val errorMessage: String? = null,
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

