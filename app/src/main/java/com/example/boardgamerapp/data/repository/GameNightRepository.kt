package com.example.boardgamerapp.data.repository

import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.BoardGame
import com.example.boardgamerapp.domain.model.LateNotice
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Vote
import com.example.boardgamerapp.domain.model.Review
import com.example.boardgamerapp.domain.model.FoodCategory
import com.example.boardgamerapp.domain.model.FoodVote
import com.example.boardgamerapp.domain.model.FoodOrder
import com.example.boardgamerapp.domain.model.Restaurant

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

data class BoardGameVoteResult(
    val suggestion: BoardGameSuggestion,
    val voterIds: Set<Long>,
) {
    val voteCount: Int = voterIds.size
}

data class VotingSnapshot(
    val gameNight: GameNight,
    val results: List<BoardGameVoteResult>,
    val playerCount: Int,
) {
    val totalVotes: Int = results.sumOf { it.voteCount }
}

data class ReviewAverages(
    val host: Double,
    val food: Double,
    val evening: Double,
)

data class ReviewSnapshot(
    val gameNight: GameNight,
    val host: Player,
    val reviews: List<Review>,
    val averages: ReviewAverages?,
)

data class FoodVoteResult(
    val category: FoodCategory,
    val voterIds: Set<Long>,
) {
    val voteCount: Int = voterIds.size
}

data class FoodVotingSnapshot(
    val gameNight: GameNight,
    val results: List<FoodVoteResult>,
    val players: List<Player>,
) {
    val totalVotes: Int = results.sumOf { it.voteCount }
    val missingPlayers: List<Player> = players.filter { player ->
        results.none { player.id in it.voterIds }
    }
}

data class OrderWithPlayer(val order: FoodOrder, val player: Player)

data class OrderingSnapshot(
    val gameNight: GameNight,
    val host: Player,
    val restaurant: Restaurant?,
    val orders: List<OrderWithPlayer>,
) {
    val totalCents: Long = orders.sumOf { it.order.priceCents }
}

interface GameNightRepository {
    fun getUpcomingGameNight(): Result<UpcomingGameNight?>
}

interface LateNoticeRepository {
    fun getLateNotices(): Result<List<LateNotice>>

    fun addLateNotice(playerId: Long, minutes: Int): Result<LateNotice>
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

interface VotingRepository {
    fun getVotingSnapshot(): Result<VotingSnapshot?>

    fun castVote(playerId: Long, boardGameId: Long): Result<Vote>
}

interface ReviewRepository {
    fun getReviewSnapshot(): Result<ReviewSnapshot?>

    fun finishGameNight(gameNightId: Long): Result<GameNight>

    fun submitReview(
        playerId: Long,
        gameNightId: Long,
        hostRating: Int,
        foodRating: Int,
        eveningRating: Int,
        comment: String,
    ): Result<Review>
}

interface FoodVotingRepository {
    fun getFoodVotingSnapshot(): Result<FoodVotingSnapshot?>

    fun addFoodCategory(name: String): Result<FoodCategory>

    fun deleteFoodCategory(categoryId: Long): Result<Unit>

    fun castFoodVote(playerId: Long, categoryId: Long): Result<FoodVote>
}

interface OrderingRepository {
    fun getOrderingSnapshot(): Result<OrderingSnapshot?>
    fun saveRestaurant(requestingPlayerId: Long, name: String, menuUrl: String): Result<Restaurant>
    fun saveFoodOrder(playerId: Long, dish: String, note: String, priceCents: Long): Result<FoodOrder>
    fun deleteFoodOrder(orderId: Long, requestingPlayerId: Long): Result<Unit>
}

interface BoardGamerRepository :
    GameNightRepository,
    LateNoticeRepository,
    PlayerRepository,
    GameSuggestionRepository,
    VotingRepository,
    ReviewRepository,
    FoodVotingRepository,
    OrderingRepository

enum class MoveDirection {
    UP,
    DOWN,
}
