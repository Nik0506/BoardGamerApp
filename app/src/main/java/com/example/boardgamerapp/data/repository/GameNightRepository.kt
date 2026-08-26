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

interface PlayerRepository {
    fun getPlayers(): Result<List<Player>>

    fun addPlayer(name: String, address: String): Result<Player>

    fun updatePlayer(id: Long, name: String, address: String): Result<Player>

    fun movePlayer(id: Long, direction: MoveDirection): Result<List<Player>>

    fun createNextGameNight(): Result<UpcomingGameNight>
}

interface BoardGamerRepository : GameNightRepository, PlayerRepository

enum class MoveDirection {
    UP,
    DOWN,
}
