package com.example.boardgamerapp.data.repository

import com.example.boardgamerapp.domain.HostRotation
import com.example.boardgamerapp.domain.model.BoardGame
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Vote
import java.time.LocalDateTime

class InMemoryGameNightRepository(
    players: List<Player> = samplePlayers,
    gameNights: List<GameNight> = sampleGameNights,
    boardGames: List<BoardGame> = sampleBoardGames,
    votes: List<Vote> = sampleVotes,
    private val now: () -> LocalDateTime = LocalDateTime::now,
) : BoardGamerRepository {

    private val players = players.toMutableList()
    private val gameNights = gameNights.toMutableList()
    private val boardGames = boardGames.toMutableList()
    private val votes = votes.toMutableList()

    override fun getUpcomingGameNight(): Result<UpcomingGameNight?> = runCatching {
        val nextGameNight = findUpcomingGameNight() ?: return@runCatching null

        val host = players.firstOrNull { it.id == nextGameNight.hostId }
            ?: error("Für den nächsten Spieleabend wurde kein Gastgeber gefunden.")

        UpcomingGameNight(gameNight = nextGameNight, host = host)
    }

    override fun getGameSuggestions(): Result<GameNightSuggestions?> = runCatching {
        val gameNight = findUpcomingGameNight() ?: return@runCatching null
        val suggestions = boardGames
            .asSequence()
            .filter { it.gameNightId == gameNight.id }
            .sortedBy { it.name.lowercase() }
            .map { boardGame ->
                val player = players.firstOrNull { it.id == boardGame.suggestedByPlayerId }
                    ?: error("Für ${boardGame.name} wurde kein Spieler gefunden.")
                BoardGameSuggestion(boardGame = boardGame, suggestedBy = player)
            }
            .toList()
        GameNightSuggestions(gameNight = gameNight, suggestions = suggestions)
    }

    override fun addGameSuggestion(
        name: String,
        description: String,
        suggestedByPlayerId: Long,
    ): Result<BoardGameSuggestion> = runCatching {
        val gameNight = findUpcomingGameNight()
            ?: error("Es gibt keinen kommenden Spieleabend.")
        val player = players.firstOrNull { it.id == suggestedByPlayerId }
            ?: error("Der ausgewählte Spieler wurde nicht gefunden.")
        val boardGame = BoardGame(
            id = (boardGames.maxOfOrNull { it.id } ?: 0L) + 1L,
            name = name.required("Spielname"),
            description = description.trim(),
            suggestedByPlayerId = player.id,
            gameNightId = gameNight.id,
        )
        boardGames += boardGame
        BoardGameSuggestion(boardGame = boardGame, suggestedBy = player)
    }

    override fun getVotingSnapshot(): Result<VotingSnapshot?> = runCatching {
        val gameNightSuggestions = getGameSuggestions().getOrThrow()
            ?: return@runCatching null
        val gameNightVotes = votes.filter { it.gameNightId == gameNightSuggestions.gameNight.id }
        val results = gameNightSuggestions.suggestions
            .map { suggestion ->
                BoardGameVoteResult(
                    suggestion = suggestion,
                    voterIds = gameNightVotes
                        .asSequence()
                        .filter { it.boardGameId == suggestion.boardGame.id }
                        .map { it.playerId }
                        .toSet(),
                )
            }
            .sortedWith(
                compareByDescending<BoardGameVoteResult> { it.voteCount }
                    .thenBy { it.suggestion.boardGame.name.lowercase() },
            )
        VotingSnapshot(
            gameNight = gameNightSuggestions.gameNight,
            results = results,
            playerCount = players.size,
        )
    }

    override fun castVote(playerId: Long, boardGameId: Long): Result<Vote> = runCatching {
        val gameNight = findUpcomingGameNight()
            ?: error("Es gibt keinen kommenden Spieleabend.")
        require(players.any { it.id == playerId }) {
            "Der ausgewählte Spieler wurde nicht gefunden."
        }
        val boardGame = boardGames.firstOrNull {
            it.id == boardGameId && it.gameNightId == gameNight.id
        } ?: error("Das ausgewählte Spiel gehört nicht zum kommenden Spieleabend.")

        votes.removeAll { it.playerId == playerId && it.gameNightId == gameNight.id }
        val vote = Vote(
            id = (votes.maxOfOrNull { it.id } ?: 0L) + 1L,
            playerId = playerId,
            boardGameId = boardGame.id,
            gameNightId = gameNight.id,
        )
        votes += vote
        vote
    }

    override fun deleteGameSuggestion(
        boardGameId: Long,
        requestingPlayerId: Long,
    ): Result<Unit> = runCatching {
        val boardGame = boardGames.firstOrNull { it.id == boardGameId }
            ?: error("Der Spielvorschlag wurde nicht gefunden.")
        require(boardGame.suggestedByPlayerId == requestingPlayerId) {
            "Nur der eigene Spielvorschlag kann gelöscht werden."
        }
        boardGames.remove(boardGame)
        votes.removeAll { it.boardGameId == boardGame.id }
        Unit
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

    private fun findUpcomingGameNight(): GameNight? = gameNights
        .asSequence()
        .filter { it.status == GameNightStatus.PLANNED }
        .filter { !it.startsAt.isBefore(now()) }
        .minByOrNull { it.startsAt }

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

        private val sampleBoardGames = listOf(
            BoardGame(
                id = 1,
                name = "Catan",
                description = "Handel und Aufbau für drei bis vier Personen.",
                suggestedByPlayerId = 1,
                gameNightId = 1,
            ),
            BoardGame(
                id = 2,
                name = "Heat",
                description = "Schnelles Autorennen mit taktischem Handmanagement.",
                suggestedByPlayerId = 2,
                gameNightId = 1,
            ),
        )

        private val sampleVotes = listOf(
            Vote(
                id = 1,
                playerId = 1,
                boardGameId = 1,
                gameNightId = 1,
            ),
            Vote(
                id = 2,
                playerId = 2,
                boardGameId = 2,
                gameNightId = 1,
            ),
        )
    }
}
