package com.example.boardgamerapp.domain.model

import java.time.LocalDateTime

data class GameNight(
    val id: Long,
    val startsAt: LocalDateTime,
    val hostId: Long,
    val location: String,
    val status: GameNightStatus,
)

enum class GameNightStatus {
    PLANNED,
    ACTIVE,
    FINISHED,
}
