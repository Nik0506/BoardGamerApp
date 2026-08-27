package com.example.boardgamerapp.domain.model

enum class GameNightStatus {
    PLANNED,
    ACTIVE,
    FINISHED,
}

data class GameNight(
    val id: String,
    val startsAtEpochMillis: Long,
    val hostId: String,
    val location: String,
    val status: GameNightStatus,
)
