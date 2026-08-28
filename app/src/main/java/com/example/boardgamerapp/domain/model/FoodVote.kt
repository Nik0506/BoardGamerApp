package com.example.boardgamerapp.domain.model

data class FoodVote(
    val id: Long,
    val playerId: Long,
    val foodCategoryId: Long,
    val gameNightId: Long,
)
