package com.example.boardgamerapp.data.repository

import com.example.boardgamerapp.domain.HostRotation
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import java.time.LocalDateTime

class InMemoryGameNightRepository(
    players: List<Player> = samplePlayers,
    gameNights: List<GameNight> = sampleGameNights,
    private val now: () -> LocalDateTime = LocalDateTime::now,
) : BoardGamerRepository {

    private val players = players.toMutableList()
    private val gameNights = gameNights.toMutableList()

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

    override fun getPlayers(): Result<List<Player>> = runCatching {
        players.sortedBy { it.hostOrder }
    }

    override fun addPlayer(name: String, address: String): Result<Player> = runCatching {
        val validatedName = name.required("Name")
        val validatedAddress = address.required("Adresse")
        val player = Player(
            id = (players.maxOfOrNull { it.id } ?: 0L) + 1L,
            name = validatedName,
            address = validatedAddress,
            hostOrder = players.size + 1,
        )
        players += player
        player
    }

    override fun updatePlayer(
        id: Long,
        name: String,
        address: String,
    ): Result<Player> = runCatching {
        val playerIndex = players.indexOfFirst { it.id == id }
        require(playerIndex >= 0) { "Der Spieler wurde nicht gefunden." }

        players[playerIndex] = players[playerIndex].copy(
            name = name.required("Name"),
            address = address.required("Adresse"),
        )
        players[playerIndex]
    }

    override fun movePlayer(
        id: Long,
        direction: MoveDirection,
    ): Result<List<Player>> = runCatching {
        val orderedPlayers = players.sortedBy { it.hostOrder }.toMutableList()
        val currentIndex = orderedPlayers.indexOfFirst { it.id == id }
        require(currentIndex >= 0) { "Der Spieler wurde nicht gefunden." }

        val targetIndex = when (direction) {
            MoveDirection.UP -> currentIndex - 1
            MoveDirection.DOWN -> currentIndex + 1
        }
        if (targetIndex in orderedPlayers.indices) {
            val player = orderedPlayers[currentIndex]
            orderedPlayers[currentIndex] = orderedPlayers[targetIndex]
            orderedPlayers[targetIndex] = player
        }

        players.clear()
        players += orderedPlayers.mapIndexed { index, item ->
            item.copy(hostOrder = index + 1)
        }
        players.toList()
    }

    override fun createNextGameNight(): Result<UpcomingGameNight> = runCatching {
        val lastGameNight = gameNights.maxByOrNull { it.startsAt }
        val host = HostRotation.nextHost(players, lastGameNight?.hostId)
            ?: error("Lege zuerst mindestens einen Spieler an.")
        val startsAt = lastGameNight?.startsAt?.plusWeeks(2)
            ?: now().plusWeeks(2).withHour(19).withMinute(0).withSecond(0).withNano(0)
        val gameNight = GameNight(
            id = (gameNights.maxOfOrNull { it.id } ?: 0L) + 1L,
            startsAt = startsAt,
            hostId = host.id,
            location = host.address,
            status = GameNightStatus.PLANNED,
        )
        gameNights += gameNight
        UpcomingGameNight(gameNight = gameNight, host = host)
    }

    private fun String.required(fieldName: String): String {
        val value = trim()
        require(value.isNotEmpty()) { "$fieldName darf nicht leer sein." }
        return value
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
