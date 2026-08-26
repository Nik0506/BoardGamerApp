package com.example.boardgamerapp.ui.dashboard

data class GameNightUiModel(
    val date: String,
    val time: String,
    val hostName: String,
    val location: String,
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data object Empty : DashboardUiState
    data class Content(val gameNight: GameNightUiModel) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}
