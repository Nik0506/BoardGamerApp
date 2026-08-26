package com.example.boardgamerapp.data.repository

import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.Player

data class UpcomingGameNight(
    val gameNight: GameNight,
    val host: Player,
)

interface GameNightRepository {
    fun getUpcomingGameNight(): Result<UpcomingGameNight?>
}
