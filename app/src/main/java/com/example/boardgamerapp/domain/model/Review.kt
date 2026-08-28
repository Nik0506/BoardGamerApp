package com.example.boardgamerapp.domain.model

data class Review(
    val id: Long,
    val playerId: Long,
    val gameNightId: Long,
    val hostRating: Int,
    val foodRating: Int,
    val eveningRating: Int,
    val comment: String,
)
