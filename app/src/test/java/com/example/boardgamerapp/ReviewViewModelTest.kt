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
import com.example.boardgamerapp.fake.FakeBoardGamerRepository
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
class ReviewViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeBoardGamerRepository

    private val host = Player(1L, "Max Mustermann", "Musterstraße 12", 1)
    private val guest = Player(2L, "Erika Musterfrau", "Neustraße 5", 2)
    private val finishedNight = GameNight(10L, LocalDateTime.of(2026, 9, 10, 19, 0), host.id, host.address, GameNightStatus.FINISHED)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeBoardGamerRepository()
        repository.players.addAll(listOf(host, guest))
        repository.gameNights.add(finishedNight)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load renders finished game night with reviews and formatted averages`() = runTest(dispatcher) {
        repository.submitReview(guest.id, finishedNight.id, hostRating = 5, foodRating = 4, eveningRating = 5, comment = "Klasse").getOrThrow()

        val vm = ReviewViewModel(repository, repository, guest.id, dispatcher)
        advanceUntilIdle()

        assertTrue(vm.uiState is ReviewUiState.Content)
        val content = vm.uiState as ReviewUiState.Content
        assertEquals(finishedNight.id, content.gameNightId)
        assertEquals("Max Mustermann", content.hostName)
        assertTrue(content.isFinished)
        assertEquals(1, content.reviewCount)
        assertEquals("5,0", content.averages?.host)
        assertEquals("4,0", content.averages?.food)
        assertEquals("5,0", content.averages?.evening)
        assertTrue(content.currentPlayerHasReviewed)
        assertNull(content.selectedPlayerId) // already reviewed
    }

    @Test
    fun `load renders Empty state when repository has no game nights`() = runTest(dispatcher) {
        repository.gameNights.clear()
        val vm = ReviewViewModel(repository, repository, guest.id, dispatcher)
        advanceUntilIdle()

        assertTrue(vm.uiState is ReviewUiState.Empty)
    }

    @Test
    fun `load renders Error state when repository returns exception`() = runTest(dispatcher) {
        val failingRepo = object : ReviewRepository {
            override suspend fun getReviewSnapshot() = Result.failure<ReviewSnapshot?>(RuntimeException("Datenbank offline"))
            override suspend fun finishGameNight(gameNightId: Long) = error("")
            override suspend fun submitReview(playerId: Long, gameNightId: Long, hostRating: Int, foodRating: Int, eveningRating: Int, comment: String) = error("")
        }

        val vm = ReviewViewModel(failingRepo, repository, guest.id, dispatcher)
        advanceUntilIdle()

        assertTrue(vm.uiState is ReviewUiState.Error)
        assertEquals("Datenbank offline", (vm.uiState as ReviewUiState.Error).message)
    }

    @Test
    fun `finishGameNight completes night and displays success message`() = runTest(dispatcher) {
        val plannedNight = GameNight(20L, LocalDateTime.of(2026, 10, 1, 19, 0), host.id, host.address, GameNightStatus.PLANNED)
        repository.gameNights.clear()
        repository.gameNights.add(plannedNight)

        val vm = ReviewViewModel(repository, repository, host.id, dispatcher)
        advanceUntilIdle()

        val contentBefore = vm.uiState as ReviewUiState.Content
        assertFalse(contentBefore.isFinished)

        vm.finishGameNight()
        advanceUntilIdle()

        val contentAfter = vm.uiState as ReviewUiState.Content
        assertTrue(contentAfter.isFinished)
        assertEquals("Spieleabend abgeschlossen.", contentAfter.message)
    }

    @Test
    fun `beginReview requires finished game night and unreviewed player`() = runTest(dispatcher) {
        // Unfinished game night
        val plannedNight = GameNight(20L, LocalDateTime.of(2026, 10, 1, 19, 0), host.id, host.address, GameNightStatus.PLANNED)
        repository.gameNights.clear()
        repository.gameNights.add(plannedNight)

        val vmUnfinished = ReviewViewModel(repository, repository, guest.id, dispatcher)
        advanceUntilIdle()

        vmUnfinished.beginReview()
        assertNull((vmUnfinished.uiState as ReviewUiState.Content).editor)

        // Finished game night, but already reviewed
        repository.gameNights.clear()
        repository.gameNights.add(finishedNight)
        repository.submitReview(guest.id, finishedNight.id, 5, 5, 5, "").getOrThrow()

        val vmReviewed = ReviewViewModel(repository, repository, guest.id, dispatcher)
        advanceUntilIdle()

        vmReviewed.beginReview()
        assertNull((vmReviewed.uiState as ReviewUiState.Content).editor)

        // Finished game night and guest has NOT reviewed yet
        repository.reviews.clear()
        val vmEligible = ReviewViewModel(repository, repository, guest.id, dispatcher)
        advanceUntilIdle()

        vmEligible.beginReview()
        assertNotNull((vmEligible.uiState as ReviewUiState.Content).editor)
    }

    @Test
    fun `review editor validates score range and saves review`() = runTest(dispatcher) {
        val vm = ReviewViewModel(repository, repository, guest.id, dispatcher)
        advanceUntilIdle()

        vm.beginReview()
        assertNotNull((vm.uiState as ReviewUiState.Content).editor)

        // Invalid: 0 points (default)
        vm.saveReview()
        val errorContent = vm.uiState as ReviewUiState.Content
        assertEquals("Vergib in allen drei Kategorien 1 bis 5 Punkte.", errorContent.editor?.errorMessage)

        // Set ratings
        vm.setHostRating(5)
        vm.setFoodRating(4)
        vm.setEveningRating(5)
        vm.updateComment("Großartiger Spieleabend!")

        vm.saveReview()
        advanceUntilIdle()

        val successContent = vm.uiState as ReviewUiState.Content
        assertNull(successContent.editor)
        assertEquals("Bewertung gespeichert.", successContent.message)
        assertEquals(1, successContent.reviewCount)
        assertTrue(successContent.currentPlayerHasReviewed)
    }

    @Test
    fun `dismissEditor and clearMessage reset state properly`() = runTest(dispatcher) {
        val vm = ReviewViewModel(repository, repository, guest.id, dispatcher)
        advanceUntilIdle()

        vm.beginReview()
        assertNotNull((vm.uiState as ReviewUiState.Content).editor)

        vm.dismissEditor()
        assertNull((vm.uiState as ReviewUiState.Content).editor)

        vm.finishGameNight()
        advanceUntilIdle()
        assertNotNull((vm.uiState as ReviewUiState.Content).message)

        vm.clearMessage()
        assertNull((vm.uiState as ReviewUiState.Content).message)
        assertNull((vm.uiState as ReviewUiState.Content).errorMessage)
    }

    @Test
    fun `ReviewViewModel factory instantiates ViewModel`() {
        val factory = ReviewViewModel.factory(repository, guest.id)
        val vm = factory.create(ReviewViewModel::class.java)
        assertNotNull(vm)
    }
}
