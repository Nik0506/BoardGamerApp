package com.example.boardgamerapp.domain.model

data class Vote(
    val id: Long,
    val playerId: Long,
    val boardGameId: Long,
    val gameNightId: Long,
)
