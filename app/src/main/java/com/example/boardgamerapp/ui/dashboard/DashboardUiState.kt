package com.example.boardgamerapp.ui.dashboard

import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.Player

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    data class Success(
        val gameNight: GameNight,
        val host: Player,
    ) : DashboardUiState

    data object Empty : DashboardUiState

    data class Error(val message: String) : DashboardUiState
}
