package com.example.boardgamerapp.data.repository

import android.content.Context
import com.example.boardgamerapp.data.local.AppDatabase
import com.example.boardgamerapp.data.local.entity.BoardGameEntity
import com.example.boardgamerapp.data.local.entity.GameNightEntity
import com.example.boardgamerapp.data.local.entity.LateNoticeEntity
import com.example.boardgamerapp.data.local.entity.PlayerEntity
import com.example.boardgamerapp.data.local.entity.ReviewEntity
import com.example.boardgamerapp.data.local.entity.VoteEntity
import com.example.boardgamerapp.domain.HostRotation
import com.example.boardgamerapp.domain.model.BoardGame
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.LateNotice
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Review
import com.example.boardgamerapp.domain.model.Vote
import java.time.LocalDateTime

class RoomGameNightRepository(
    private val database: AppDatabase,
    private val now: () -> LocalDateTime = LocalDateTime::now,
    seedIfEmpty: Boolean = false,
) : BoardGamerRepository {

    init {
        if (seedIfEmpty) seedIfDatabaseIsEmpty()
    }

    override fun getUpcomingGameNight(): Result<UpcomingGameNight?> = runCatching {
        val gameNight = findUpcomingGameNight() ?: return@runCatching null
        val host = database.playerDao().getById(gameNight.hostId)?.toDomain()
            ?: error("Für den nächsten Spieleabend wurde kein Gastgeber gefunden.")
        UpcomingGameNight(gameNight = gameNight.toDomain(), host = host)
    }

    override fun getGameSuggestions(): Result<GameNightSuggestions?> = runCatching {
        val gameNight = findUpcomingGameNight() ?: return@runCatching null
        val suggestions = database.boardGameDao()
            .getAll()
            .asSequence()
            .filter { it.gameNightId == gameNight.id }
            .sortedBy { it.name.lowercase() }
            .map { boardGame ->
                val player = database.playerDao().getById(boardGame.suggestedByPlayerId)?.toDomain()
                    ?: error("Für ${boardGame.name} wurde kein Spieler gefunden.")
                BoardGameSuggestion(boardGame.toDomain(), player)
            }

            .toList()
        GameNightSuggestions(gameNight.toDomain(), suggestions)
    }

    override fun getLateNotices(): Result<List<LateNotice>> = runCatching {
        val gameNight = findUpcomingGameNight() ?: return@runCatching emptyList()
        database.lateNoticeDao()
            .getForGameNight(gameNight.id)
            .map { it.toDomain() }
    }

    override fun addLateNotice(playerId: Long, minutes: Int): Result<LateNotice> = runCatching {
        val gameNight = findUpcomingGameNight()
            ?: error("Es gibt keinen kommenden Spieleabend.")
        require(minutes > 0) { "Die Verspätung muss größer als 0 Minuten sein." }
        require(database.playerDao().getById(playerId) != null) {
            "Der ausgewählte Spieler wurde nicht gefunden."
        }
        val lateNotice = LateNoticeEntity(
            playerId = playerId,
            gameNightId = gameNight.id,
            minutes = minutes,
            createdAt = now(),
        )
        lateNotice.copy(id = database.lateNoticeDao().insert(lateNotice)).toDomain()
    }

    override fun addGameSuggestion(
        name: String,
        description: String,
        suggestedByPlayerId: Long,
    ): Result<BoardGameSuggestion> = runCatching {
        val gameNight = findUpcomingGameNight()
            ?: error("Es gibt keinen kommenden Spieleabend.")
        val player = database.playerDao().getById(suggestedByPlayerId)?.toDomain()
            ?: error("Der ausgewählte Spieler wurde nicht gefunden.")
        val boardGame = BoardGameEntity(
            name = name.required("Spielname"),
            description = description.trim(),
            suggestedByPlayerId = player.id,
            gameNightId = gameNight.id,
        )
        val savedBoardGame = boardGame.copy(id = database.boardGameDao().insert(boardGame))
        BoardGameSuggestion(savedBoardGame.toDomain(), player)
    }

    override fun deleteGameSuggestion(
        boardGameId: Long,
        requestingPlayerId: Long,
    ): Result<Unit> = runCatching {
        val boardGame = database.boardGameDao().getById(boardGameId)
            ?: error("Der Spielvorschlag wurde nicht gefunden.")
        require(boardGame.suggestedByPlayerId == requestingPlayerId) {
            "Nur der eigene Spielvorschlag kann gelöscht werden."
        }
        database.boardGameDao().delete(boardGame)
    }

    override fun getVotingSnapshot(): Result<VotingSnapshot?> = runCatching {
        val gameNightSuggestions = getGameSuggestions().getOrThrow()
            ?: return@runCatching null
        val gameNightVotes = database.voteDao()
            .getForGameNight(gameNightSuggestions.gameNight.id)
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
            playerCount = database.playerDao().count(),
        )
    }

    override fun castVote(playerId: Long, boardGameId: Long): Result<Vote> = runCatching {
        val gameNight = findUpcomingGameNight()
            ?: error("Es gibt keinen kommenden Spieleabend.")
        require(database.playerDao().getById(playerId) != null) {
            "Der ausgewählte Spieler wurde nicht gefunden."
        }
        val boardGame = database.boardGameDao().getById(boardGameId)
        require(boardGame != null && boardGame.gameNightId == gameNight.id) {
            "Das ausgewählte Spiel gehört nicht zum kommenden Spieleabend."
        }
        database.voteDao().replaceForPlayerAndGameNight(
            VoteEntity(
                playerId = playerId,
                boardGameId = boardGameId,
                gameNightId = gameNight.id,
            ),
        ).toDomain()
    }

    override fun getPlayers(): Result<List<Player>> = runCatching {
        database.playerDao().getAll().map { it.toDomain() }
    }

    override fun addPlayer(name: String, address: String): Result<Player> = runCatching {
        val player = PlayerEntity(
            name = name.required("Name"),
            address = address.required("Adresse"),
            hostOrder = database.playerDao().count() + 1,
        )
        player.copy(id = database.playerDao().insert(player)).toDomain()
    }

    override fun updatePlayer(
        id: Long,
        name: String,
        address: String,
    ): Result<Player> = runCatching {
        val existing = database.playerDao().getById(id)
            ?: error("Der Spieler wurde nicht gefunden.")
        val updated = existing.copy(
            name = name.required("Name"),
            address = address.required("Adresse"),
        )
        database.playerDao().update(updated)
        updated.toDomain()
    }

    override fun movePlayer(
        id: Long,
        direction: MoveDirection,
    ): Result<List<Player>> = runCatching {
        val orderedPlayers = database.playerDao().getAll().toMutableList()
        val currentIndex = orderedPlayers.indexOfFirst { it.id == id }
        require(currentIndex >= 0) { "Der Spieler wurde nicht gefunden." }

        val targetIndex = when (direction) {
            MoveDirection.UP -> currentIndex - 1
            MoveDirection.DOWN -> currentIndex + 1
        }
        if (targetIndex in orderedPlayers.indices) {
            val currentPlayer = orderedPlayers[currentIndex]
            orderedPlayers[currentIndex] = orderedPlayers[targetIndex]
            orderedPlayers[targetIndex] = currentPlayer
        }

        val reordered = orderedPlayers.mapIndexed { index, player ->
            player.copy(hostOrder = index + 1)
        }
        database.playerDao().updateHostOrder(reordered)
        reordered.map { it.toDomain() }
    }

    override fun createNextGameNight(): Result<UpcomingGameNight> = runCatching {
        val players = database.playerDao().getAll().map { it.toDomain() }
        val lastGameNight = database.gameNightDao().getAll().maxByOrNull { it.startsAt }
        val host = HostRotation.nextHost(players, lastGameNight?.hostId)
            ?: error("Lege zuerst mindestens einen Spieler an.")
        val startsAt = lastGameNight?.startsAt?.plusWeeks(2)
            ?: now().plusWeeks(2).withHour(19).withMinute(0).withSecond(0).withNano(0)
        val gameNight = GameNightEntity(
            startsAt = startsAt,
            hostId = host.id,
            location = host.address,
            status = GameNightStatus.PLANNED,
        )
        val saved = gameNight.copy(id = database.gameNightDao().insert(gameNight)).toDomain()
        UpcomingGameNight(saved, host)
    }

    override fun getReviewSnapshot(): Result<ReviewSnapshot?> = runCatching {
        val gameNight = database.gameNightDao().getAll().maxByOrNull { it.startsAt }
            ?: return@runCatching null
        val host = database.playerDao().getById(gameNight.hostId)?.toDomain()
            ?: error("Für den Spieleabend wurde kein Gastgeber gefunden.")
        val reviews = database.reviewDao().getForGameNight(gameNight.id).map { it.toDomain() }
        ReviewSnapshot(gameNight.toDomain(), host, reviews, reviews.toAverages())
    }

    override fun finishGameNight(gameNightId: Long): Result<GameNight> = runCatching {
        val gameNight = database.gameNightDao().getById(gameNightId)
            ?: error("Der Spieleabend wurde nicht gefunden.")
        require(gameNight.status != GameNightStatus.FINISHED) {
            "Der Spieleabend ist bereits abgeschlossen."
        }
        val finished = gameNight.copy(status = GameNightStatus.FINISHED)
        database.gameNightDao().update(finished)
        finished.toDomain()
    }

    override fun submitReview(
        playerId: Long,
        gameNightId: Long,
        hostRating: Int,
        foodRating: Int,
        eveningRating: Int,
        comment: String,
    ): Result<Review> = runCatching {
        val gameNight = database.gameNightDao().getById(gameNightId)
            ?: error("Der Spieleabend wurde nicht gefunden.")
        require(gameNight.status == GameNightStatus.FINISHED) {
            "Nur abgeschlossene Spieleabende können bewertet werden."
        }
        require(database.playerDao().getById(playerId) != null) {
            "Der ausgewählte Spieler wurde nicht gefunden."
        }
        require(listOf(hostRating, foodRating, eveningRating).all { it in 1..5 }) {
            "Alle Bewertungen müssen zwischen 1 und 5 liegen."
        }
        val review = ReviewEntity(
            playerId = playerId,
            gameNightId = gameNightId,
            hostRating = hostRating,
            foodRating = foodRating,
            eveningRating = eveningRating,
            comment = comment.trim(),
        )
        review.copy(id = database.reviewDao().insert(review)).toDomain()
    }

    private fun findUpcomingGameNight(): GameNightEntity? = database.gameNightDao()
        .getAll()
        .asSequence()
        .filter { it.status == GameNightStatus.PLANNED }
        .filter { !it.startsAt.isBefore(now()) }
        .minByOrNull { it.startsAt }

    private fun seedIfDatabaseIsEmpty() {
        if (
            database.playerDao().count() != 0 ||
            database.gameNightDao().count() != 0 ||
            database.boardGameDao().count() != 0 ||
            database.voteDao().count() != 0
        ) {
            return
        }

        val players = listOf(
            PlayerEntity(
                id = 1,
                name = "Max Mustermann",
                address = "Musterstraße 12, 33100 Paderborn",
                hostOrder = 1,
            ),
            PlayerEntity(
                id = 2,
                name = "Lea Beispiel",
                address = "Spielweg 4, 33102 Paderborn",
                hostOrder = 2,
            ),
        )
        val gameNight = GameNightEntity(
            id = 1,
            startsAt = LocalDateTime.of(2026, 8, 28, 19, 0),
            hostId = 1,
            location = "Musterstraße 12, 33100 Paderborn",
            status = GameNightStatus.PLANNED,
        )
        database.playerDao().insert(players[0])
        database.playerDao().insert(players[1])
        database.gameNightDao().insert(gameNight)
        database.boardGameDao().insert(
            BoardGameEntity(
                id = 1,
                name = "Catan",
                description = "Handel und Aufbau für drei bis vier Personen.",
                suggestedByPlayerId = 1,
                gameNightId = 1,
            ),
        )
        database.boardGameDao().insert(
            BoardGameEntity(
                id = 2,
                name = "Heat",
                description = "Schnelles Autorennen mit taktischem Handmanagement.",
                suggestedByPlayerId = 2,
                gameNightId = 1,
            ),
        )
        database.voteDao().insert(VoteEntity(id = 1, playerId = 1, boardGameId = 1, gameNightId = 1))
        database.voteDao().insert(VoteEntity(id = 2, playerId = 2, boardGameId = 2, gameNightId = 1))
    }

    private fun String.required(fieldName: String): String {
        val value = trim()
        require(value.isNotEmpty()) { "$fieldName darf nicht leer sein." }
        return value
    }

    private fun PlayerEntity.toDomain() = Player(id, name, address, hostOrder)

    private fun GameNightEntity.toDomain() = GameNight(id, startsAt, hostId, location, status)

    private fun BoardGameEntity.toDomain() =
        BoardGame(id, name, description, suggestedByPlayerId, gameNightId)

    private fun VoteEntity.toDomain() = Vote(id, playerId, boardGameId, gameNightId)

    private fun LateNoticeEntity.toDomain() = LateNotice(
        id = id,
        playerId = playerId,
        gameNightId = gameNightId,
        minutes = minutes,
        createdAt = createdAt,
    )

    private fun ReviewEntity.toDomain() = Review(
        id, playerId, gameNightId, hostRating, foodRating, eveningRating, comment,
    )

    private fun List<Review>.toAverages(): ReviewAverages? = takeIf { it.isNotEmpty() }?.let {
        ReviewAverages(
            host = it.map(Review::hostRating).average(),
            food = it.map(Review::foodRating).average(),
            evening = it.map(Review::eveningRating).average(),
        )
    }

    companion object {
        fun create(context: Context): RoomGameNightRepository =
            RoomGameNightRepository(
                database = AppDatabase.getInstance(context),
                seedIfEmpty = true,
            )
    }
}
