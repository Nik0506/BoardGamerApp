package com.example.boardgamerapp.domain.model

data class BoardGame(
    val id: Long,
    val name: String,
    val description: String,
    val suggestedByPlayerId: Long,
    val gameNightId: Long,
)
