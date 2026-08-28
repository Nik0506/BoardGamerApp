package com.example.boardgamerapp.data.repository

import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.BoardGame
import com.example.boardgamerapp.domain.model.LateNotice
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Vote
import com.example.boardgamerapp.domain.model.Review

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

interface BoardGamerRepository :
    GameNightRepository,
    LateNoticeRepository,
    PlayerRepository,
    GameSuggestionRepository,
    VotingRepository,
    ReviewRepository

enum class MoveDirection {
    UP,
    DOWN,
}
