package com.example.boardgamerapp

import com.example.boardgamerapp.data.repository.MoveDirection
import com.example.boardgamerapp.domain.model.AttendanceStatusType
import com.example.boardgamerapp.domain.model.GameNight
import com.example.boardgamerapp.domain.model.GameNightStatus
import com.example.boardgamerapp.domain.model.Player
import com.example.boardgamerapp.fake.FakeBoardGamerRepository
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BoardGamerRepositoryTest {

    private lateinit var repository: FakeBoardGamerRepository
    private val player1 = Player(1L, "Max Mustermann", "Musterstraße 12", 1)
    private val player2 = Player(2L, "Erika Musterfrau", "Neustraße 5", 2)

    @Before
    fun setUp() {
        repository = FakeBoardGamerRepository()
        repository.players.add(player1)
        repository.players.add(player2)
    }

    @Test
    fun `getUpcomingGameNight returns active game night and skips cancelled or finished ones`() = runTest {
        assertNull(repository.getUpcomingGameNight().getOrNull())

        val finished = GameNight(1L, LocalDateTime.now().minusDays(7), 1L, "Addr", GameNightStatus.FINISHED)
        val cancelled = GameNight(2L, LocalDateTime.now().minusDays(1), 1L, "Addr", GameNightStatus.CANCELLED)
        val planned = GameNight(3L, LocalDateTime.now().plusDays(5), 2L, "Neustraße 5", GameNightStatus.PLANNED)

        repository.gameNights.addAll(listOf(finished, cancelled, planned))

        val result = repository.getUpcomingGameNight().getOrNull()
        assertNotNull(result)
        assertEquals(3L, result?.gameNight?.id)
        assertEquals(2L, result?.host?.id)
        assertEquals("Erika Musterfrau", result?.host?.name)
    }

    @Test
    fun `selectGameNight marks summary as selected`() = runTest {
        val night1 = GameNight(10L, LocalDateTime.now().plusDays(2), 1L, "Addr", GameNightStatus.PLANNED)
        val night2 = GameNight(20L, LocalDateTime.now().plusDays(5), 2L, "Addr2", GameNightStatus.PLANNED)
        repository.gameNights.addAll(listOf(night1, night2))

        repository.selectGameNight("grp1", "20")

        val summaries = repository.getUpcomingGameNights().getOrThrow()
        assertEquals(2, summaries.size)
        assertFalse(summaries.first { it.gameNightDocId == "10" }.isSelected)
        assertTrue(summaries.first { it.gameNightDocId == "20" }.isSelected)
    }

    @Test
    fun `createNextGameNight adds new game night with first available host`() = runTest {
        val customDate = LocalDateTime.of(2026, 10, 15, 19, 0)
        val result = repository.createNextGameNight(startsAt = customDate).getOrThrow()

        assertEquals(customDate, result.gameNight.startsAt)
        assertEquals(player1.id, result.host.id)
        assertEquals(GameNightStatus.PLANNED, result.gameNight.status)
        assertEquals(1, repository.gameNights.size)
    }

    @Test
    fun `updateGameNight changes date and host location`() = runTest {
        val night = repository.createNextGameNight().getOrThrow().gameNight
        val newDate = LocalDateTime.of(2026, 11, 20, 18, 0)

        val updated = repository.updateGameNight(night.id, newDate, player2.id).getOrThrow()

        assertEquals(newDate, updated.gameNight.startsAt)
        assertEquals(player2.id, updated.host.id)
        assertEquals("Neustraße 5", updated.gameNight.location)
    }

    @Test
    fun `rescheduleGameNight updates date and clears existing attendances`() = runTest {
        val night = repository.createNextGameNight().getOrThrow().gameNight
        repository.setAttendance(player1.id, AttendanceStatusType.ATTENDING)
        repository.setAttendance(player2.id, AttendanceStatusType.LATE, minutesLate = 15)
        assertEquals(2, repository.getAttendances().getOrThrow().size)

        val newDate = LocalDateTime.of(2026, 11, 25, 20, 0)
        val rescheduled = repository.rescheduleGameNight(night.id, newDate).getOrThrow()

        assertEquals(newDate, rescheduled.gameNight.startsAt)
        assertEquals(0, repository.getAttendances().getOrThrow().size)
    }

    @Test
    fun `reassignHost transfers hosting and marks former host as declined`() = runTest {
        val night = repository.createNextGameNight().getOrThrow().gameNight
        assertEquals(player1.id, night.hostId)

        val reassigned = repository.reassignHost(night.id, player2.id).getOrThrow()

        assertEquals(player2.id, reassigned.host.id)
        assertEquals(player2.address, reassigned.gameNight.location)

        val attendances = repository.getAttendances().getOrThrow()
        val formerHostAttendance = attendances.firstOrNull { it.playerId == player1.id }
        assertNotNull(formerHostAttendance)
        assertEquals(AttendanceStatusType.DECLINED, formerHostAttendance?.status)
    }

    @Test
    fun `cancelGameNight transitions status to CANCELLED`() = runTest {
        val night = repository.createNextGameNight().getOrThrow().gameNight
        repository.cancelGameNight(night.id, "Krankheit").getOrThrow()

        val stored = repository.gameNights.first { it.id == night.id }
        assertEquals(GameNightStatus.CANCELLED, stored.status)
        assertNull(repository.getUpcomingGameNight().getOrNull())
    }

    @Test
    fun `setAttendance records status and late reasons`() = runTest {
        repository.createNextGameNight().getOrThrow()

        repository.setAttendance(player1.id, AttendanceStatusType.LATE, minutesLate = 20, reason = "Stau").getOrThrow()
        val list = repository.getAttendances().getOrThrow()

        assertEquals(1, list.size)
        assertEquals(AttendanceStatusType.LATE, list[0].status)
        assertEquals(20, list[0].minutesLate)
        assertEquals("Stau", list[0].reason)
    }

    @Test
    fun `addLateNotice tracks arrival delays`() = runTest {
        repository.createNextGameNight().getOrThrow()

        repository.addLateNotice(player2.id, 15).getOrThrow()
        val notices = repository.getLateNotices().getOrThrow()

        assertEquals(1, notices.size)
        assertEquals(player2.id, notices[0].playerId)
        assertEquals(15, notices[0].minutes)
    }

    @Test
    fun `player management allows update and reordering`() = runTest {
        // Update player
        repository.updatePlayer(player1.id, "Max Neuer Name", "Neue Straße 1").getOrThrow()
        val updated = repository.getPlayers().getOrThrow().first { it.id == player1.id }
        assertEquals("Max Neuer Name", updated.name)
        assertEquals("Neue Straße 1", updated.address)

        // Move player1 down (from hostOrder 1 to 2)
        val reordered = repository.movePlayer(player1.id, MoveDirection.DOWN).getOrThrow()
        assertEquals(player2.id, reordered[0].id)
        assertEquals(1, reordered[0].hostOrder)
        assertEquals(player1.id, reordered[1].id)
        assertEquals(2, reordered[1].hostOrder)
    }

    @Test
    fun `game suggestions and voting cycle functions correctly`() = runTest {
        repository.createNextGameNight().getOrThrow()

        val suggestion = repository.addGameSuggestion("Terraforming Mars", "Strategiespiel", player1.id).getOrThrow()
        val suggestions = repository.getGameSuggestions().getOrThrow()
        assertEquals(1, suggestions?.suggestions?.size)

        // Non-owner cannot delete
        val failDelete = repository.deleteGameSuggestion(suggestion.boardGame.id, player2.id)
        assertTrue(failDelete.isFailure)

        // Cast vote
        repository.castVote(player2.id, suggestion.boardGame.id).getOrThrow()
        val snapshot = repository.getVotingSnapshot().getOrThrow()
        assertEquals(1, snapshot?.totalVotes)
        assertEquals(setOf(player2.id), snapshot?.results?.first()?.voterIds)

        // Owner deletes
        repository.deleteGameSuggestion(suggestion.boardGame.id, player1.id).getOrThrow()
        assertEquals(0, repository.getGameSuggestions().getOrThrow()?.suggestions?.size)
    }

    @Test
    fun `food voting tracks votes and identifies missing voters`() = runTest {
        repository.createNextGameNight().getOrThrow()

        val catPizza = repository.addFoodCategory("Pizza").getOrThrow()
        val catSushi = repository.addFoodCategory("Sushi").getOrThrow()

        // Only player 1 votes
        repository.castFoodVote(player1.id, catPizza.id).getOrThrow()

        val snapshot = repository.getFoodVotingSnapshot().getOrThrow()
        assertNotNull(snapshot)
        assertEquals(1, snapshot?.totalVotes)
        assertEquals(1, snapshot?.missingPlayers?.size)
        assertEquals(player2.id, snapshot?.missingPlayers?.first()?.id)

        // Delete category cleans up votes
        repository.deleteFoodCategory(catPizza.id).getOrThrow()
        assertEquals(1, repository.getFoodVotingSnapshot().getOrThrow()?.results?.size)
    }

    @Test
    fun `ordering snapshot calculates total price and enforces ownership on deletion`() = runTest {
        repository.createNextGameNight().getOrThrow()

        repository.saveRestaurant(player1.id, "Trattoria Roma", "https://menu.example.com").getOrThrow()
        val order1 = repository.saveFoodOrder(player1.id, "Pasta Bolognese", "", 1200L).getOrThrow()
        val order2 = repository.saveFoodOrder(player2.id, "Insalata Mista", "", 850L).getOrThrow()

        val snapshot = repository.getOrderingSnapshot().getOrThrow()
        assertNotNull(snapshot)
        assertEquals("Trattoria Roma", snapshot?.restaurant?.name)
        assertEquals(2, snapshot?.orders?.size)
        assertEquals(2050L, snapshot?.totalCents)

        // Player 2 cannot delete Player 1's order
        assertTrue(repository.deleteFoodOrder(order1.id, player2.id).isFailure)

        // Player 1 can delete own order
        assertTrue(repository.deleteFoodOrder(order1.id, player1.id).isSuccess)
        assertEquals(850L, repository.getOrderingSnapshot().getOrThrow()?.totalCents)
    }

    @Test
    fun `reviewing and finishing game night computes rating averages`() = runTest {
        val night = repository.createNextGameNight().getOrThrow().gameNight

        // Finish game night
        val finished = repository.finishGameNight(night.id).getOrThrow()
        assertEquals(GameNightStatus.FINISHED, finished.status)

        // Submit reviews
        repository.submitReview(player1.id, night.id, hostRating = 5, foodRating = 4, eveningRating = 5, comment = "Top").getOrThrow()
        repository.submitReview(player2.id, night.id, hostRating = 4, foodRating = 5, eveningRating = 4, comment = "Gut").getOrThrow()

        val snapshot = repository.getReviewSnapshot().getOrThrow()
        assertNotNull(snapshot)
        assertEquals(2, snapshot?.reviews?.size)
        assertEquals(4.5, snapshot?.averages?.host ?: 0.0, 0.01)
        assertEquals(4.5, snapshot?.averages?.food ?: 0.0, 0.01)
        assertEquals(4.5, snapshot?.averages?.evening ?: 0.0, 0.01)
    }
}
