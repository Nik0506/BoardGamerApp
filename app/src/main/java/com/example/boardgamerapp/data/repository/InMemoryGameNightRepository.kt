package com.example.boardgamerapp.data.repository

import com.example.boardgamerapp.domain.HostRotation
import com.example.boardgamerapp.domain.model.BoardGame
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.FoodCategory
import com.example.boardgamerapp.domain.model.FoodVote
import com.example.boardgamerapp.domain.model.FoodOrder
import com.example.boardgamerapp.domain.model.Restaurant
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.LateNotice
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Review
import com.example.boardgamerapp.domain.model.Vote
import java.time.LocalDateTime

class InMemoryGameNightRepository(
    players: List<Player> = emptyList(),
    gameNights: List<GameNight> = emptyList(),
    boardGames: List<BoardGame> = emptyList(),
    votes: List<Vote> = emptyList(),
    lateNotices: List<LateNotice> = emptyList(),
    reviews: List<Review> = emptyList(),
    foodCategories: List<FoodCategory> = emptyList(),
    foodVotes: List<FoodVote> = emptyList(),
    restaurants: List<Restaurant> = emptyList(),
    foodOrders: List<FoodOrder> = emptyList(),
    private val now: () -> LocalDateTime = LocalDateTime::now,
) : BoardGamerRepository {

    private val players = players.toMutableList()
    private val gameNights = gameNights.toMutableList()
    private val boardGames = boardGames.toMutableList()
    private val votes = votes.toMutableList()
    private val lateNotices = lateNotices.toMutableList()
    private val reviews = reviews.toMutableList()
    private val foodCategories = foodCategories.toMutableList()
    private val foodVotes = foodVotes.toMutableList()
    private val restaurants = restaurants.toMutableList()
    private val foodOrders = foodOrders.toMutableList()

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

    override fun getLateNotices(): Result<List<LateNotice>> = runCatching {
        val gameNight = findUpcomingGameNight() ?: return@runCatching emptyList()
        lateNotices
            .filter { it.gameNightId == gameNight.id }
            .sortedByDescending { it.createdAt }
    }

    override fun addLateNotice(playerId: Long, minutes: Int): Result<LateNotice> = runCatching {
        val gameNight = findUpcomingGameNight()
            ?: error("Es gibt keinen kommenden Spieleabend.")
        require(minutes > 0) { "Die Verspätung muss größer als 0 Minuten sein." }
        require(players.any { it.id == playerId }) {
            "Der ausgewählte Spieler wurde nicht gefunden."
        }
        val lateNotice = LateNotice(
            id = (lateNotices.maxOfOrNull { it.id } ?: 0L) + 1L,
            playerId = playerId,
            gameNightId = gameNight.id,
            minutes = minutes,
            createdAt = now(),
        )
        lateNotices += lateNotice
        lateNotice
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

    override fun getReviewSnapshot(): Result<ReviewSnapshot?> = runCatching {
        val gameNight = gameNights.maxByOrNull { it.startsAt } ?: return@runCatching null
        val host = players.firstOrNull { it.id == gameNight.hostId }
            ?: error("Für den Spieleabend wurde kein Gastgeber gefunden.")
        val gameNightReviews = reviews.filter { it.gameNightId == gameNight.id }
        ReviewSnapshot(gameNight, host, gameNightReviews, gameNightReviews.toAverages())
    }

    override fun finishGameNight(gameNightId: Long): Result<GameNight> = runCatching {
        val index = gameNights.indexOfFirst { it.id == gameNightId }
        require(index >= 0) { "Der Spieleabend wurde nicht gefunden." }
        require(gameNights[index].status != GameNightStatus.FINISHED) {
            "Der Spieleabend ist bereits abgeschlossen."
        }
        gameNights[index] = gameNights[index].copy(status = GameNightStatus.FINISHED)
        gameNights[index]
    }

    override fun submitReview(
        playerId: Long,
        gameNightId: Long,
        hostRating: Int,
        foodRating: Int,
        eveningRating: Int,
        comment: String,
    ): Result<Review> = runCatching {
        val gameNight = gameNights.firstOrNull { it.id == gameNightId }
            ?: error("Der Spieleabend wurde nicht gefunden.")
        require(gameNight.status == GameNightStatus.FINISHED) {
            "Nur abgeschlossene Spieleabende können bewertet werden."
        }
        require(players.any { it.id == playerId }) { "Der ausgewählte Spieler wurde nicht gefunden." }
        require(reviews.none { it.playerId == playerId && it.gameNightId == gameNightId }) {
            "Dieser Spieler hat den Spieleabend bereits bewertet."
        }
        require(listOf(hostRating, foodRating, eveningRating).all { it in 1..5 }) {
            "Alle Bewertungen müssen zwischen 1 und 5 liegen."
        }
        Review(
            id = (reviews.maxOfOrNull { it.id } ?: 0L) + 1,
            playerId = playerId,
            gameNightId = gameNightId,
            hostRating = hostRating,
            foodRating = foodRating,
            eveningRating = eveningRating,
            comment = comment.trim(),
        ).also(reviews::add)
    }

    override fun getFoodVotingSnapshot(): Result<FoodVotingSnapshot?> = runCatching {
        val gameNight = findUpcomingGameNight() ?: return@runCatching null
        if (foodCategories.none { it.gameNightId == gameNight.id }) {
            defaultFoodCategories.forEach { name ->
                foodCategories += FoodCategory(
                    id = (foodCategories.maxOfOrNull { it.id } ?: 0L) + 1,
                    name = name,
                    gameNightId = gameNight.id,
                )
            }
        }
        val categories = foodCategories.filter { it.gameNightId == gameNight.id }
        val votes = foodVotes.filter { it.gameNightId == gameNight.id }
        FoodVotingSnapshot(
            gameNight = gameNight,
            results = categories.map { category ->
                FoodVoteResult(
                    category,
                    votes.filter { it.foodCategoryId == category.id }.mapTo(mutableSetOf()) { it.playerId },
                )
            }.sortedWith(compareByDescending<FoodVoteResult> { it.voteCount }.thenBy { it.category.name.lowercase() }),
            players = players.sortedBy { it.hostOrder },
        )
    }

    override fun addFoodCategory(name: String): Result<FoodCategory> = runCatching {
        val gameNight = findUpcomingGameNight() ?: error("Es gibt keinen kommenden Spieleabend.")
        val validatedName = name.required("Kategorie")
        require(foodCategories.none { it.gameNightId == gameNight.id && it.name.equals(validatedName, true) }) {
            "Diese Essenskategorie gibt es bereits."
        }
        FoodCategory(
            id = (foodCategories.maxOfOrNull { it.id } ?: 0L) + 1,
            name = validatedName,
            gameNightId = gameNight.id,
        ).also(foodCategories::add)
    }

    override fun deleteFoodCategory(categoryId: Long): Result<Unit> = runCatching {
        val gameNight = findUpcomingGameNight() ?: error("Es gibt keinen kommenden Spieleabend.")
        val category = foodCategories.firstOrNull { it.id == categoryId && it.gameNightId == gameNight.id }
            ?: error("Die Essenskategorie wurde nicht gefunden.")
        foodCategories.remove(category)
        foodVotes.removeAll { it.foodCategoryId == category.id }
    }

    override fun castFoodVote(playerId: Long, categoryId: Long): Result<FoodVote> = runCatching {
        val gameNight = findUpcomingGameNight() ?: error("Es gibt keinen kommenden Spieleabend.")
        require(players.any { it.id == playerId }) { "Der ausgewählte Spieler wurde nicht gefunden." }
        require(foodCategories.any { it.id == categoryId && it.gameNightId == gameNight.id }) {
            "Die ausgewählte Essenskategorie gehört nicht zum kommenden Spieleabend."
        }
        foodVotes.removeAll { it.playerId == playerId && it.gameNightId == gameNight.id }
        FoodVote(
            id = (foodVotes.maxOfOrNull { it.id } ?: 0L) + 1,
            playerId = playerId,
            foodCategoryId = categoryId,
            gameNightId = gameNight.id,
        ).also(foodVotes::add)
    }

    override fun getOrderingSnapshot(): Result<OrderingSnapshot?> = runCatching {
        val night = findUpcomingGameNight() ?: return@runCatching null
        val host = players.firstOrNull { it.id == night.hostId } ?: error("Der Gastgeber wurde nicht gefunden.")
        OrderingSnapshot(
            night,
            host,
            restaurants.firstOrNull { it.gameNightId == night.id },
            foodOrders.filter { it.gameNightId == night.id }.map { order ->
                OrderWithPlayer(order, players.first { it.id == order.playerId })
            }.sortedBy { it.player.name.lowercase() },
        )
    }

    override fun saveRestaurant(requestingPlayerId: Long, name: String, menuUrl: String): Result<Restaurant> = runCatching {
        val night = findUpcomingGameNight() ?: error("Es gibt keinen kommenden Spieleabend.")
        require(requestingPlayerId == night.hostId) { "Nur der Gastgeber kann das Restaurant bearbeiten." }
        val cleanUrl = menuUrl.trim()
        require(cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) { "Der Menü-Link muss mit http:// oder https:// beginnen." }
        val existing = restaurants.firstOrNull { it.gameNightId == night.id }
        val value = Restaurant(existing?.id ?: ((restaurants.maxOfOrNull { it.id } ?: 0) + 1), night.id, name.required("Restaurantname"), cleanUrl)
        restaurants.removeAll { it.gameNightId == night.id }
        restaurants += value
        value
    }

    override fun saveFoodOrder(playerId: Long, dish: String, note: String, priceCents: Long): Result<FoodOrder> = runCatching {
        val night = findUpcomingGameNight() ?: error("Es gibt keinen kommenden Spieleabend.")
        require(players.any { it.id == playerId }) { "Der ausgewählte Spieler wurde nicht gefunden." }
        require(priceCents >= 0) { "Der Preis darf nicht negativ sein." }
        val existing = foodOrders.firstOrNull { it.playerId == playerId && it.gameNightId == night.id }
        val value = FoodOrder(existing?.id ?: ((foodOrders.maxOfOrNull { it.id } ?: 0) + 1), night.id, playerId, dish.required("Gericht"), note.trim(), priceCents)
        foodOrders.removeAll { it.playerId == playerId && it.gameNightId == night.id }
        foodOrders += value
        value
    }

    override fun deleteFoodOrder(orderId: Long, requestingPlayerId: Long): Result<Unit> = runCatching {
        val order = foodOrders.firstOrNull { it.id == orderId } ?: error("Die Bestellung wurde nicht gefunden.")
        require(order.playerId == requestingPlayerId) { "Nur die eigene Bestellung kann gelöscht werden." }
        foodOrders.remove(order)
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

    private fun List<Review>.toAverages(): ReviewAverages? = takeIf { it.isNotEmpty() }?.let {
        ReviewAverages(
            host = it.map(Review::hostRating).average(),
            food = it.map(Review::foodRating).average(),
            evening = it.map(Review::eveningRating).average(),
        )
    }

    companion object {
        private val defaultFoodCategories = listOf("Asiatisch", "Burger", "Pizza")
    }
}
