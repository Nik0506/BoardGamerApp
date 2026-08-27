package com.example.boardgamerapp.ui.dashboard

data class GameNightUiModel(
    val date: String,
    val time: String,
    val hostName: String,
    val location: String,
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

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data object Empty : DashboardUiState
    data class Content(
        val gameNight: GameNightUiModel,
        val players: List<DashboardPlayerUiModel> = emptyList(),
        val selectedPlayerId: Long? = null,
        val lateNotices: List<LateNoticeUiModel> = emptyList(),
        val editor: LateNoticeEditorUiState? = null,
        val message: String? = null,
        val errorMessage: String? = null,
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}
