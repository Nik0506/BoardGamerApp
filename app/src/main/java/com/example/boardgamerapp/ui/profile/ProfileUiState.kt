package com.example.boardgamerapp.ui.profile

import com.example.boardgamerapp.domain.model.Player

data class ProfileUiState(
    val isLoading: Boolean = true,
    val players: List<Player> = emptyList(),
    val errorMessage: String? = null,
)
