package com.example.boardgamerapp.domain.repository

import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.Player

interface GameNightRepository {
    suspend fun getPlayers(): List<Player>

    suspend fun getNextGameNight(): GameNight?

    suspend fun savePlayer(player: Player)

    suspend fun planNextGameNight(): GameNight?
}
