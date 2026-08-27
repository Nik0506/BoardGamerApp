package com.example.boardgamerapp.data.repository

import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.repository.GameNightRepository

class MockGameNightRepository(
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : GameNightRepository {

    private val players = listOf(
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

    override suspend fun getPlayers(): List<Player> = players

    override suspend fun getNextGameNight(): GameNight = GameNight(
        id = "game-night-1",
        startsAtEpochMillis = nowMillis() + DAYS_UNTIL_DEMO_NIGHT * MILLIS_PER_DAY,
        hostId = "player-1",
        location = "Musterstraße 1, 10115 Berlin",
        status = GameNightStatus.PLANNED,
    )

    private companion object {
        const val DAYS_UNTIL_DEMO_NIGHT = 7L
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
    }
}
