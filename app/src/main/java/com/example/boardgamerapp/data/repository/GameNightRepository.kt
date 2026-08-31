package com.example.boardgamerapp.data.repository

import com.example.boardgamerapp.domain.model.AttendanceStatusType
import com.example.boardgamerapp.domain.model.GameNightAttendance
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
    suspend fun getUpcomingGameNight(): Result<UpcomingGameNight?>

    suspend fun updateGameNight(
        gameNightId: Long,
        startsAt: java.time.LocalDateTime,
        hostPlayerId: Long,
    ): Result<UpcomingGameNight> = Result.failure(UnsupportedOperationException("Nicht implementiert"))
}

interface AttendanceRepository {
    suspend fun getAttendances(): Result<List<GameNightAttendance>>

    suspend fun setAttendance(
        playerId: Long,
        status: AttendanceStatusType,
        minutesLate: Int? = null,
        reason: String? = null,
    ): Result<GameNightAttendance>
}

interface LateNoticeRepository {
    suspend fun getLateNotices(): Result<List<LateNotice>>

    suspend fun addLateNotice(playerId: Long, minutes: Int): Result<LateNotice>
}

interface PlayerRepository {
    suspend fun getPlayers(): Result<List<Player>>

    suspend fun addPlayer(name: String, address: String): Result<Player>

    suspend fun updatePlayer(id: Long, name: String, address: String): Result<Player>

    suspend fun movePlayer(id: Long, direction: MoveDirection): Result<List<Player>>

    suspend fun createNextGameNight(
        startsAt: java.time.LocalDateTime? = null,
        preferredHostUid: String? = null,
        memberOrderOverride: List<String>? = null,
    ): Result<UpcomingGameNight>
}

interface GameSuggestionRepository {
    suspend fun getGameSuggestions(): Result<GameNightSuggestions?>

    suspend fun addGameSuggestion(
        name: String,
        description: String,
        suggestedByPlayerId: Long,
    ): Result<BoardGameSuggestion>

    suspend fun deleteGameSuggestion(boardGameId: Long, requestingPlayerId: Long): Result<Unit>
}

interface VotingRepository {
    suspend fun getVotingSnapshot(): Result<VotingSnapshot?>

    suspend fun castVote(playerId: Long, boardGameId: Long): Result<Vote>
}

interface ReviewRepository {
    suspend fun getReviewSnapshot(): Result<ReviewSnapshot?>

    suspend fun finishGameNight(gameNightId: Long): Result<GameNight>

    suspend fun submitReview(
        playerId: Long,
        gameNightId: Long,
        hostRating: Int,
        foodRating: Int,
        eveningRating: Int,
        comment: String,
    ): Result<Review>
}

interface FoodVotingRepository {
    suspend fun getFoodVotingSnapshot(): Result<FoodVotingSnapshot?>

    suspend fun addFoodCategory(name: String): Result<FoodCategory>

    suspend fun deleteFoodCategory(categoryId: Long): Result<Unit>

    suspend fun castFoodVote(playerId: Long, categoryId: Long): Result<FoodVote>
}

interface OrderingRepository {
    suspend fun getOrderingSnapshot(): Result<OrderingSnapshot?>
    suspend fun saveRestaurant(requestingPlayerId: Long, name: String, menuUrl: String): Result<Restaurant>
    suspend fun saveFoodOrder(playerId: Long, dish: String, note: String, priceCents: Long): Result<FoodOrder>
    suspend fun deleteFoodOrder(orderId: Long, requestingPlayerId: Long): Result<Unit>
}

interface BoardGamerRepository :
    GameNightRepository,
    AttendanceRepository,
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
