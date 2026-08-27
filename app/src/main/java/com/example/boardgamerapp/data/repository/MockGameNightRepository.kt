package com.example.boardgamerapp.data.repository

import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.nextHost
import com.example.boardgamerapp.domain.repository.GameNightRepository

class MockGameNightRepository(
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : GameNightRepository {

    private val players = mutableListOf(
        Player(
            id = "player-1",
            name = "Alex",
            address = "Musterstraße 1, 10115 Berlin",
            hostOrder = 1,
        ),
        Player(
            id = "player-2",
            name = "Sam",
            address = "Spielweg 5, 10117 Berlin",
            hostOrder = 2,
        ),
        Player(
            id = "player-3",
            name = "Chris",
            address = "Würfelallee 8, 10119 Berlin",
            hostOrder = 3,
        ),
    )

    override suspend fun getPlayers(): List<Player> = players.toList()

    private var nextGameNight = createGameNight(hostId = "player-1")

    override suspend fun getNextGameNight(): GameNight = nextGameNight

    override suspend fun savePlayer(player: Player) {
        val index = players.indexOfFirst { it.id == player.id }
        if (index >= 0) {
            players[index] = player
        } else {
            players += player
        }
    }

    override suspend fun planNextGameNight(): GameNight? {
        val host = nextHost(players, nextGameNight.hostId) ?: return null
        nextGameNight = createGameNight(host.id)
        return nextGameNight
    }

    private fun createGameNight(hostId: String) = GameNight(
        id = "game-night-${nowMillis()}",
        startsAtEpochMillis = nowMillis() + DAYS_UNTIL_DEMO_NIGHT * MILLIS_PER_DAY,
        hostId = hostId,
        location = players.first { it.id == hostId }.address,
        status = GameNightStatus.PLANNED,
    )

    private companion object {
        const val DAYS_UNTIL_DEMO_NIGHT = 7L
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
    }
}
