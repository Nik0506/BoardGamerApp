package com.example.boardgamerapp.data.repository

import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import java.time.LocalDateTime

class InMemoryGameNightRepository(
    private val players: List<Player> = samplePlayers,
    private val gameNights: List<GameNight> = sampleGameNights,
    private val now: () -> LocalDateTime = LocalDateTime::now,
) : GameNightRepository {

    override fun getUpcomingGameNight(): Result<UpcomingGameNight?> = runCatching {
        val nextGameNight = gameNights
            .asSequence()
            .filter { it.status == GameNightStatus.PLANNED }
            .filter { !it.startsAt.isBefore(now()) }
            .minByOrNull { it.startsAt }
            ?: return@runCatching null

        val host = players.firstOrNull { it.id == nextGameNight.hostId }
            ?: error("Für den nächsten Spieleabend wurde kein Gastgeber gefunden.")

        UpcomingGameNight(gameNight = nextGameNight, host = host)
    }

    companion object {
        private val samplePlayers = listOf(
            Player(
                id = 1,
                name = "Max Mustermann",
                address = "Musterstraße 12, 33100 Paderborn",
                hostOrder = 1,
            ),
            Player(
                id = 2,
                name = "Lea Beispiel",
                address = "Spielweg 4, 33102 Paderborn",
                hostOrder = 2,
            ),
        )

        private val sampleGameNights = listOf(
            GameNight(
                id = 1,
                startsAt = LocalDateTime.of(2026, 8, 28, 19, 0),
                hostId = 1,
                location = "Musterstraße 12, 33100 Paderborn",
                status = GameNightStatus.PLANNED,
            ),
        )
    }
}
