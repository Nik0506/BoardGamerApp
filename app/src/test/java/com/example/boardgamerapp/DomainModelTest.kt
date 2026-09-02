package com.example.boardgamerapp

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelTest {

    @Test
    fun `GameNightStatus contains expected lifecycle states`() {
        val states = GameNightStatus.entries.map { it.name }
        assertTrue(states.contains("PLANNED"))
        assertTrue(states.contains("ACTIVE"))
        assertTrue(states.contains("FINISHED"))
        assertTrue(states.contains("CANCELLED"))
    }

    @Test
    fun `AttendanceStatusType contains all RSVP options`() {
        val types = AttendanceStatusType.entries.map { it.name }
        assertEquals(listOf("ATTENDING", "LATE", "DECLINED", "PENDING"), types)
    }

    @Test
    fun `GameNightAttendance holds correct properties and defaults`() {
        val now = LocalDateTime.now()
        val attendance = GameNightAttendance(
            id = 101L,
            playerId = 1L,
            gameNightId = 42L,
            status = AttendanceStatusType.LATE,
            minutesLate = 15,
            reason = "Stau auf A1",
            createdAt = now,
            updatedAt = now,
        )

        assertEquals(101L, attendance.id)
        assertEquals(1L, attendance.playerId)
        assertEquals(42L, attendance.gameNightId)
        assertEquals(AttendanceStatusType.LATE, attendance.status)
        assertEquals(15, attendance.minutesLate)
        assertEquals("Stau auf A1", attendance.reason)
        assertEquals(now, attendance.createdAt)
        assertEquals(now, attendance.updatedAt)

        val copy = attendance.copy(status = AttendanceStatusType.ATTENDING, minutesLate = null)
        assertEquals(AttendanceStatusType.ATTENDING, copy.status)
        assertNull(copy.minutesLate)
    }

    @Test
    fun `Player model stores and compares correctly`() {
        val player1 = Player(1L, "Max Mustermann", "Musterstraße 12", 1)
        val player2 = Player(1L, "Max Mustermann", "Musterstraße 12", 1)
        val player3 = Player(2L, "Erika Musterfrau", "Neustraße 5", 2)

        assertEquals(player1, player2)
        assertNotEquals(player1, player3)
        assertEquals(1, player1.hostOrder)
    }

    @Test
    fun `BoardGame model retains name and suggestion metadata`() {
        val game = BoardGame(
            id = 10L,
            name = "Scythe",
            description = "Dieselpunk Strategiespiel",
            suggestedByPlayerId = 2L,
            gameNightId = 42L,
        )

        assertEquals(10L, game.id)
        assertEquals("Scythe", game.name)
        assertEquals("Dieselpunk Strategiespiel", game.description)
        assertEquals(2L, game.suggestedByPlayerId)
        assertEquals(42L, game.gameNightId)
    }

    @Test
    fun `FoodCategory and FoodOrder store items with cent amounts`() {
        val category = FoodCategory(id = 5L, name = "Pizza", gameNightId = 42L)
        assertEquals("Pizza", category.name)

        val order = FoodOrder(
            id = 1L,
            gameNightId = 42L,
            playerId = 2L,
            dish = "Pizza Margherita",
            note = "Knusprig gebacken",
            priceCents = 850L,
        )
        assertEquals("Pizza Margherita", order.dish)
        assertEquals("Knusprig gebacken", order.note)
        assertEquals(850L, order.priceCents)
    }

    @Test
    fun `Restaurant model retains menuUrl and name`() {
        val restaurant = Restaurant(
            id = 1L,
            gameNightId = 42L,
            name = "Pizzeria Bella",
            menuUrl = "https://example.com/menu",
        )
        assertEquals("Pizzeria Bella", restaurant.name)
        assertEquals("https://example.com/menu", restaurant.menuUrl)
    }

    @Test
    fun `Vote and FoodVote map player choices to target items`() {
        val gameVote = Vote(id = 1L, playerId = 2L, boardGameId = 10L, gameNightId = 42L)
        assertEquals(2L, gameVote.playerId)
        assertEquals(10L, gameVote.boardGameId)

        val foodVote = FoodVote(id = 2L, playerId = 3L, foodCategoryId = 5L, gameNightId = 42L)
        assertEquals(3L, foodVote.playerId)
        assertEquals(5L, foodVote.foodCategoryId)
    }

    @Test
    fun `Review retains scores in 1 to 5 range and comments`() {
        val review = Review(
            id = 1L,
            playerId = 2L,
            gameNightId = 42L,
            hostRating = 5,
            foodRating = 4,
            eveningRating = 5,
            comment = "Toller Abend!",
        )
        assertEquals(5, review.hostRating)
        assertEquals(4, review.foodRating)
        assertEquals(5, review.eveningRating)
        assertEquals("Toller Abend!", review.comment)
    }

    @Test
    fun `LateNotice records minutes and timestamp`() {
        val timestamp = LocalDateTime.of(2026, 9, 2, 19, 15)
        val notice = LateNotice(
            id = 1L,
            playerId = 3L,
            gameNightId = 42L,
            minutes = 20,
            createdAt = timestamp,
        )
        assertEquals(20, notice.minutes)
        assertEquals(timestamp, notice.createdAt)
    }

    @Test
    fun `GameNight model maintains host and location links`() {
        val startsAt = LocalDateTime.of(2026, 9, 10, 18, 0)
        val gameNight = GameNight(
            id = 42L,
            startsAt = startsAt,
            hostId = 1L,
            location = "Musterstraße 12",
            status = GameNightStatus.PLANNED,
        )
        assertEquals(42L, gameNight.id)
        assertEquals(startsAt, gameNight.startsAt)
        assertEquals(1L, gameNight.hostId)
        assertEquals("Musterstraße 12", gameNight.location)
        assertEquals(GameNightStatus.PLANNED, gameNight.status)
    }
}
