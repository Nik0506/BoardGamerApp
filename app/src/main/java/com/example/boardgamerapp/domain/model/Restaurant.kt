package com.example.boardgamerapp.domain.model

data class Restaurant(
    val id: Long,
    val gameNightId: Long,
    val name: String,
    val menuUrl: String,
)

data class FoodOrder(
    val id: Long,
    val gameNightId: Long,
    val playerId: Long,
    val dish: String,
    val note: String,
    val priceCents: Long,
)
