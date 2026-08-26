package com.example.boardgamerapp.data.repository

import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.BoardGame
import com.example.boardgamerapp.domain.model.Player

data class UpcomingGameNight(
    val gameNight: GameNight,
    val host: Player,
)

data class BoardGameSuggestion(
    val boardGame: BoardGame,
    val suggestedBy: Player,
)

data class GameNightSuggestions(
    val gameNight: GameNight,
    val suggestions: List<BoardGameSuggestion>,
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

interface GameSuggestionRepository {
    fun getGameSuggestions(): Result<GameNightSuggestions?>

    fun addGameSuggestion(
        name: String,
        description: String,
        suggestedByPlayerId: Long,
    ): Result<BoardGameSuggestion>

    fun deleteGameSuggestion(boardGameId: Long, requestingPlayerId: Long): Result<Unit>
}

interface BoardGamerRepository : GameNightRepository, PlayerRepository, GameSuggestionRepository

enum class MoveDirection {
    UP,
    DOWN,
}
