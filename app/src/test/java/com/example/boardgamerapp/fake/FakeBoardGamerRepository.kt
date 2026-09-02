package com.example.boardgamerapp.fake

import com.example.boardgamerapp.data.repository.BoardGameSuggestion
import com.example.boardgamerapp.data.repository.BoardGameVoteResult
import com.example.boardgamerapp.data.repository.BoardGamerRepository
import com.example.boardgamerapp.data.repository.FoodVoteResult
import com.example.boardgamerapp.data.repository.FoodVotingSnapshot
import com.example.boardgamerapp.data.repository.GameNightSuggestions
import com.example.boardgamerapp.data.repository.MoveDirection
import com.example.boardgamerapp.data.repository.OrderingSnapshot
import com.example.boardgamerapp.data.repository.OrderWithPlayer
import com.example.boardgamerapp.data.repository.ReviewAverages
import com.example.boardgamerapp.data.repository.ReviewSnapshot
import com.example.boardgamerapp.data.repository.UpcomingGameNight
import com.example.boardgamerapp.data.repository.UpcomingGameNightSummary
import com.example.boardgamerapp.data.repository.VotingSnapshot
import com.example.boardgamerapp.domain.model.AttendanceStatusType
import com.example.boardgamerapp.domain.model.BoardGame
import com.example.boardgamerapp.domain.model.FoodCategory
import com.example.boardgamerapp.domain.model.FoodOrder
import com.example.boardgamerapp.domain.model.FoodVote
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightAttendance
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.LateNotice
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Restaurant
import com.example.boardgamerapp.domain.model.Review
import com.example.boardgamerapp.domain.model.Vote
import java.time.LocalDateTime

class FakeBoardGamerRepository : BoardGamerRepository {

    val players = mutableListOf<Player>()
    val gameNights = mutableListOf<GameNight>()
    val attendances = mutableListOf<GameNightAttendance>()
    val lateNotices = mutableListOf<LateNotice>()
    val suggestions = mutableListOf<BoardGame>()
    val votes = mutableListOf<Vote>()
    val reviews = mutableListOf<Review>()
    val foodCategories = mutableListOf<FoodCategory>()
    val foodVotes = mutableListOf<FoodVote>()
    var restaurant: Restaurant? = null
    val foodOrders = mutableListOf<FoodOrder>()

    var selectedGroupId: String? = null
    var selectedGameNightDocId: String? = null

    private var nextId = 1000L

    override fun selectGameNight(groupId: String, gameNightDocId: String) {
        selectedGroupId = groupId
        selectedGameNightDocId = gameNightDocId
    }

    override suspend fun getUpcomingGameNight(): Result<UpcomingGameNight?> {
        val gameNight = gameNights.firstOrNull { it.status != GameNightStatus.FINISHED && it.status != GameNightStatus.CANCELLED }
            ?: return Result.success(null)
        val host = players.firstOrNull { it.id == gameNight.hostId }
            ?: Player(gameNight.hostId, "Gastgeber", gameNight.location, 1)
        return Result.success(UpcomingGameNight(gameNight, host))
    }

    override suspend fun getUpcomingGameNights(): Result<List<UpcomingGameNightSummary>> {
        val summaries = gameNights
            .filter { it.status != GameNightStatus.FINISHED && it.status != GameNightStatus.CANCELLED }
            .map { gn ->
                val host = players.firstOrNull { it.id == gn.hostId }
                    ?: Player(gn.hostId, "Gastgeber", gn.location, 1)
                UpcomingGameNightSummary(
                    groupId = gn.groupId ?: "default-group",
                    groupName = "Test Gruppe",
                    gameNightDocId = gn.id.toString(),
                    gameNight = gn,
                    host = host,
                    isSelected = selectedGameNightDocId == gn.id.toString(),
                )
            }
        return Result.success(summaries)
    }

    override suspend fun updateGameNight(gameNightId: Long, startsAt: LocalDateTime, hostPlayerId: Long): Result<UpcomingGameNight> {
        val index = gameNights.indexOfFirst { it.id == gameNightId }
        if (index == -1) return Result.failure(IllegalArgumentException("Termin nicht gefunden."))
        val host = players.firstOrNull { it.id == hostPlayerId } ?: error("Host nicht gefunden.")
        val updated = gameNights[index].copy(startsAt = startsAt, hostId = hostPlayerId, location = host.address)
        gameNights[index] = updated
        return Result.success(UpcomingGameNight(updated, host))
    }

    override suspend fun createNextGameNight(
        startsAt: LocalDateTime?,
        preferredHostUid: String?,
        memberOrderOverride: List<String>?,
    ): Result<UpcomingGameNight> {
        val host = players.firstOrNull() ?: error("Keine Spieler vorhanden.")
        val newNight = GameNight(
            id = nextId++,
            startsAt = startsAt ?: LocalDateTime.now().plusDays(7),
            hostId = host.id,
            location = host.address,
            status = GameNightStatus.PLANNED,
        )
        gameNights.add(newNight)
        return Result.success(UpcomingGameNight(newNight, host))
    }

    override suspend fun cancelGameNight(gameNightId: Long, reason: String?): Result<Unit> {
        val index = gameNights.indexOfFirst { it.id == gameNightId }
        if (index == -1) return Result.failure(IllegalArgumentException("Termin nicht gefunden."))
        gameNights[index] = gameNights[index].copy(status = GameNightStatus.CANCELLED)
        return Result.success(Unit)
    }

    override suspend fun rescheduleGameNight(gameNightId: Long, startsAt: LocalDateTime): Result<UpcomingGameNight> {
        val index = gameNights.indexOfFirst { it.id == gameNightId }
        if (index == -1) return Result.failure(IllegalArgumentException("Termin nicht gefunden."))
        val updated = gameNights[index].copy(startsAt = startsAt)
        gameNights[index] = updated
        // Reset attendances
        attendances.clear()
        val host = players.firstOrNull { it.id == updated.hostId } ?: error("Host nicht gefunden.")
        return Result.success(UpcomingGameNight(updated, host))
    }

    override suspend fun reassignHost(gameNightId: Long, newHostPlayerId: Long): Result<UpcomingGameNight> {
        val index = gameNights.indexOfFirst { it.id == gameNightId }
        if (index == -1) return Result.failure(IllegalArgumentException("Termin nicht gefunden."))
        val newHost = players.firstOrNull { it.id == newHostPlayerId } ?: error("Neuer Host nicht gefunden.")
        val oldHostId = gameNights[index].hostId
        val updated = gameNights[index].copy(hostId = newHostPlayerId, location = newHost.address)
        gameNights[index] = updated
        // Mark old host as declined
        attendances.removeAll { it.playerId == oldHostId }
        attendances.add(
            GameNightAttendance(
                id = nextId++,
                playerId = oldHostId,
                gameNightId = gameNightId,
                status = AttendanceStatusType.DECLINED,
                reason = "Gastgeberschaft an ${newHost.name} übergeben",
            )
        )
        return Result.success(UpcomingGameNight(updated, newHost))
    }

    override suspend fun getAttendances(): Result<List<GameNightAttendance>> =
        Result.success(attendances.toList())

    override suspend fun setAttendance(
        playerId: Long,
        status: AttendanceStatusType,
        minutesLate: Int?,
        reason: String?,
    ): Result<GameNightAttendance> {
        val activeNight = gameNights.firstOrNull { it.status == GameNightStatus.PLANNED }
            ?: return Result.failure(IllegalStateException("Kein geplanter Spieleabend."))
        attendances.removeAll { it.playerId == playerId && it.gameNightId == activeNight.id }
        val attendance = GameNightAttendance(
            id = nextId++,
            playerId = playerId,
            gameNightId = activeNight.id,
            status = status,
            minutesLate = minutesLate,
            reason = reason,
            updatedAt = LocalDateTime.now(),
        )
        attendances.add(attendance)
        return Result.success(attendance)
    }

    override suspend fun getLateNotices(): Result<List<LateNotice>> =
        Result.success(lateNotices.toList())

    override suspend fun addLateNotice(playerId: Long, minutes: Int): Result<LateNotice> {
        val activeNight = gameNights.firstOrNull { it.status == GameNightStatus.PLANNED }
            ?: return Result.failure(IllegalStateException("Kein geplanter Spieleabend."))
        val notice = LateNotice(
            id = nextId++,
            playerId = playerId,
            gameNightId = activeNight.id,
            minutes = minutes,
            createdAt = LocalDateTime.now(),
        )
        lateNotices.add(notice)
        return Result.success(notice)
    }

    override suspend fun getPlayers(): Result<List<Player>> =
        Result.success(players.sortedBy { it.hostOrder })

    override suspend fun addPlayer(name: String, address: String): Result<Player> {
        val player = Player(id = nextId++, name = name, address = address, hostOrder = players.size + 1)
        players.add(player)
        return Result.success(player)
    }

    override suspend fun updatePlayer(id: Long, name: String, address: String): Result<Player> {
        val index = players.indexOfFirst { it.id == id }
        if (index == -1) return Result.failure(IllegalArgumentException("Spieler nicht gefunden."))
        val updated = players[index].copy(name = name, address = address)
        players[index] = updated
        return Result.success(updated)
    }

    override suspend fun movePlayer(id: Long, direction: MoveDirection): Result<List<Player>> {
        val index = players.indexOfFirst { it.id == id }
        if (index == -1) return Result.failure(IllegalArgumentException("Spieler nicht gefunden."))
        val targetIndex = when (direction) {
            MoveDirection.UP -> (index - 1).coerceAtLeast(0)
            MoveDirection.DOWN -> (index + 1).coerceAtMost(players.size - 1)
        }
        if (index != targetIndex) {
            val item = players.removeAt(index)
            players.add(targetIndex, item)
            players.forEachIndexed { i, p ->
                players[i] = p.copy(hostOrder = i + 1)
            }
        }
        return Result.success(players.toList())
    }

    override suspend fun getGameSuggestions(): Result<GameNightSuggestions?> {
        val activeNight = gameNights.firstOrNull { it.status == GameNightStatus.PLANNED }
            ?: return Result.success(null)
        val list = suggestions.filter { it.gameNightId == activeNight.id }.map { game ->
            val player = players.firstOrNull { it.id == game.suggestedByPlayerId }
                ?: Player(game.suggestedByPlayerId, "User", "", 1)
            BoardGameSuggestion(game, player)
        }
        return Result.success(GameNightSuggestions(activeNight, list))
    }

    override suspend fun addGameSuggestion(name: String, description: String, suggestedByPlayerId: Long): Result<BoardGameSuggestion> {
        val activeNight = gameNights.firstOrNull { it.status == GameNightStatus.PLANNED }
            ?: return Result.failure(IllegalStateException("Kein aktiver Spieleabend."))
        val game = BoardGame(
            id = nextId++,
            name = name,
            description = description,
            suggestedByPlayerId = suggestedByPlayerId,
            gameNightId = activeNight.id,
        )
        suggestions.add(game)
        val player = players.firstOrNull { it.id == suggestedByPlayerId } ?: Player(suggestedByPlayerId, "User", "", 1)
        return Result.success(BoardGameSuggestion(game, player))
    }

    override suspend fun deleteGameSuggestion(boardGameId: Long, requestingPlayerId: Long): Result<Unit> {
        val game = suggestions.firstOrNull { it.id == boardGameId }
            ?: return Result.failure(IllegalArgumentException("Spiel nicht gefunden."))
        if (game.suggestedByPlayerId != requestingPlayerId) {
            return Result.failure(IllegalStateException("Nur der Ersteller kann den Vorschlag löschen."))
        }
        suggestions.remove(game)
        votes.removeAll { it.boardGameId == boardGameId }
        return Result.success(Unit)
    }

    override suspend fun getVotingSnapshot(): Result<VotingSnapshot?> {
        val activeNight = gameNights.firstOrNull { it.status == GameNightStatus.PLANNED }
            ?: return Result.success(null)
        val gameList = suggestions.filter { it.gameNightId == activeNight.id }
        val results = gameList.map { game ->
            val player = players.firstOrNull { it.id == game.suggestedByPlayerId } ?: Player(game.suggestedByPlayerId, "User", "", 1)
            val voterIds = votes.filter { it.boardGameId == game.id }.map { it.playerId }.toSet()
            BoardGameVoteResult(BoardGameSuggestion(game, player), voterIds)
        }
        return Result.success(VotingSnapshot(activeNight, results, players.size))
    }

    override suspend fun castVote(playerId: Long, boardGameId: Long): Result<Vote> {
        val activeNight = gameNights.firstOrNull { it.status == GameNightStatus.PLANNED }
            ?: return Result.failure(IllegalStateException("Kein aktiver Spieleabend."))
        votes.removeAll { it.playerId == playerId && it.boardGameId == boardGameId }
        val vote = Vote(id = nextId++, playerId = playerId, boardGameId = boardGameId, gameNightId = activeNight.id)
        votes.add(vote)
        return Result.success(vote)
    }

    override suspend fun getReviewSnapshot(): Result<ReviewSnapshot?> {
        val night = gameNights.firstOrNull() ?: return Result.success(null)
        val host = players.firstOrNull { it.id == night.hostId } ?: Player(night.hostId, "Gastgeber", "", 1)
        val nightReviews = reviews.filter { it.gameNightId == night.id }
        val averages = if (nightReviews.isEmpty()) null else {
            ReviewAverages(
                host = nightReviews.map { it.hostRating }.average(),
                food = nightReviews.map { it.foodRating }.average(),
                evening = nightReviews.map { it.eveningRating }.average(),
            )
        }
        return Result.success(ReviewSnapshot(night, host, nightReviews, averages))
    }

    override suspend fun finishGameNight(gameNightId: Long): Result<GameNight> {
        val index = gameNights.indexOfFirst { it.id == gameNightId }
        if (index == -1) return Result.failure(IllegalArgumentException("Termin nicht gefunden."))
        val finished = gameNights[index].copy(status = GameNightStatus.FINISHED)
        gameNights[index] = finished
        return Result.success(finished)
    }

    override suspend fun submitReview(
        playerId: Long,
        gameNightId: Long,
        hostRating: Int,
        foodRating: Int,
        eveningRating: Int,
        comment: String,
    ): Result<Review> {
        reviews.removeAll { it.playerId == playerId && it.gameNightId == gameNightId }
        val review = Review(
            id = nextId++,
            playerId = playerId,
            gameNightId = gameNightId,
            hostRating = hostRating,
            foodRating = foodRating,
            eveningRating = eveningRating,
            comment = comment,
        )
        reviews.add(review)
        return Result.success(review)
    }

    override suspend fun getFoodVotingSnapshot(): Result<FoodVotingSnapshot?> {
        val activeNight = gameNights.firstOrNull { it.status == GameNightStatus.PLANNED }
            ?: return Result.success(null)
        val results = foodCategories.filter { it.gameNightId == activeNight.id }.map { cat ->
            val voterIds = foodVotes.filter { it.foodCategoryId == cat.id }.map { it.playerId }.toSet()
            FoodVoteResult(cat, voterIds)
        }
        return Result.success(FoodVotingSnapshot(activeNight, results, players.toList()))
    }

    override suspend fun addFoodCategory(name: String): Result<FoodCategory> {
        val activeNight = gameNights.firstOrNull { it.status == GameNightStatus.PLANNED }
            ?: return Result.failure(IllegalStateException("Kein aktiver Spieleabend."))
        val cat = FoodCategory(id = nextId++, name = name, gameNightId = activeNight.id)
        foodCategories.add(cat)
        return Result.success(cat)
    }

    override suspend fun deleteFoodCategory(categoryId: Long): Result<Unit> {
        foodCategories.removeAll { it.id == categoryId }
        foodVotes.removeAll { it.foodCategoryId == categoryId }
        return Result.success(Unit)
    }

    override suspend fun castFoodVote(playerId: Long, categoryId: Long): Result<FoodVote> {
        val activeNight = gameNights.firstOrNull { it.status == GameNightStatus.PLANNED }
            ?: return Result.failure(IllegalStateException("Kein aktiver Spieleabend."))
        foodVotes.removeAll { it.playerId == playerId && it.foodCategoryId == categoryId }
        val vote = FoodVote(id = nextId++, playerId = playerId, foodCategoryId = categoryId, gameNightId = activeNight.id)
        foodVotes.add(vote)
        return Result.success(vote)
    }

    override suspend fun getOrderingSnapshot(): Result<OrderingSnapshot?> {
        val activeNight = gameNights.firstOrNull { it.status == GameNightStatus.PLANNED }
            ?: return Result.success(null)
        val host = players.firstOrNull { it.id == activeNight.hostId } ?: Player(activeNight.hostId, "Gastgeber", "", 1)
        val ordersWithPlayer = foodOrders.filter { it.gameNightId == activeNight.id }.map { order ->
            val player = players.firstOrNull { it.id == order.playerId } ?: Player(order.playerId, "User", "", 1)
            OrderWithPlayer(order, player)
        }
        return Result.success(OrderingSnapshot(activeNight, host, restaurant, ordersWithPlayer))
    }

    override suspend fun saveRestaurant(requestingPlayerId: Long, name: String, menuUrl: String): Result<Restaurant> {
        val activeNight = gameNights.firstOrNull { it.status == GameNightStatus.PLANNED }
            ?: return Result.failure(IllegalStateException("Kein aktiver Spieleabend."))
        val rest = Restaurant(id = nextId++, gameNightId = activeNight.id, name = name, menuUrl = menuUrl)
        restaurant = rest
        return Result.success(rest)
    }

    override suspend fun saveFoodOrder(playerId: Long, dish: String, note: String, priceCents: Long): Result<FoodOrder> {
        val activeNight = gameNights.firstOrNull { it.status == GameNightStatus.PLANNED }
            ?: return Result.failure(IllegalStateException("Kein aktiver Spieleabend."))
        foodOrders.removeAll { it.playerId == playerId && it.gameNightId == activeNight.id }
        val order = FoodOrder(id = nextId++, gameNightId = activeNight.id, playerId = playerId, dish = dish, note = note, priceCents = priceCents)
        foodOrders.add(order)
        return Result.success(order)
    }

    override suspend fun deleteFoodOrder(orderId: Long, requestingPlayerId: Long): Result<Unit> {
        val order = foodOrders.firstOrNull { it.id == orderId }
            ?: return Result.failure(IllegalArgumentException("Bestellung nicht gefunden."))
        if (order.playerId != requestingPlayerId) {
            return Result.failure(IllegalStateException("Nur die eigene Bestellung kann gelöscht werden."))
        }
        foodOrders.remove(order)
        return Result.success(Unit)
    }
}
