package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.BoardGamerRepository
import com.example.boardgamerapp.data.repository.FoodVoteResult
import com.example.boardgamerapp.data.repository.FoodVotingSnapshot
import com.example.boardgamerapp.data.repository.OrderingSnapshot
import com.example.boardgamerapp.data.repository.OrderWithPlayer
import com.example.boardgamerapp.domain.model.AttendanceStatusType
import com.example.boardgamerapp.domain.model.FoodCategory
import com.example.boardgamerapp.domain.model.FoodOrder
import com.example.boardgamerapp.domain.model.FoodVote
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightAttendance
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.LateNotice
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.domain.model.Restaurant
import com.example.boardgamerapp.ui.food.FoodViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoodLogicTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createDummyRepository(
        foodVotingSnapshot: FoodVotingSnapshot? = null,
        orderingSnapshot: OrderingSnapshot? = null,
    ): BoardGamerRepository = object : BoardGamerRepository {
        override suspend fun getFoodVotingSnapshot(): Result<FoodVotingSnapshot?> = Result.success(foodVotingSnapshot)
        override suspend fun getOrderingSnapshot(): Result<OrderingSnapshot?> = Result.success(orderingSnapshot)
        override suspend fun addFoodCategory(name: String): Result<FoodCategory> = Result.success(FoodCategory(10L, name, 1L))
        override suspend fun deleteFoodCategory(categoryId: Long): Result<Unit> = Result.success(Unit)
        override suspend fun castFoodVote(playerId: Long, categoryId: Long): Result<FoodVote> = Result.success(FoodVote(1L, playerId, categoryId, 1L))
        override suspend fun saveRestaurant(requestingPlayerId: Long, name: String, menuUrl: String): Result<Restaurant> =
            Result.success(Restaurant(1L, 1L, name, menuUrl))
        override suspend fun saveFoodOrder(playerId: Long, dish: String, note: String, priceCents: Long): Result<FoodOrder> =
            Result.success(FoodOrder(1L, 1L, playerId, dish, note, priceCents))
        override suspend fun deleteFoodOrder(orderId: Long, requestingPlayerId: Long): Result<Unit> = Result.success(Unit)

        override suspend fun getAttendances(): Result<List<GameNightAttendance>> = Result.success(emptyList())
        override suspend fun setAttendance(playerId: Long, status: AttendanceStatusType, minutesLate: Int?, reason: String?): Result<GameNightAttendance> = error("")
        override suspend fun getLateNotices(): Result<List<LateNotice>> = Result.success(emptyList())
        override suspend fun addLateNotice(playerId: Long, minutes: Int): Result<LateNotice> = error("")

        // Stubs for remaining BoardGamerRepository methods
        override suspend fun getUpcomingGameNight() = error("")
        override suspend fun getPlayers() = error("")
        override suspend fun addPlayer(name: String, address: String) = error("")
        override suspend fun updatePlayer(id: Long, name: String, address: String) = error("")
        override suspend fun movePlayer(id: Long, direction: com.example.boardgamerapp.data.repository.MoveDirection) = error("")
        override suspend fun createNextGameNight(startsAt: LocalDateTime?, preferredHostUid: String?, memberOrderOverride: List<String>?) = error("")
        override suspend fun getGameSuggestions() = error("")
        override suspend fun addGameSuggestion(name: String, description: String, suggestedByPlayerId: Long) = error("")
        override suspend fun deleteGameSuggestion(boardGameId: Long, requestingPlayerId: Long) = error("")
        override suspend fun getVotingSnapshot() = error("")
        override suspend fun castVote(playerId: Long, boardGameId: Long) = error("")
        override suspend fun getReviewSnapshot() = error("")
        override suspend fun finishGameNight(gameNightId: Long) = error("")
        override suspend fun submitReview(playerId: Long, gameNightId: Long, hostRating: Int, foodRating: Int, eveningRating: Int, comment: String) = error("")
    }

    @Test
    fun `food vote result text handles empty, single leader and tie`() = runTest(dispatcher) {
        val p1 = Player(1L, "Max", "Addr", 1)
        val p2 = Player(2L, "Erika", "Addr", 2)
        val gameNight = GameNight(1L, LocalDateTime.of(2026, 9, 10, 19, 0), 1L, "Addr", GameNightStatus.PLANNED)

        val catPizza = FoodCategory(1L, "Pizza", 1L)
        val catBurger = FoodCategory(2L, "Burger", 1L)

        // 1. Empty votes
        val emptySnapshot = FoodVotingSnapshot(
            gameNight = gameNight,
            results = listOf(FoodVoteResult(catPizza, emptySet()), FoodVoteResult(catBurger, emptySet())),
            players = listOf(p1, p2),
        )
        val vmEmpty = FoodViewModel(createDummyRepository(emptySnapshot), 1L, dispatcher)
        advanceUntilIdle()
        assertEquals("Noch keine Stimmen", vmEmpty.uiState.resultText)

        // 2. Single leader
        val leaderSnapshot = FoodVotingSnapshot(
            gameNight = gameNight,
            results = listOf(FoodVoteResult(catPizza, setOf(1L, 2L)), FoodVoteResult(catBurger, emptySet())),
            players = listOf(p1, p2),
        )
        val vmLeader = FoodViewModel(createDummyRepository(leaderSnapshot), 1L, dispatcher)
        advanceUntilIdle()
        assertEquals("Aktuell vorne: Pizza", vmLeader.uiState.resultText)

        // 3. Tie
        val tieSnapshot = FoodVotingSnapshot(
            gameNight = gameNight,
            results = listOf(FoodVoteResult(catPizza, setOf(1L)), FoodVoteResult(catBurger, setOf(2L))),
            players = listOf(p1, p2),
        )
        val vmTie = FoodViewModel(createDummyRepository(tieSnapshot), 1L, dispatcher)
        advanceUntilIdle()
        assertEquals("Gleichstand: Pizza und Burger", vmTie.uiState.resultText)
    }

    @Test
    fun `order editor price validation correctly formats and checks inputs`() = runTest(dispatcher) {
        val p1 = Player(1L, "Max", "Addr", 1)
        val gameNight = GameNight(1L, LocalDateTime.of(2026, 9, 10, 19, 0), 1L, "Addr", GameNightStatus.PLANNED)
        val snapshot = FoodVotingSnapshot(gameNight, emptyList(), listOf(p1))

        val vm = FoodViewModel(createDummyRepository(snapshot), 1L, dispatcher)
        advanceUntilIdle()

        vm.beginOrderEditor()
        assertNotNull(vm.uiState.orderEditor)

        // Invalid price format
        vm.updateOrderDish("Pizza")
        vm.updateOrderPrice("abc")
        vm.saveOrder()
        assertEquals("Bitte einen gültigen Preis mit höchstens zwei Nachkommastellen eingeben.", vm.uiState.editorError)

        // Three decimals is invalid
        vm.updateOrderPrice("12.345")
        vm.saveOrder()
        assertEquals("Bitte einen gültigen Preis mit höchstens zwei Nachkommastellen eingeben.", vm.uiState.editorError)

        // Valid price with comma
        vm.updateOrderPrice("12,50")
        vm.saveOrder()
        advanceUntilIdle()
        assertNull(vm.uiState.orderEditor)
        assertEquals("Bestellung gespeichert.", vm.uiState.message)
    }

    @Test
    fun `remindMissingPlayers handles both complete and incomplete voting states`() = runTest(dispatcher) {
        val p1 = Player(1L, "Max", "Addr", 1)
        val p2 = Player(2L, "Erika", "Addr", 2)
        val gameNight = GameNight(1L, LocalDateTime.of(2026, 9, 10, 19, 0), 1L, "Addr", GameNightStatus.PLANNED)

        // Incomplete: Erika has not voted
        val cat = FoodCategory(1L, "Pizza", 1L)
        val incompleteSnapshot = FoodVotingSnapshot(
            gameNight = gameNight,
            results = listOf(FoodVoteResult(cat, setOf(1L))),
            players = listOf(p1, p2),
        )
        val vmIncomplete = FoodViewModel(createDummyRepository(incompleteSnapshot), 1L, dispatcher)
        advanceUntilIdle()

        vmIncomplete.remindMissingPlayers()
        assertTrue(vmIncomplete.uiState.message?.contains("Erika") == true)

        // Complete: Both voted
        val completeSnapshot = FoodVotingSnapshot(
            gameNight = gameNight,
            results = listOf(FoodVoteResult(cat, setOf(1L, 2L))),
            players = listOf(p1, p2),
        )
        val vmComplete = FoodViewModel(createDummyRepository(completeSnapshot), 1L, dispatcher)
        advanceUntilIdle()

        vmComplete.remindMissingPlayers()
        assertEquals("Alle haben bereits abgestimmt.", vmComplete.uiState.message)
    }
}
