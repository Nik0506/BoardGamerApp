package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.MoveDirection
import com.example.boardgamerapp.data.repository.PlayerRepository
import com.example.boardgamerapp.data.repository.ReviewAverages
import com.example.boardgamerapp.data.repository.ReviewRepository
import com.example.boardgamerapp.data.repository.ReviewSnapshot
import com.example.boardgamerapp.data.repository.UpcomingGameNight
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Review
import com.example.boardgamerapp.ui.review.ReviewUiState
import com.example.boardgamerapp.ui.review.ReviewViewModel
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewLogicTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createPlayerRepo(players: List<Player>): PlayerRepository = object : PlayerRepository {
        override suspend fun getPlayers(): Result<List<Player>> = Result.success(players)
        override suspend fun addPlayer(name: String, address: String): Result<Player> = error("")
        override suspend fun updatePlayer(id: Long, name: String, address: String): Result<Player> = error("")
        override suspend fun movePlayer(id: Long, direction: MoveDirection): Result<List<Player>> = error("")
        override suspend fun createNextGameNight(startsAt: LocalDateTime?, preferredHostUid: String?, memberOrderOverride: List<String>?): Result<UpcomingGameNight> = error("")
    }

    private fun createReviewRepo(
        snapshot: ReviewSnapshot?,
        onSubmitReview: ((Int, Int, Int, String) -> Unit)? = null,
        onFinishGameNight: (() -> Unit)? = null,
    ): ReviewRepository = object : ReviewRepository {
        override suspend fun getReviewSnapshot(): Result<ReviewSnapshot?> = Result.success(snapshot)
        override suspend fun finishGameNight(gameNightId: Long): Result<GameNight> {
            onFinishGameNight?.invoke()
            return Result.success(GameNight(gameNightId, LocalDateTime.now(), 1L, "", GameNightStatus.FINISHED))
        }
        override suspend fun submitReview(
            playerId: Long,
            gameNightId: Long,
            hostRating: Int,
            foodRating: Int,
            eveningRating: Int,
            comment: String,
        ): Result<Review> {
            onSubmitReview?.invoke(hostRating, foodRating, eveningRating, comment)
            return Result.success(Review(1L, playerId, gameNightId, hostRating, foodRating, eveningRating, comment))
        }
    }

    @Test
    fun `review snapshot maps averages and formats correctly`() = runTest(dispatcher) {
        val host = Player(1L, "Max", "Addr", 1)
        val p2 = Player(2L, "Erika", "Addr2", 2)
        val gameNight = GameNight(42L, LocalDateTime.of(2026, 9, 10, 19, 0), 1L, "Addr", GameNightStatus.FINISHED)
        val reviews = listOf(
            Review(1L, 2L, 42L, 5, 4, 5, "Super"),
        )
        val averages = ReviewAverages(host = 4.66, food = 3.5, evening = 4.0)

        val snapshot = ReviewSnapshot(gameNight, host, reviews, averages)
        val vm = ReviewViewModel(
            createReviewRepo(snapshot),
            createPlayerRepo(listOf(host, p2)),
            currentPlayerId = 2L,
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        val content = vm.uiState as ReviewUiState.Content
        assertEquals(42L, content.gameNightId)
        assertTrue(content.isFinished)
        assertEquals(1, content.reviewCount)
        assertEquals("4,7", content.averages?.host)
        assertEquals("3,5", content.averages?.food)
        assertEquals("4,0", content.averages?.evening)
        assertTrue(content.currentPlayerHasReviewed)
    }

    @Test
    fun `saveReview validates that ratings must be in 1 to 5 range`() = runTest(dispatcher) {
        val host = Player(1L, "Max", "Addr", 1)
        val p2 = Player(2L, "Erika", "Addr2", 2)
        val gameNight = GameNight(42L, LocalDateTime.of(2026, 9, 10, 19, 0), 1L, "Addr", GameNightStatus.FINISHED)

        // No reviews yet
        val snapshot = ReviewSnapshot(gameNight, host, emptyList(), null)
        val vm = ReviewViewModel(
            createReviewRepo(snapshot),
            createPlayerRepo(listOf(host, p2)),
            currentPlayerId = 2L,
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        val content = vm.uiState as ReviewUiState.Content
        assertFalse(content.currentPlayerHasReviewed)

        vm.beginReview()
        assertNotNull((vm.uiState as ReviewUiState.Content).editor)

        // Invalid: 0 points (default)
        vm.saveReview()
        val errorContent = vm.uiState as ReviewUiState.Content
        assertEquals("Vergib in allen drei Kategorien 1 bis 5 Punkte.", errorContent.editor?.errorMessage)

        // Valid ratings: 1..5
        vm.setHostRating(5)
        vm.setFoodRating(4)
        vm.setEveningRating(5)
        vm.updateComment("Schöner Abend!")
        vm.saveReview()
        advanceUntilIdle()

        val successContent = vm.uiState as ReviewUiState.Content
        assertEquals("Bewertung gespeichert.", successContent.message)
    }

    @Test
    fun `finishGameNight calls repository and sets completion message`() = runTest(dispatcher) {
        val host = Player(1L, "Max", "Addr", 1)
        val gameNight = GameNight(42L, LocalDateTime.of(2026, 9, 10, 19, 0), 1L, "Addr", GameNightStatus.PLANNED)
        var finishCalled = false

        val snapshot = ReviewSnapshot(gameNight, host, emptyList(), null)
        val vm = ReviewViewModel(
            createReviewRepo(snapshot, onFinishGameNight = { finishCalled = true }),
            createPlayerRepo(listOf(host)),
            currentPlayerId = 1L,
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        vm.finishGameNight()
        advanceUntilIdle()

        assertTrue(finishCalled)
        assertEquals("Spieleabend abgeschlossen.", (vm.uiState as ReviewUiState.Content).message)
    }
}
